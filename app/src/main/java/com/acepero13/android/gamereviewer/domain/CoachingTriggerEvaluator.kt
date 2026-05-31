package com.acepero13.android.gamereviewer.domain

import android.util.Log
import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.engine.highlights.BoardAttackHelper
import com.acepero13.android.gamereviewer.engine.highlights.materialValue
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square

private const val TAG = "CoachTriggerEval"

/**
 * Detects proactive coaching moments across an entire game's evaluation data.
 *
 * All detection logic is heuristic and deliberately lightweight:
 *   - [CoachingTrigger.ForcingMove] and [CoachingTrigger.CandidateMoves] use only stored
 *     [GameEvaluation.motif] and [GameEvaluation.evalCp] — no board loads.
 *   - [CoachingTrigger.Safety], [CoachingTrigger.WorstPiece], and [CoachingTrigger.OpponentPlan]
 *     load the FEN via [BoardAttackHelper] for geometry checks.
 *
 * Runs during background analysis after Stockfish completes (Dispatchers.Default).
 */
object CoachingTriggerEvaluator {

    // ── Thresholds ─────────────────────────────────────────────────────────────

    private const val CANDIDATE_EVAL_THRESHOLD_CP    = 30   // ±30cp = balanced position
    private const val WORST_PIECE_MAX_MOBILITY       = 2   // pieces with ≤ this many target squares
    private const val WORST_PIECE_STREAK_NEEDED      = 3   // consecutive positions with same restricted piece
    private const val MIDDLEGAME_START               = 10  // half-move index
    private const val MIDDLEGAME_END                 = 50  // half-move index
    private const val OPPONENT_PLAN_MIN_CP           = 30  // minimum gain to note opponent's plan
    private const val OPPONENT_PLAN_MAX_CP           = 120 // above this it's a user blunder (already flagged)
    private const val KING_MIN_ADJACENT_DEFENDERS    = 1   // fewer than this = Safety trigger fires
    // In the opening phase, only fire PreMoveChecklist / ForcingMove when the
    // mover dropped ≥ this many centipawns — suppress intentional tension / gambits.
    private const val OPENING_TRIGGER_THRESHOLD_CP   = -75
    private const val IMPULSE_TIME_THRESHOLD_SECONDS = 5   // moves played faster than this qualify as impulsive
    private const val IMPULSE_CP_LOSS_THRESHOLD      = 200 // centipawn loss that makes a fast move a coached event
    private const val CANDIDATE_SEARCH_MIN_CP        = 50  // |eval| lower bound for "rich with plans" zone
    private const val CANDIDATE_SEARCH_MAX_CP        = 300 // above this the position is near-decisive, not a plan choice
    private const val CCT_CHECK_EVAL_SHIFT_CP        = 100 // any shift ≥ 1.0 pawn triggers CCT habit reminder

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Evaluates all positions in [evaluations], loading boards from [fenByMoveIndex] where needed.
     *
     * @param evaluations    Sorted or unsorted per-move evaluation records for one game.
     * @param fenByMoveIndex Maps moveIndex → FEN string for positions needing geometry checks.
     * @return               Map from moveIndex to the list of triggers that fired at that position.
     */
    fun evaluate(
        evaluations: List<GameEvaluation>,
        fenByMoveIndex: (Int) -> String,
        timeByMoveIndex: (Int) -> Int? = { null },
        playerIsWhite: Boolean = true,
    ): Map<Int, List<CoachingTrigger>> {
        val result = mutableMapOf<Int, MutableList<CoachingTrigger>>()
        val sorted = evaluations.sortedBy { it.moveIndex }

        // Worst-piece streak tracking: square name → consecutive-position count
        val worstPieceStreak = mutableMapOf<String, Int>()

        sorted.forEachIndexed { i, eval ->
            val triggers  = mutableListOf<CoachingTrigger>()
            val prevEval  = if (i > 0) sorted[i - 1] else null
            val isWhite   = eval.moveIndex % 2 == 1

            // Only load the board once and reuse for multiple triggers
            val board: Board? = runCatching {
                val fen = fenByMoveIndex(eval.moveIndex)
                if (fen.isBlank()) null else Board().apply { loadFromFen(fen) }
            }.getOrNull()

            // ── 1. Safety ─────────────────────────────────────────────────────
            if (board != null && eval.moveIndex in MIDDLEGAME_START..MIDDLEGAME_END) {
                detectSafety(board, eval.moveIndex, playerIsWhite)?.let { triggers.add(it) }
            }

            // ── 2. Candidate Moves ────────────────────────────────────────────
            if (eval.moveIndex in MIDDLEGAME_START..MIDDLEGAME_END) {
                detectCandidate(eval, isWhite)?.let { triggers.add(it) }
            }

            // ── 3. Worst Piece ────────────────────────────────────────────────
            if (board != null && eval.moveIndex in MIDDLEGAME_START..MIDDLEGAME_END) {
                detectWorstPiece(board, eval.moveIndex, isWhite, worstPieceStreak)
                    ?.let { triggers.add(it) }
            } else if (eval.moveIndex !in MIDDLEGAME_START..MIDDLEGAME_END) {
                worstPieceStreak.clear()
            }

            // ── 4. Forcing Move ───────────────────────────────────────────────
            // Suppress in the opening unless the mover actually blundered (≥75 cp drop).
            detectForcingMove(eval)
                ?.takeIf { eval.moveIndex >= MIDDLEGAME_START || eval.evalDelta <= OPENING_TRIGGER_THRESHOLD_CP }
                ?.let { triggers.add(it) }

            // ── 5. Opponent's Plan ────────────────────────────────────────────
            if (prevEval != null) {
                detectOpponentPlan(eval, prevEval, isWhite)?.let { triggers.add(it) }
            }

            // ── 6. Pre-Move Checklist ─────────────────────────────────────────
            // Suppress in the opening unless there is a genuine mistake — tension
            // (gambits, accepted pawns) is intentional and should not be flagged.
            if (board != null) {
                detectPreMoveChecklist(board, eval.moveIndex)
                    ?.takeIf { eval.moveIndex >= MIDDLEGAME_START || eval.evalDelta <= OPENING_TRIGGER_THRESHOLD_CP }
                    ?.let { triggers.add(it) }
            }

            // ── 7. Rook Activation ────────────────────────────────────────────
            if (board != null && eval.moveIndex in MIDDLEGAME_START..MIDDLEGAME_END) {
                detectRookActivation(board, eval.moveIndex, isWhite)?.let { triggers.add(it) }
            }

            // ── 8. Impulse Control ────────────────────────────────────────────
            // Fast move (< 5 s) that caused a significant evaluation drop.
            detectImpulseControl(eval, isWhite, timeByMoveIndex)?.let { triggers.add(it) }

            // ── 9. Candidate Search ───────────────────────────────────────────
            // Moderately complex position with no forcing sequence — multiple plans exist.
            if (eval.moveIndex in MIDDLEGAME_START..MIDDLEGAME_END) {
                detectCandidateSearch(eval, isWhite)?.let { triggers.add(it) }
            }

            // ── 10. CCT Check ─────────────────────────────────────────────────
            // Any significant eval shift — instil the habit of scanning opponent CCT responses.
            if (eval.moveIndex >= MIDDLEGAME_START) {
                detectCctCheck(eval)?.let { triggers.add(it) }
            }

            if (triggers.isNotEmpty()) {
                result[eval.moveIndex] = triggers
            }
        }

        val boardNullCount = sorted.count { ev ->
            val fen = fenByMoveIndex(ev.moveIndex)
            fen.isBlank()
        }
        Log.d(TAG, "evaluate: ${sorted.size} evals → ${result.size} trigger positions | boardNullFens=$boardNullCount | by type: ${result.values.flatten().groupBy { it.typeName() }.mapValues { it.value.size }}")

        return result
    }

