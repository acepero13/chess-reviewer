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
    private const val OPPONENT_PLAN_MIN_CP           = 50  // minimum gain to note opponent's plan
    private const val OPPONENT_PLAN_MAX_CP           = 120 // above this it's a user blunder (already flagged)
    private const val KING_MIN_ADJACENT_DEFENDERS    = 1   // fewer than this = Safety candidate
    // Safety only fires when there is a genuine tactical reason, not merely structural exposure.
    // If the evaluation drop is below this and no opponent piece directly attacks a king-adjacent
    // square, the position is treated as structural — promote CandidateSearch instead.
    private const val SAFETY_MIN_CP_DROP             = 100
    // In the opening phase, only fire PreMoveChecklist / ForcingMove when the
    // mover dropped ≥ this many centipawns — suppress intentional tension / gambits.
    private const val OPENING_TRIGGER_THRESHOLD_CP   = -75
    private const val IMPULSE_TIME_THRESHOLD_SECONDS = 5   // moves played faster than this qualify as impulsive
    private const val IMPULSE_CP_LOSS_THRESHOLD      = 200 // centipawn loss that makes a fast move a coached event
    private const val CANDIDATE_SEARCH_MIN_CP        = 50  // |eval| lower bound for "rich with plans" zone
    private const val CANDIDATE_SEARCH_MAX_CP        = 300 // above this the position is near-decisive, not a plan choice
    private const val CCT_CHECK_EVAL_SHIFT_CP        = 100 // any shift ≥ 1.0 pawn triggers CCT habit reminder
    // Rook Activation is a middlegame concept — suppress it entirely during the opening.
    // Half-move 24 ≈ move 12 per side; requiring 2 developed minors guards against rooks
    // that are still physically blocked by unmoved knights/bishops.
    private const val ROOK_ACTIVATION_MIN_HALF_MOVE       = 24
    private const val ROOK_ACTIVATION_MIN_DEVELOPED_MINORS = 2
    // ForcingMove only fires when the mover missed a significant tactical opportunity.
    // Suppress when the played move itself was good (e.g., the user captured the hanging piece).
    private const val FORCING_MOVE_MIN_CP_LOSS       = 100
    // PreMoveChecklist is a habit trigger — suppress it when the move played was not actually bad.
    private const val PRE_MOVE_CHECKLIST_MIN_CP_LOSS = 100
    // When the player holds a significant advantage, suppress Development/Positional triggers and
    // promote conversion coaching instead. Configurable — 500 cp = 5.0 pawns.
    private const val CONVERSION_ADVANTAGE_THRESHOLD_CP = 500

    // Coordinated Attack: 3+ non-pawn pieces aimed at the king zone = coordinated attack established.
    // Attack dissolved when the count drops to ≤ 1.
    private const val KING_ATTACK_FIRE_THRESHOLD  = 3
    private const val KING_ATTACK_LOSS_THRESHOLD  = 1
    // Piece Harmony: general coordination score (overlap squares) thresholds.
    // Require a minimum delta to avoid noise from tiny fluctuations.
    private const val HARMONY_FIRE_THRESHOLD      = 6
    private const val HARMONY_LOSS_THRESHOLD      = 3
    private const val HARMONY_MIN_DELTA           = 2
    // Coordination triggers (PieceHarmony, CoordinatedAttack) must be backed by a real eval edge.
    // If the position is near-equal, a geometry transition is noise, not a coaching signal.
    // Also suppressed when the mover just blundered — opponent coordination gain is a consequence,
    // not an independent pattern worth coaching.
    private const val COORDINATION_EVAL_MIN_ADVANTAGE_CP = 50
    private const val COORDINATION_BLUNDER_SUPPRESS_CP   = 150

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

        // Coordination state — carried across moves to detect transitions only
        var prevPlayerKingAttack   = 0
        var prevOpponentKingAttack = 0
        var prevPlayerHarmony      = 0
        var prevOpponentHarmony    = 0

        sorted.forEachIndexed { i, eval ->
            val triggers   = mutableListOf<CoachingTrigger>()
            val prevEval   = if (i > 0) sorted[i - 1] else null
            val isWhite    = eval.moveIndex % 2 == 1
            val moverLoss  = if (isWhite) -eval.evalDelta else eval.evalDelta

            // Only load the board once and reuse for multiple triggers
            val board: Board? = runCatching {
                val fen = fenByMoveIndex(eval.moveIndex)
                if (fen.isBlank()) null else Board().apply { loadFromFen(fen) }
            }.getOrNull()

            // ── 1. Safety ─────────────────────────────────────────────────────
            if (board != null && eval.moveIndex in MIDDLEGAME_START..MIDDLEGAME_END) {
                val prevBoard: Board? = if (prevEval != null) runCatching {
                    val fen = fenByMoveIndex(prevEval.moveIndex)
                    if (fen.isBlank()) null else Board().apply { loadFromFen(fen) }
                }.getOrNull() else null
                detectSafety(board, eval, playerIsWhite, prevBoard)?.let { triggers.add(it) }
            }

            // ── 2. Candidate Moves ────────────────────────────────────────────
            if (eval.moveIndex in MIDDLEGAME_START..MIDDLEGAME_END) {
                detectCandidate(eval, isWhite)?.let { triggers.add(it) }
            }

            // ── 3. Worst Piece ────────────────────────────────────────────────
            if (board != null && eval.moveIndex in MIDDLEGAME_START..MIDDLEGAME_END) {
                detectWorstPiece(board, eval.moveIndex, playerIsWhite, worstPieceStreak)
                    ?.let { triggers.add(it) }
            } else if (eval.moveIndex !in MIDDLEGAME_START..MIDDLEGAME_END) {
                worstPieceStreak.clear()
            }

            // ── 4. Forcing Move ───────────────────────────────────────────────
            // Suppress in the opening unless the mover actually blundered (≥75 cp drop).
            detectForcingMove(eval, isWhite, playerIsWhite, moverLoss)
                ?.takeIf { eval.moveIndex >= MIDDLEGAME_START || moverLoss >= -OPENING_TRIGGER_THRESHOLD_CP }
                ?.let { triggers.add(it) }

            // ── 5. Opponent's Plan ────────────────────────────────────────────
            if (prevEval != null) {
                detectOpponentPlan(eval, prevEval, isWhite)?.let { triggers.add(it) }
            }

            // ── 6. Pre-Move Checklist ─────────────────────────────────────────
            // Suppress in the opening unless there is a genuine mistake — tension
            // (gambits, accepted pawns) is intentional and should not be flagged.
            if (board != null) {
                detectPreMoveChecklist(board, eval, moverLoss)
                    ?.takeIf { eval.moveIndex >= MIDDLEGAME_START || moverLoss >= -OPENING_TRIGGER_THRESHOLD_CP }
                    ?.let { triggers.add(it) }
            }

            // ── 7. Rook Activation ────────────────────────────────────────────
            if (board != null && eval.moveIndex in ROOK_ACTIVATION_MIN_HALF_MOVE..MIDDLEGAME_END) {
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

            // ── 11. Coordination Triggers ─────────────────────────────────────────
            // Fire only on state transitions (gained / lost) to avoid repeating on
            // consecutive moves where coordination barely changes.
            //
            // Opponent-GAINING triggers (isLoss = false) are eval-gated: the opponent must
            // hold a genuine evaluation advantage AND the mover must not have just blundered.
            // This prevents geometry transitions caused by a tactical mistake from being
            // mislabelled as a coordination coaching moment.
            if (board != null && eval.moveIndex >= MIDDLEGAME_START) {
                val playerSide   = if (playerIsWhite) Side.WHITE else Side.BLACK
                val opponentSide = if (playerIsWhite) Side.BLACK else Side.WHITE

                val newPlayerKingAttack   = CoordinationAnalyzer.kingAttackScore(board, playerSide)
                val newOpponentKingAttack = CoordinationAnalyzer.kingAttackScore(board, opponentSide)
                val newPlayerHarmony      = CoordinationAnalyzer.generalCoordinationScore(board, playerSide)
                val newOpponentHarmony    = CoordinationAnalyzer.generalCoordinationScore(board, opponentSide)

                // Eval gate: coordination GAINING triggers require (a) the gaining side holds a
                // real eval edge, (b) the mover did not just blunder, (c) no piece is genuinely
                // hanging, and (d) there is no forcing tactical motif.
                // Conditions (c) and (d) prevent Ne5-style blunders from triggering coordination
                // coaching: a piece placed on a strong central square can temporarily boost the
                // harmony score even though it is about to be captured for free.
                val opponentEvalAdvantage  = if (playerIsWhite) -eval.evalCp else eval.evalCp
                val playerEvalAdvantage    = if (playerIsWhite) eval.evalCp  else -eval.evalCp
                val moverJustBlundered     = moverLoss >= COORDINATION_BLUNDER_SUPPRESS_CP
                val hasHangingPiece        = BoardAttackHelper.allPieces(board)
                    .filter { (_, piece) -> piece.pieceType != PieceType.KING }
                    .any    { (sq, piece) -> isGenuinelyHanging(board, sq, piece) }
                val isTacticallyClean      = !hasHangingPiece && eval.motif == "mixed"
                val opponentGainIsEvalBacked =
                    opponentEvalAdvantage >= COORDINATION_EVAL_MIN_ADVANTAGE_CP &&
                    !moverJustBlundered && isTacticallyClean
                val playerGainIsEvalBacked  =
                    playerEvalAdvantage   >= COORDINATION_EVAL_MIN_ADVANTAGE_CP &&
                    !moverJustBlundered && isTacticallyClean

                // Player king attack — gaining requires eval backing; loss always fires
                when {
                    newPlayerKingAttack >= KING_ATTACK_FIRE_THRESHOLD && prevPlayerKingAttack < KING_ATTACK_FIRE_THRESHOLD -> {
                        if (playerGainIsEvalBacked) {
                            val detail = CoordinationAnalyzer.kingAttackDetail(board, playerSide)
                            triggers.add(CoachingTrigger.CoordinatedAttack(eval.moveIndex, isPlayerSide = true, isLoss = false, pieceCount = newPlayerKingAttack, attackerSquares = detail.attackerSquares, targetSquare = detail.targetSquares.firstOrNull()))
                        }
                    }
                    newPlayerKingAttack <= KING_ATTACK_LOSS_THRESHOLD && prevPlayerKingAttack >= KING_ATTACK_FIRE_THRESHOLD ->
                        triggers.add(CoachingTrigger.CoordinatedAttack(eval.moveIndex, isPlayerSide = true, isLoss = true, pieceCount = prevPlayerKingAttack))
                }
                // Opponent king attack — gaining requires eval backing; loss always fires
                when {
                    newOpponentKingAttack >= KING_ATTACK_FIRE_THRESHOLD && prevOpponentKingAttack < KING_ATTACK_FIRE_THRESHOLD -> {
                        if (opponentGainIsEvalBacked) {
                            val detail = CoordinationAnalyzer.kingAttackDetail(board, opponentSide)
                            triggers.add(CoachingTrigger.CoordinatedAttack(eval.moveIndex, isPlayerSide = false, isLoss = false, pieceCount = newOpponentKingAttack, attackerSquares = detail.attackerSquares, targetSquare = detail.targetSquares.firstOrNull()))
                        }
                    }
                    newOpponentKingAttack <= KING_ATTACK_LOSS_THRESHOLD && prevOpponentKingAttack >= KING_ATTACK_FIRE_THRESHOLD ->
                        triggers.add(CoachingTrigger.CoordinatedAttack(eval.moveIndex, isPlayerSide = false, isLoss = true, pieceCount = prevOpponentKingAttack))
                }

                // Player piece harmony — gaining requires eval backing; loss always fires
                val playerAttackFired = triggers.any { it is CoachingTrigger.CoordinatedAttack && it.isPlayerSide }
                if (!playerAttackFired) {
                    val delta = newPlayerHarmony - prevPlayerHarmony
                    when {
                        newPlayerHarmony >= HARMONY_FIRE_THRESHOLD && prevPlayerHarmony < HARMONY_FIRE_THRESHOLD && delta >= HARMONY_MIN_DELTA -> {
                            if (playerGainIsEvalBacked) {
                                val detail = CoordinationAnalyzer.harmonyDetail(board, playerSide)
                                triggers.add(CoachingTrigger.PieceHarmony(eval.moveIndex, isPlayerSide = true, isLoss = false, score = newPlayerHarmony, attackerSquares = detail.attackerSquares, targetSquares = detail.targetSquares))
                            }
                        }
                        newPlayerHarmony <= HARMONY_LOSS_THRESHOLD && prevPlayerHarmony >= HARMONY_FIRE_THRESHOLD && kotlin.math.abs(delta) >= HARMONY_MIN_DELTA ->
                            triggers.add(CoachingTrigger.PieceHarmony(eval.moveIndex, isPlayerSide = true, isLoss = true, score = prevPlayerHarmony))
                    }
                }

                // Opponent piece harmony — gaining requires eval backing; loss always fires
                val opponentAttackFired = triggers.any { it is CoachingTrigger.CoordinatedAttack && !it.isPlayerSide }
                if (!opponentAttackFired) {
                    val delta = newOpponentHarmony - prevOpponentHarmony
                    when {
                        newOpponentHarmony >= HARMONY_FIRE_THRESHOLD && prevOpponentHarmony < HARMONY_FIRE_THRESHOLD && delta >= HARMONY_MIN_DELTA -> {
                            if (opponentGainIsEvalBacked) {
                                val detail = CoordinationAnalyzer.harmonyDetail(board, opponentSide)
                                triggers.add(CoachingTrigger.PieceHarmony(eval.moveIndex, isPlayerSide = false, isLoss = false, score = newOpponentHarmony, attackerSquares = detail.attackerSquares, targetSquares = detail.targetSquares))
                            }
                        }
                        newOpponentHarmony <= HARMONY_LOSS_THRESHOLD && prevOpponentHarmony >= HARMONY_FIRE_THRESHOLD && kotlin.math.abs(delta) >= HARMONY_MIN_DELTA ->
                            triggers.add(CoachingTrigger.PieceHarmony(eval.moveIndex, isPlayerSide = false, isLoss = true, score = prevOpponentHarmony))
                    }
                }

                prevPlayerKingAttack   = newPlayerKingAttack
                prevOpponentKingAttack = newOpponentKingAttack
                prevPlayerHarmony      = newPlayerHarmony
                prevOpponentHarmony    = newOpponentHarmony
            }

            // ── Post-processing: tactical blunder suppression ─────────────────────
            // When the mover just blundered (≥ COORDINATION_BLUNDER_SUPPRESS_CP), all
            // tier-3/4 positional triggers are noise. Tier-1 tactical triggers
            // (ForcingMove, PreMoveChecklist, Safety, ImpulseControl) and tier-2
            // (CctCheck) are the appropriate coaching voice at such positions.
            if (moverLoss >= COORDINATION_BLUNDER_SUPPRESS_CP) {
                triggers.removeAll {
                    it is CoachingTrigger.PieceHarmony       ||
                    it is CoachingTrigger.CoordinatedAttack  ||
                    it is CoachingTrigger.WorstPiece         ||
                    it is CoachingTrigger.CandidateMoves     ||
                    it is CoachingTrigger.CandidateSearch    ||
                    it is CoachingTrigger.RookActivation     ||
                    it is CoachingTrigger.OpponentPlan
                }
            }

            // ── 12. Conversion Strategy / Advantage Handler ───────────────────────
            // When the position is clearly decided (either side ahead > 4.0 pawns),
            // positional development coaching is a distraction. Suppress those triggers
            // and, when it is the player's turn and they are the winning side, inject a
            // conversion-focused coaching prompt instead.
            val evalFromPlayer = if (playerIsWhite) eval.evalCp else -eval.evalCp
            if (kotlin.math.abs(eval.evalCp) > CONVERSION_ADVANTAGE_THRESHOLD_CP) {
                triggers.removeAll {
                    it is CoachingTrigger.RookActivation    ||
                    it is CoachingTrigger.CandidateMoves    ||
                    it is CoachingTrigger.WorstPiece        ||
                    it is CoachingTrigger.CandidateSearch   ||
                    it is CoachingTrigger.OpponentPlan      ||
                    it is CoachingTrigger.CoordinatedAttack ||
                    it is CoachingTrigger.PieceHarmony
                }
            }
            if (evalFromPlayer > CONVERSION_ADVANTAGE_THRESHOLD_CP) {
                triggers.add(CoachingTrigger.ConversionStrategy(eval.moveIndex, eval.evalCp))
            }

            if (triggers.isNotEmpty()) {
                // Tier-based prioritization: keep only the highest-priority tier that fired.
                // ConversionStrategy (tier 0) is immune — already injected above after clearing
                // lower-priority triggers, so it is never filtered here.
                // Within a tier, all triggers are kept (e.g., Safety + ForcingMove can co-exist).
                val nonConversion = triggers.filter { it !is CoachingTrigger.ConversionStrategy }
                if (nonConversion.isNotEmpty()) {
                    val highestTier: Int = nonConversion.minOf { it.tier() }
                    triggers.removeAll { it !is CoachingTrigger.ConversionStrategy && it.tier() != highestTier }
                }
                if (triggers.isNotEmpty()) {
                    result[eval.moveIndex] = triggers
                }
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

    private fun detectSafety(
        board: Board,
        eval: GameEvaluation,
        isWhite: Boolean,
        prevBoard: Board? = null,
    ): CoachingTrigger.Safety? {
        val movingSide   = if (isWhite) Side.WHITE else Side.BLACK
        val opponentSide = if (isWhite) Side.BLACK else Side.WHITE
        val kingSquare   = BoardAttackHelper.kingSquare(board, movingSide) ?: return null

        // Count friendly pieces in the 8 adjacent squares (including pawns)
        val adjacent      = adjacentSquares(kingSquare)
        val defenderCount = adjacent.count { sq ->
            val p = board.getPiece(sq)
            p != Piece.NONE && p.pieceSide == movingSide && p.pieceType != PieceType.KING
        }

        // Structural guard: king must already be short on defenders
        if (defenderCount > KING_MIN_ADJACENT_DEFENDERS) return null

        // Check for immediate tactical danger: opponent pieces attacking king-adjacent squares.
        // Build a map of adjacent square → attacker count so we can name the focal point.
        val attacksBySquare: Map<Square, Int> = adjacent.associateWith { sq ->
            BoardAttackHelper.attackersOf(board, sq, opponentSide).size
        }
        val mostAttackedEntry = attacksBySquare.maxByOrNull { (_, cnt) -> cnt }
        val hasDirectAttack   = (mostAttackedEntry?.value ?: 0) > 0

        // Distinguish structural exposure from real tactical danger:
        //   - Significant evaluation drop (≥ SAFETY_MIN_CP_DROP) → real consequence visible in engine
        //   - Opponent piece directly attacking a king-adjacent square AND the move worsened it
        // Either condition alone justifies the coaching prompt; neither alone is structural.
        val moverDelta        = if (isWhite) eval.evalDelta else -eval.evalDelta
        val isSignificantDrop = moverDelta <= -SAFETY_MIN_CP_DROP

        if (!isSignificantDrop && !hasDirectAttack) return null

        // Delta guard for direct-attack path: if the eval drop is below the threshold, only fire
        // when the move itself *worsened* king safety (attacker count increased). Pre-existing
        // structural exposure (king already on f2, attacks already present) must not trigger
        // SAFETY for unrelated developmental moves like Bb5.
        if (hasDirectAttack && !isSignificantDrop && prevBoard != null) {
            val prevKingSquare   = BoardAttackHelper.kingSquare(prevBoard, movingSide) ?: kingSquare
            val prevAdjacent     = adjacentSquares(prevKingSquare)
            val prevMaxAttackers = prevAdjacent.maxOfOrNull { sq ->
                BoardAttackHelper.attackersOf(prevBoard, sq, opponentSide).size
            } ?: 0
            val currentMaxAttackers = mostAttackedEntry?.value ?: 0
            // Attack level did not increase — this is pre-existing exposure, not caused by the move.
            if (currentMaxAttackers <= prevMaxAttackers) return null
        }

        val threatSquare = if (hasDirectAttack) mostAttackedEntry?.key?.name else null
        return CoachingTrigger.Safety(eval.moveIndex, kingSquare.name, threatSquare)
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

    private fun detectForcingMove(
        eval: GameEvaluation,
        isWhite: Boolean,
        playerIsWhite: Boolean,
        moverLoss: Int,
    ): CoachingTrigger.ForcingMove? {
        if (eval.motif !in listOf("fork", "hanging", "checkmate")) return null
        // ForcingMove coaches the player who is ABOUT TO MOVE after the opponent blundered.
        // The player is to move only at positions created by the opponent's last move:
        //   Black player → fires after White's moves (isWhite=true, playerIsWhite=false)
        //   White player → fires after Black's moves (isWhite=false, playerIsWhite=true)
        val isPlayerToMove = isWhite != playerIsWhite
        if (!isPlayerToMove) return null
        // Only fire when the mover (opponent) actually dropped significant material.
        // moverLoss is perspective-correct: positive = mover lost centipawns.
        if (moverLoss < FORCING_MOVE_MIN_CP_LOSS) return null
        return CoachingTrigger.ForcingMove(eval.moveIndex, eval.motif)
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

    private fun detectPreMoveChecklist(
        board: Board,
        eval: GameEvaluation,
        moverLoss: Int,
    ): CoachingTrigger.PreMoveChecklist? {
        // Suppress on neutral or strong moves — the habit prompt only makes sense when
        // the mover actually dropped significant material, leaving a piece hanging.
        // moverLoss is perspective-correct: positive = mover lost centipawns.
        if (moverLoss < PRE_MOVE_CHECKLIST_MIN_CP_LOSS) return null

        val hangingSquare = BoardAttackHelper.allPieces(board)
            .filter { (_, piece) -> piece.pieceType != PieceType.KING }
            .firstOrNull { (sq, piece) -> isGenuinelyHanging(board, sq, piece) }
            ?.first

        return if (hangingSquare != null) {
            CoachingTrigger.PreMoveChecklist(eval.moveIndex, hangingSquare.name)
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
            val rookRank = BoardAttackHelper.rankOf(rookSq)

            // Rooks still on their back-rank corner squares are physically blocked by
            // undeveloped minor pieces. Don't nag the user until those pieces have moved.
            val homeRank = if (side == Side.WHITE) 0 else 7
            val isOnStartSquare = rookRank == homeRank && (rookFile == 0 || rookFile == 7)
            if (isOnStartSquare && countDevelopedMinors(board, side) < ROOK_ACTIVATION_MIN_DEVELOPED_MINORS) continue

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

    /**
     * Counts how many of [side]'s minor pieces (knights and bishops) have left their
     * starting squares. A piece is considered "developed" when its home square is no
     * longer occupied by the expected piece (captured pieces also count as vacated).
     */
    private fun countDevelopedMinors(board: Board, side: Side): Int {
        val homeSquaresToPiece = if (side == Side.WHITE)
            listOf(
                Square.B1 to com.github.bhlangonijr.chesslib.Piece.WHITE_KNIGHT,
                Square.G1 to com.github.bhlangonijr.chesslib.Piece.WHITE_KNIGHT,
                Square.C1 to com.github.bhlangonijr.chesslib.Piece.WHITE_BISHOP,
                Square.F1 to com.github.bhlangonijr.chesslib.Piece.WHITE_BISHOP,
            )
        else
            listOf(
                Square.B8 to com.github.bhlangonijr.chesslib.Piece.BLACK_KNIGHT,
                Square.G8 to com.github.bhlangonijr.chesslib.Piece.BLACK_KNIGHT,
                Square.C8 to com.github.bhlangonijr.chesslib.Piece.BLACK_BISHOP,
                Square.F8 to com.github.bhlangonijr.chesslib.Piece.BLACK_BISHOP,
            )
        return homeSquaresToPiece.count { (sq, expectedPiece) -> board.getPiece(sq) != expectedPiece }
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