    /**
     * Decodes a comma-separated trigger string (as stored in [GameEvaluation.coachingTriggers])
     * back into a list of stub [CoachingTrigger] objects.
     */
    fun parseTriggers(encoded: String, moveIndex: Int): List<CoachingTrigger> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split(",").mapNotNull { type ->
            CoachingTrigger.fromTypeName(type.trim(), moveIndex)
        }
    }

    // ── Private: individual trigger detectors ──────────────────────────────────

    private fun detectSafety(board: Board, moveIndex: Int, isWhite: Boolean): CoachingTrigger.Safety? {
        // Always check the player's own king (isWhite = playerIsWhite, not the moving side)
        val movingSide  = if (isWhite) Side.WHITE else Side.BLACK
        val kingSquare  = BoardAttackHelper.kingSquare(board, movingSide) ?: return null

        // Count friendly pieces in the 8 adjacent squares (including pawns)
        val adjacent    = adjacentSquares(kingSquare)
        val defenderCount = adjacent.count { sq ->
            val p = board.getPiece(sq)
            p != Piece.NONE && p.pieceSide == movingSide && p.pieceType != PieceType.KING
        }

        // Fire if king has fewer than threshold defenders around it
        return if (defenderCount <= KING_MIN_ADJACENT_DEFENDERS) {
            CoachingTrigger.Safety(moveIndex, kingSquare.name)
        } else null
    }

    private fun detectCandidate(eval: GameEvaluation, isWhite: Boolean): CoachingTrigger.CandidateMoves? {
        val evalFromMover = if (isWhite) eval.evalCp else -eval.evalCp
        return if (eval.motif == "mixed" && kotlin.math.abs(evalFromMover) <= CANDIDATE_EVAL_THRESHOLD_CP) {
            CoachingTrigger.CandidateMoves(eval.moveIndex, eval.evalCp)
        } else null
    }

    private fun detectWorstPiece(
        board: Board,
        moveIndex: Int,
        isWhite: Boolean,
        streakTracker: MutableMap<String, Int>,
    ): CoachingTrigger.WorstPiece? {
        val side = if (isWhite) Side.WHITE else Side.BLACK

        // Consider only minor/major pieces (exclude kings and pawns — naturally limited)
        val candidatePieceTypes = setOf(
            PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN,
        )
        val restricted = BoardAttackHelper.piecesOf(board, side)
            .filter { (_, piece) -> piece.pieceType in candidatePieceTypes }
            .map    { (sq, _)   -> sq to BoardAttackHelper.attacksFrom(board, sq).size }
            .filter { (_, mob)  -> mob <= WORST_PIECE_MAX_MOBILITY }

        if (restricted.isEmpty()) {
            streakTracker.clear()
            return null
        }

        val (worstSq, mobility) = restricted.minByOrNull { (_, mob) -> mob } ?: return null
        val key = worstSq.name

        // Read previous streak for this square BEFORE clearing the map
        val prevStreak = streakTracker[key] ?: 0
        streakTracker.clear()
        val newStreak = prevStreak + 1
        streakTracker[key] = newStreak

        return if (newStreak >= WORST_PIECE_STREAK_NEEDED) {
            CoachingTrigger.WorstPiece(moveIndex, key, mobility)
        } else null
    }

    private fun detectForcingMove(eval: GameEvaluation): CoachingTrigger.ForcingMove? {
        return if (eval.motif in listOf("fork", "hanging", "checkmate")) {
            CoachingTrigger.ForcingMove(eval.moveIndex, eval.motif)
        } else null
    }

    private fun detectOpponentPlan(
        eval: GameEvaluation,
        prevEval: GameEvaluation,
        isWhite: Boolean,
    ): CoachingTrigger.OpponentPlan? {
        val moverGain = if (isWhite) eval.evalDelta else -eval.evalDelta
        Log.v(TAG, "detectOpponentPlan move=${eval.moveIndex} evalDelta=${eval.evalDelta} moverGain=$moverGain (need $OPPONENT_PLAN_MIN_CP..$OPPONENT_PLAN_MAX_CP)")

        return if (moverGain in OPPONENT_PLAN_MIN_CP..OPPONENT_PLAN_MAX_CP) {
            CoachingTrigger.OpponentPlan(eval.moveIndex, moverGain)
        } else null
    }

    private fun detectPreMoveChecklist(board: Board, moveIndex: Int): CoachingTrigger.PreMoveChecklist? {
        val hangingSquare = BoardAttackHelper.allPieces(board)
            .filter { (_, piece) -> piece.pieceType != PieceType.KING }
            .firstOrNull { (sq, piece) -> isGenuinelyHanging(board, sq, piece) }
            ?.first

        return if (hangingSquare != null) {
            CoachingTrigger.PreMoveChecklist(moveIndex, hangingSquare.name)
        } else null
    }

    private fun isGenuinelyHanging(board: Board, sq: Square, piece: Piece) =
        BoardAttackHelper.isGenuinelyHanging(board, sq, piece)

    private fun detectRookActivation(board: Board, moveIndex: Int, isWhite: Boolean): CoachingTrigger.RookActivation? {
        val side      = if (isWhite) Side.WHITE else Side.BLACK
        val rookPiece = if (side == Side.WHITE) com.github.bhlangonijr.chesslib.Piece.WHITE_ROOK
                        else                    com.github.bhlangonijr.chesslib.Piece.BLACK_ROOK

        val rooks = BoardAttackHelper.piecesOf(board, side)
            .filter { (_, piece) -> piece == rookPiece }

        for ((rookSq, _) in rooks) {
            val rookFile = BoardAttackHelper.fileOf(rookSq)

            // Closed file = any pawn (either color) on the rook's file
            val rookFileHasPawn = (0..7).any { rank ->
                val sq = BoardAttackHelper.squareAt(rookFile, rank) ?: return@any false
                val p  = board.getPiece(sq)
                p != Piece.NONE && p.pieceType == PieceType.PAWN
            }
            if (!rookFileHasPawn) continue  // already on open file

            // Low rook mobility confirms it's stuck
            val mobility = BoardAttackHelper.attacksFrom(board, rookSq).size
            if (mobility >= 6) continue

            // Find an open file (no pawns of either color) or half-open (no friendly pawns)
            val betterFile = (0..7).firstOrNull { file ->
                if (file == rookFile) return@firstOrNull false
                // Half-open: no friendly pawns on this file
                val hasFriendlyPawn = (0..7).any { rank ->
                    val sq = BoardAttackHelper.squareAt(file, rank) ?: return@any false
                    val p  = board.getPiece(sq)
                    p != Piece.NONE && p.pieceType == PieceType.PAWN && p.pieceSide == side
                }
                !hasFriendlyPawn
            }

            if (betterFile != null) {
                return CoachingTrigger.RookActivation(moveIndex, rookSq.name, betterFile)
            }
        }
        return null
    }

    private fun detectImpulseControl(
        eval: GameEvaluation,
        isWhite: Boolean,
        timeByMoveIndex: (Int) -> Int?,
    ): CoachingTrigger.ImpulseControl? {
        val timeSpent = timeByMoveIndex(eval.moveIndex) ?: return null
        if (timeSpent >= IMPULSE_TIME_THRESHOLD_SECONDS) return null
        val moverEvalDelta = if (isWhite) eval.evalDelta else -eval.evalDelta
        return if (moverEvalDelta <= -IMPULSE_CP_LOSS_THRESHOLD) {
            CoachingTrigger.ImpulseControl(eval.moveIndex, timeSpent, kotlin.math.abs(moverEvalDelta))
        } else null
    }

    private fun detectCandidateSearch(eval: GameEvaluation, isWhite: Boolean): CoachingTrigger.CandidateSearch? {
        val evalFromMover = if (isWhite) eval.evalCp else -eval.evalCp
        val absMover = kotlin.math.abs(evalFromMover)
        // Position is not trivially equal and not overwhelmingly decisive, with no clear
        // forcing sequence — the "rich with plans" zone where comparing options matters most.
        return if (eval.motif == "mixed" && absMover in CANDIDATE_SEARCH_MIN_CP..CANDIDATE_SEARCH_MAX_CP) {
            CoachingTrigger.CandidateSearch(eval.moveIndex, eval.evalCp)
        } else null
    }

    private fun detectCctCheck(eval: GameEvaluation): CoachingTrigger.CctCheck? {
        return if (kotlin.math.abs(eval.evalDelta) > CCT_CHECK_EVAL_SHIFT_CP) {
            CoachingTrigger.CctCheck(eval.moveIndex, eval.evalDelta)
        } else null
    }

    // ── Board geometry helpers ─────────────────────────────────────────────────

    private fun adjacentSquares(square: Square): List<Square> {
        val file = BoardAttackHelper.fileOf(square)
        val rank = BoardAttackHelper.rankOf(square)
        val result = mutableListOf<Square>()
        for (df in -1..1) {
            for (dr in -1..1) {
                if (df == 0 && dr == 0) continue
                BoardAttackHelper.squareAt(file + df, rank + dr)?.let { result.add(it) }
            }
        }
        return result
    }
}
