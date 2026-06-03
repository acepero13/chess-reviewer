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
    private const val IMPULSE_TIME_THRESHOLD_SECONDS      = 10  // moves played faster than this qualify as impulsive
    private const val IMPULSE_CP_LOSS_THRESHOLD            = 200 // centipawn loss that makes a fast move a coached event
    private const val CALCULATION_BLUNDER_TIME_THRESHOLD_SECONDS = 30 // moves taking longer than this qualify as slow-think blunders
    // TacticalOversight fills the gap between ImpulseControl and CalculationBlunder:
    // a normal-paced move (5–30 s) that still caused a large tactical loss.
    private const val TACTICAL_OVERSIGHT_MIN_SECONDS = IMPULSE_TIME_THRESHOLD_SECONDS
    private const val TACTICAL_OVERSIGHT_MAX_SECONDS = CALCULATION_BLUNDER_TIME_THRESHOLD_SECONDS
    private const val CANDIDATE_SEARCH_MIN_CP        = 50  // |eval| lower bound for "rich with plans" zone
    private const val CANDIDATE_SEARCH_MAX_CP        = 300 // above this the position is near-decisive, not a plan choice
    private const val CCT_CHECK_EVAL_SHIFT_CP        = 100 // any shift ≥ 1.0 pawn triggers CCT habit reminder
    // Tier 4 triggers (positional habits: WorstPiece, RookActivation, CandidateMoves,
    // CandidateSearch) are only meaningful once both sides have largely completed development.
    // Half-move 30 ≈ move 15 per side — pieces are expected to be undeveloped before that.
    private const val TIER4_OPENING_GATE                  = 30

    // Rook Activation is a middlegame concept — suppress it entirely during the opening.
    // Aligned with TIER4_OPENING_GATE; requiring 2 developed minors guards against rooks
    // that are still physically blocked by unmoved knights/bishops.
    private const val ROOK_ACTIVATION_MIN_HALF_MOVE       = 30
    private const val ROOK_ACTIVATION_MIN_DEVELOPED_MINORS = 2

    // CandidateSearch: if the engine's best move is clearly head-and-shoulders above
    // alternatives (moverLoss ≥ this value), there is ONE right answer — the student
    // should calculate, not browse plans.
    private const val CANDIDATE_SEARCH_CLARITY_CP         = 150
    // ForcingMove only fires when the mover missed a significant tactical opportunity.
    // Suppress when the played move itself was good (e.g., the user captured the hanging piece).
    // 150 cp prevents ordinary bad exchanges (≈1 pawn) from triggering a "find the tactic" prompt.
    private const val FORCING_MOVE_MIN_CP_LOSS             = 150
    // After the opponent's mistake, the player must also hold a real advantage before we ask them
    // to "find the forcing sequence". Without this gate a bad exchange in an otherwise equal or
    // worse position fires ForcingMove even though no clear tactic exists.
    private const val FORCING_MOVE_MIN_PLAYER_ADVANTAGE_CP = 150
    // PreMoveChecklist is a habit trigger — suppress it when the move played was not actually bad.
    // Also suppress when the loss is catastrophically large: a ≥200cp loss means the player
    // already committed a terminal tactical error; the hanging piece detected is the blundered
    // piece itself, so a mild "before you commit" reminder is retroactively wrong.
    private const val PRE_MOVE_CHECKLIST_MIN_CP_LOSS = 100
    private const val PRE_MOVE_CHECKLIST_MAX_CP_LOSS = 200
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
    // PunishBlunder fires when the opponent's move caused a ≥ 120 cp loss from their perspective.
    // Mutually exclusive with ForcingMove (ForcingMove is more specific — motif-backed).
    private const val PUNISH_BLUNDER_MIN_CP_LOSS = 120

    // Coordination triggers (PieceHarmony, CoordinatedAttack) must be backed by a real eval edge.
    // If the position is near-equal, a geometry transition is noise, not a coaching signal.
    // Also suppressed when the mover just blundered — opponent coordination gain is a consequence,
    // not an independent pattern worth coaching.
    private const val COORDINATION_EVAL_MIN_ADVANTAGE_CP = 50
    private const val COORDINATION_BLUNDER_SUPPRESS_CP   = 150

    // EvalCalibration: quiz the user's positional assessment at stable, non-tactical positions.
    // POST_OPENING window: half-moves 28–44 (≈ move 14–22 per side).
    // EVAL_JUMP: eval crosses from within the "equal" band into an "advantage" band.
    private const val CALIBRATION_POST_OPENING_MIN  = 28
    private const val CALIBRATION_POST_OPENING_MAX  = 44
    private const val CALIBRATION_EVAL_JUMP_FROM_CP = 50   // inner edge of "equal" zone
    private const val CALIBRATION_EVAL_JUMP_TO_CP   = 150  // outer edge entering "advantage" zone
    private const val CALIBRATION_FREQUENCY_CAP     = 15   // minimum half-moves between calibrations
    private const val CALIBRATION_VOLATILITY_MAX_CP = 50   // avg |evalDelta| over last 3 moves
    private const val CALIBRATION_MAX_EVAL_CP       = 300  // skip lopsided positions (>3 pawns)

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
        gameId: Long = 0L,
        weakTriggerTypes: Set<String> = emptySet(),
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

        // Sliding window of the last 10 evalDeltas — powers the Volatility metric
        val recentEvalDeltas = ArrayDeque<Int>(10)
        // Sliding window of the last 6 evalCp values — powers the Decisiveness Slope metric
        val recentEvalCps    = ArrayDeque<Int>(6)

        // Calibration frequency cap: tracks the last half-move index where calibration fired.
        // Initialised far enough in the past so the first eligible position can fire.
        var lastCalibrationAt = -CALIBRATION_FREQUENCY_CAP * 2

        sorted.forEachIndexed { i, eval ->
            val triggers   = mutableListOf<CoachingTrigger>()
            val prevEval   = if (i > 0) sorted[i - 1] else null
            val isWhite    = eval.moveIndex % 2 == 1
            val moverLoss  = if (isWhite) -eval.evalDelta else eval.evalDelta

            // Debug prefix shared by all per-trigger logs for this position
            val pfx = "[game=$gameId move=${eval.moveIndex} ${if (isWhite) "W" else "B"} evalCp=${eval.evalCp} delta=${eval.evalDelta} moverLoss=$moverLoss motif=${eval.motif}]"
            Log.d(TAG, "$pfx --- evaluating triggers ---")

            // ── Update rolling windows ────────────────────────────────────────────
            if (recentEvalDeltas.size >= 10) recentEvalDeltas.removeFirst()
            recentEvalDeltas.addLast(eval.evalDelta)
            if (recentEvalCps.size >= 6) recentEvalCps.removeFirst()
            recentEvalCps.addLast(eval.evalCp)

            // Load the board once and reuse for multiple triggers + pressure metric
            val board: Board? = runCatching {
                val fen = fenByMoveIndex(eval.moveIndex)
                if (fen.isBlank()) null else Board().apply { loadFromFen(fen) }
            }.getOrNull()

            // ── Build GameNarrativeContext for this position ───────────────────────
            val pressure = if (board != null) computePressureScore(board) else 0f
            val context  = buildNarrativeContext(
                evalCp           = eval.evalCp,
                moverLoss        = moverLoss,
                motif            = eval.motif,
                recentDeltas     = recentEvalDeltas,
                recentCps        = recentEvalCps,
                pressure         = pressure,
                playerIsWhite    = playerIsWhite,
                isWhiteMove      = isWhite,
                weakTriggerTypes = weakTriggerTypes,
            )
            Log.d(TAG, "$pfx Narrative: volatility=${context.volatility} pressure=${context.pressure} slope=${context.playerEvalSlope} complexityGap=${context.complexityGap} volatile=${context.isVolatile} highPressure=${context.isHighPressure} cruising=${context.isCruising} stumbling=${context.isStumbling}")

            // ── 1. Safety ─────────────────────────────────────────────────────
            if (board != null && eval.moveIndex in MIDDLEGAME_START..MIDDLEGAME_END) {
                val prevBoard: Board? = if (prevEval != null) runCatching {
                    val fen = fenByMoveIndex(prevEval.moveIndex)
                    if (fen.isBlank()) null else Board().apply { loadFromFen(fen) }
                }.getOrNull() else null
                val t = detectSafety(board, eval, playerIsWhite, prevBoard, pfx)
                if (t != null) { triggers.add(t); Log.d(TAG, "$pfx Safety: FIRE king=${t.kingSquare} threat=${t.threatSquare}") }
                else Log.d(TAG, "$pfx Safety: SUPPRESS (see above for reason)")
            } else {
                Log.d(TAG, "$pfx Safety: SKIP moveIndex not in middlegame range or board=null")
            }

            // ── 2. Candidate Moves ────────────────────────────────────────────
            // Also suppressed when cruising (player is executing a plan) or when there is a
            // clearly forced best move (player should calculate, not browse plans).
            if (eval.moveIndex in TIER4_OPENING_GATE..MIDDLEGAME_END
                && !context.isSuppressTier4
                && !context.hasForcedBestMove
                && !context.isCruising) {
                val t = detectCandidate(eval, isWhite, pfx)
                if (t != null) { triggers.add(t); Log.d(TAG, "$pfx CandidateMoves: FIRE evalCp=${t.evalCp}") }
            } else if (context.isSuppressTier4) {
                Log.d(TAG, "$pfx CandidateMoves: SKIP board hot (volatile=${context.isVolatile} pressure=${context.isHighPressure})")
            } else if (context.hasForcedBestMove) {
                Log.d(TAG, "$pfx CandidateMoves: SKIP forced best move (complexityGap=${context.complexityGap})")
            } else if (context.isCruising) {
                Log.d(TAG, "$pfx CandidateMoves: SKIP player cruising (slope=${context.playerEvalSlope})")
            } else {
                Log.d(TAG, "$pfx CandidateMoves: SKIP opening gate or outside range (moveIndex=${eval.moveIndex})")
            }

            // ── 3. Worst Piece ────────────────────────────────────────────────
            if (board != null && eval.moveIndex in TIER4_OPENING_GATE..MIDDLEGAME_END && !context.isSuppressTier4) {
                val t = detectWorstPiece(board, eval.moveIndex, playerIsWhite, worstPieceStreak, pfx)
                if (t != null) { triggers.add(t); Log.d(TAG, "$pfx WorstPiece: FIRE sq=${t.pieceSquare} mobility=${t.mobility}") }
            } else {
                if (eval.moveIndex < TIER4_OPENING_GATE) {
                    worstPieceStreak.clear()
                    Log.d(TAG, "$pfx WorstPiece: SKIP opening gate (moveIndex=${eval.moveIndex} < $TIER4_OPENING_GATE)")
                } else if (context.isSuppressTier4) {
                    Log.d(TAG, "$pfx WorstPiece: SKIP board hot (volatile=${context.isVolatile} pressure=${context.isHighPressure})")
                } else {
                    worstPieceStreak.clear()
                    Log.d(TAG, "$pfx WorstPiece: SKIP outside middlegame range")
                }
            }

            // ── 4. Forcing Move ───────────────────────────────────────────────
            // Suppress in the opening unless the mover actually blundered (≥75 cp drop).
            val rawForcing = detectForcingMove(eval, isWhite, playerIsWhite, moverLoss, pfx)
            val forcing = rawForcing?.takeIf { eval.moveIndex >= MIDDLEGAME_START || moverLoss >= -OPENING_TRIGGER_THRESHOLD_CP }
            if (forcing != null) {
                triggers.add(forcing)
                Log.d(TAG, "$pfx ForcingMove: FIRE motif=${forcing.motif}")
            } else if (rawForcing != null) {
                Log.d(TAG, "$pfx ForcingMove: SUPPRESS opening phase + moverLoss=$moverLoss < ${-OPENING_TRIGGER_THRESHOLD_CP}")
            }

            // ── 5. Opponent's Plan ────────────────────────────────────────────
            if (prevEval != null && isWhite != playerIsWhite) {
                val t = detectOpponentPlan(eval, prevEval, isWhite, pfx)
                if (t != null) { triggers.add(t); Log.d(TAG, "$pfx OpponentPlan: FIRE gain=${t.evalGain}") }
            } else {
                Log.d(TAG, "$pfx OpponentPlan: SKIP isPlayerMove=${isWhite == playerIsWhite} prevEval=${prevEval != null}")
            }

            // ── 6. Pre-Move Checklist ─────────────────────────────────────────
            // Suppress in the opening unless there is a genuine mistake — tension
            // (gambits, accepted pawns) is intentional and should not be flagged.
            if (board != null) {
                val rawPmc = detectPreMoveChecklist(board, eval, moverLoss, pfx)
                val pmc = rawPmc?.takeIf { eval.moveIndex >= MIDDLEGAME_START || moverLoss >= -OPENING_TRIGGER_THRESHOLD_CP }
                if (pmc != null) {
                    triggers.add(pmc)
                    Log.d(TAG, "$pfx PreMoveChecklist: FIRE hangingSq=${pmc.hangingSquare}")
                } else if (rawPmc != null) {
                    Log.d(TAG, "$pfx PreMoveChecklist: SUPPRESS opening phase + moverLoss=$moverLoss < ${-OPENING_TRIGGER_THRESHOLD_CP}")
                }
            } else {
                Log.d(TAG, "$pfx PreMoveChecklist: SKIP board=null")
            }

            // ── 7. Rook Activation ────────────────────────────────────────────
            // Also suppressed when board pressure is high — closed files under tension are
            // not a strategic choice but a product of the position's complexity.
            if (board != null && eval.moveIndex in ROOK_ACTIVATION_MIN_HALF_MOVE..MIDDLEGAME_END
                && !context.isHighPressure) {
                val t = detectRookActivation(board, eval.moveIndex, playerIsWhite, pfx)
                if (t != null) { triggers.add(t); Log.d(TAG, "$pfx RookActivation: FIRE rookSq=${t.rookSquare} betterFile=${t.openFileIndex}") }
            } else if (context.isHighPressure) {
                Log.d(TAG, "$pfx RookActivation: SKIP high board pressure (pressure=${context.pressure})")
            } else {
                Log.d(TAG, "$pfx RookActivation: SKIP moveIndex=${eval.moveIndex} not in [$ROOK_ACTIVATION_MIN_HALF_MOVE..$MIDDLEGAME_END] or board=null")
            }

            // ── 8. Impulse Control ────────────────────────────────────────────
            // Fast move (< 5 s) that caused a significant evaluation drop.
            // Only coach the player's own impulsive moves, not the opponent's.
            if (isWhite == playerIsWhite) {
                val t = detectImpulseControl(eval, isWhite, timeByMoveIndex, pfx)
                if (t != null) { triggers.add(t); Log.d(TAG, "$pfx ImpulseControl: FIRE time=${t.timeSpentSeconds}s cpLoss=${t.cpLoss}") }
            } else {
                Log.d(TAG, "$pfx ImpulseControl: SKIP opponent's move")
            }

            // ── 8b. Calculation Blunder ───────────────────────────────────────
            // Slow move (> 30 s) that still caused a significant evaluation drop —
            // signals a visualization or calculation failure, not a time-pressure issue.
            // Only fires on the player's own moves; mutually exclusive with ImpulseControl.
            if (isWhite == playerIsWhite) {
                val t = detectCalculationBlunder(eval, isWhite, timeByMoveIndex, pfx)
                if (t != null) { triggers.add(t); Log.d(TAG, "$pfx CalculationBlunder: FIRE time=${t.timeSpentSeconds}s cpLoss=${t.cpLoss}") }
            } else {
                Log.d(TAG, "$pfx CalculationBlunder: SKIP opponent's move")
            }

            // ── 8c. Tactical Oversight ────────────────────────────────────────
            // Normal-paced move (5–30 s) that still caused a large evaluation drop —
            // fills the gap between ImpulseControl and CalculationBlunder.
            // Only fires on the player's own moves.
            if (isWhite == playerIsWhite) {
                val t = detectTacticalOversight(eval, isWhite, timeByMoveIndex, pfx)
                if (t != null) { triggers.add(t); Log.d(TAG, "$pfx TacticalOversight: FIRE time=${t.timeSpentSeconds}s cpLoss=${t.cpLoss}") }
            } else {
                Log.d(TAG, "$pfx TacticalOversight: SKIP opponent's move")
            }

            // ── 8d. Punish Blunder ────────────────────────────────────────
            // Fires when the opponent's move was a clear blunder (≥ 150 cp loss from their perspective).
            // Mutually exclusive with ForcingMove — suppressed in post-processing when ForcingMove fires.
            if (isWhite != playerIsWhite && eval.moveIndex >= MIDDLEGAME_START) {
                val t = detectPunishBlunder(eval, moverLoss, pfx)
                if (t != null) { triggers.add(t); Log.d(TAG, "$pfx PunishBlunder: FIRE opponentLoss=${t.opponentLoss}") }
            } else if (isWhite == playerIsWhite) {
                Log.d(TAG, "$pfx PunishBlunder: SKIP player's own move")
            }

            // ── 9. Candidate Search ───────────────────────────────────────────
            // Moderately complex position with no forcing sequence — multiple plans exist.
            // Promoted when the player is stumbling (losing a won game — needs to find a plan).
            // Suppressed when cruising or when one move is clearly forced.
            if (eval.moveIndex in TIER4_OPENING_GATE..MIDDLEGAME_END
                && !context.isSuppressTier4
                && !context.hasForcedBestMove
                && !context.isCruising) {
                val t = detectCandidateSearch(eval, isWhite, playerIsWhite, moverLoss, pfx)
                if (t != null) {
                    triggers.add(t)
                    Log.d(TAG, "$pfx CandidateSearch: FIRE evalCp=${t.evalCp} stumbling=${context.isStumbling}")
                }
            } else if (context.isSuppressTier4) {
                Log.d(TAG, "$pfx CandidateSearch: SKIP board hot (volatile=${context.isVolatile} pressure=${context.isHighPressure})")
            } else if (context.hasForcedBestMove) {
                Log.d(TAG, "$pfx CandidateSearch: SKIP forced best move (complexityGap=${context.complexityGap})")
            } else if (context.isCruising) {
                Log.d(TAG, "$pfx CandidateSearch: SKIP player cruising (slope=${context.playerEvalSlope})")
            } else {
                Log.d(TAG, "$pfx CandidateSearch: SKIP opening gate or outside range (moveIndex=${eval.moveIndex})")
            }

            // ── 10. CCT Check ─────────────────────────────────────────────────
            // Fires when the OPPONENT just played a strong CCT move (significant eval gain for
            // them). Suppressed when the opponent blundered — a mover loss is already covered by
            // ForcingMove / PreMoveChecklist and carries no CCT coaching value.
            if (eval.moveIndex >= MIDDLEGAME_START) {
                val t = detectCctCheck(eval, isWhite, playerIsWhite, moverLoss, pfx)
                if (t != null) { triggers.add(t); Log.d(TAG, "$pfx CctCheck: FIRE evalDelta=${eval.evalDelta}") }
            } else {
                Log.d(TAG, "$pfx CctCheck: SKIP opening phase")
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

                Log.d(TAG, "$pfx Coordination context: playerKingAttack=$newPlayerKingAttack(prev=$prevPlayerKingAttack) opponentKingAttack=$newOpponentKingAttack(prev=$prevOpponentKingAttack) playerHarmony=$newPlayerHarmony(prev=$prevPlayerHarmony) opponentHarmony=$newOpponentHarmony(prev=$prevOpponentHarmony) blundered=$moverJustBlundered hanging=$hasHangingPiece tacticallyClean=$isTacticallyClean playerAdvantage=$playerEvalAdvantage opponentAdvantage=$opponentEvalAdvantage")

                // Player king attack — gaining requires eval backing; loss always fires
                when {
                    newPlayerKingAttack >= KING_ATTACK_FIRE_THRESHOLD && prevPlayerKingAttack < KING_ATTACK_FIRE_THRESHOLD -> {
                        if (playerGainIsEvalBacked) {
                            val detail = CoordinationAnalyzer.kingAttackDetail(board, playerSide)
                            triggers.add(CoachingTrigger.CoordinatedAttack(eval.moveIndex, isPlayerSide = true, isLoss = false, pieceCount = newPlayerKingAttack, attackerSquares = detail.attackerSquares, targetSquare = detail.targetSquares.firstOrNull()))
                            Log.d(TAG, "$pfx CoordinatedAttack(player gain): FIRE pieces=$newPlayerKingAttack")
                        } else {
                            Log.d(TAG, "$pfx CoordinatedAttack(player gain): SUPPRESS evalBacked=$playerGainIsEvalBacked (advantage=$playerEvalAdvantage blundered=$moverJustBlundered tacticallyClean=$isTacticallyClean)")
                        }
                    }
                    newPlayerKingAttack <= KING_ATTACK_LOSS_THRESHOLD && prevPlayerKingAttack >= KING_ATTACK_FIRE_THRESHOLD -> {
                        triggers.add(CoachingTrigger.CoordinatedAttack(eval.moveIndex, isPlayerSide = true, isLoss = true, pieceCount = prevPlayerKingAttack))
                        Log.d(TAG, "$pfx CoordinatedAttack(player loss): FIRE prevPieces=$prevPlayerKingAttack")
                    }
                    else -> Log.d(TAG, "$pfx CoordinatedAttack(player): SKIP no transition (attack=$newPlayerKingAttack prev=$prevPlayerKingAttack)")
                }
                // Opponent king attack — gaining requires eval backing; loss always fires
                when {
                    newOpponentKingAttack >= KING_ATTACK_FIRE_THRESHOLD && prevOpponentKingAttack < KING_ATTACK_FIRE_THRESHOLD -> {
                        if (opponentGainIsEvalBacked) {
                            val detail = CoordinationAnalyzer.kingAttackDetail(board, opponentSide)
                            triggers.add(CoachingTrigger.CoordinatedAttack(eval.moveIndex, isPlayerSide = false, isLoss = false, pieceCount = newOpponentKingAttack, attackerSquares = detail.attackerSquares, targetSquare = detail.targetSquares.firstOrNull()))
                            Log.d(TAG, "$pfx CoordinatedAttack(opponent gain): FIRE pieces=$newOpponentKingAttack")
                        } else {
                            Log.d(TAG, "$pfx CoordinatedAttack(opponent gain): SUPPRESS evalBacked=$opponentGainIsEvalBacked (advantage=$opponentEvalAdvantage blundered=$moverJustBlundered tacticallyClean=$isTacticallyClean)")
                        }
                    }
                    newOpponentKingAttack <= KING_ATTACK_LOSS_THRESHOLD && prevOpponentKingAttack >= KING_ATTACK_FIRE_THRESHOLD -> {
                        triggers.add(CoachingTrigger.CoordinatedAttack(eval.moveIndex, isPlayerSide = false, isLoss = true, pieceCount = prevOpponentKingAttack))
                        Log.d(TAG, "$pfx CoordinatedAttack(opponent loss): FIRE prevPieces=$prevOpponentKingAttack")
                    }
                    else -> Log.d(TAG, "$pfx CoordinatedAttack(opponent): SKIP no transition (attack=$newOpponentKingAttack prev=$prevOpponentKingAttack)")
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
                                Log.d(TAG, "$pfx PieceHarmony(player gain): FIRE score=$newPlayerHarmony delta=$delta")
                            } else {
                                Log.d(TAG, "$pfx PieceHarmony(player gain): SUPPRESS evalBacked=$playerGainIsEvalBacked (advantage=$playerEvalAdvantage blundered=$moverJustBlundered tacticallyClean=$isTacticallyClean)")
                            }
                        }
                        newPlayerHarmony <= HARMONY_LOSS_THRESHOLD && prevPlayerHarmony >= HARMONY_FIRE_THRESHOLD && kotlin.math.abs(delta) >= HARMONY_MIN_DELTA -> {
                            triggers.add(CoachingTrigger.PieceHarmony(eval.moveIndex, isPlayerSide = true, isLoss = true, score = prevPlayerHarmony))
                            Log.d(TAG, "$pfx PieceHarmony(player loss): FIRE prevScore=$prevPlayerHarmony delta=$delta")
                        }
                        else -> Log.d(TAG, "$pfx PieceHarmony(player): SKIP no transition (harmony=$newPlayerHarmony prev=$prevPlayerHarmony delta=${newPlayerHarmony - prevPlayerHarmony})")
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
                                Log.d(TAG, "$pfx PieceHarmony(opponent gain): FIRE score=$newOpponentHarmony delta=$delta")
                            } else {
                                Log.d(TAG, "$pfx PieceHarmony(opponent gain): SUPPRESS evalBacked=$opponentGainIsEvalBacked (advantage=$opponentEvalAdvantage blundered=$moverJustBlundered tacticallyClean=$isTacticallyClean)")
                            }
                        }
                        newOpponentHarmony <= HARMONY_LOSS_THRESHOLD && prevOpponentHarmony >= HARMONY_FIRE_THRESHOLD && kotlin.math.abs(delta) >= HARMONY_MIN_DELTA -> {
                            triggers.add(CoachingTrigger.PieceHarmony(eval.moveIndex, isPlayerSide = false, isLoss = true, score = prevOpponentHarmony))
                            Log.d(TAG, "$pfx PieceHarmony(opponent loss): FIRE prevScore=$prevOpponentHarmony delta=$delta")
                        }
                        else -> Log.d(TAG, "$pfx PieceHarmony(opponent): SKIP no transition (harmony=$newOpponentHarmony prev=$prevOpponentHarmony delta=${newOpponentHarmony - prevOpponentHarmony})")
                    }
                }

                prevPlayerKingAttack   = newPlayerKingAttack
                prevOpponentKingAttack = newOpponentKingAttack
                prevPlayerHarmony      = newPlayerHarmony
                prevOpponentHarmony    = newOpponentHarmony
            } else {
                Log.d(TAG, "$pfx Coordination: SKIP opening phase or board=null")
            }

            // ── Post-processing: King Safety Override ────────────────────────────
            // If the king is exposed (Safety fired), the player is in fight-or-flight mode.
            // Any Tier 3/4 coaching at this moment is noise — explicitly suppress it here
            // before the tier filter so the intent is unambiguous in the logs.
            if (triggers.any { it is CoachingTrigger.Safety }) {
                val suppressedBySafety = triggers.filter { it.tier() >= 3 }
                if (suppressedBySafety.isNotEmpty()) {
                    Log.d(TAG, "$pfx KingSafetyOverride: suppressing ${suppressedBySafety.map { it.typeName() }} (king exposed)")
                    triggers.removeAll { it.tier() >= 3 }
                }
            }

            // ── Post-processing: In-Check Override ───────────────────────────────
            // When the side-to-move is in check the player has no strategic freedom —
            // only moves that escape check are legal. Suppress tier-3/4 positional
            // coaching (rook activation, worst piece, coordination, plan selection)
            // unconditionally, even when the Safety trigger itself did not fire
            // (e.g. the eval drop was below the Safety threshold).
            if (board != null && board.isKingAttacked()) {
                val suppressedByCheck = triggers.filter { it.tier() >= 3 }
                if (suppressedByCheck.isNotEmpty()) {
                    Log.d(TAG, "$pfx InCheckOverride: suppressing ${suppressedByCheck.map { it.typeName() }} (side-to-move is in check)")
                    triggers.removeAll { it in suppressedByCheck }
                }
            }

            // ── Post-processing: high eval-swing suppression ──────────────────────
            // When the absolute eval change is large (|evalDelta| ≥ threshold), all
            // tier-3/4 positional triggers are noise — this applies symmetrically to
            // blunders AND brilliant moves. Tier-1 tactical triggers and tier-2 (CctCheck)
            // are the appropriate coaching voice at such positions.
            val evalSwing = kotlin.math.abs(eval.evalDelta)
            if (evalSwing >= COORDINATION_BLUNDER_SUPPRESS_CP) {
                val suppressed = triggers.filter {
                    it is CoachingTrigger.PieceHarmony       ||
                    it is CoachingTrigger.CoordinatedAttack  ||
                    it is CoachingTrigger.WorstPiece         ||
                    it is CoachingTrigger.CandidateMoves     ||
                    it is CoachingTrigger.CandidateSearch    ||
                    it is CoachingTrigger.RookActivation     ||
                    it is CoachingTrigger.OpponentPlan
                }
                if (suppressed.isNotEmpty()) {
                    Log.d(TAG, "$pfx EvalSwingSuppression: removing ${suppressed.map { it.typeName() }} (|evalDelta|=$evalSwing >= $COORDINATION_BLUNDER_SUPPRESS_CP)")
                }
                triggers.removeAll { it in suppressed }
            }

            // ── 12. Conversion Strategy / Advantage Handler ───────────────────────
            // When the position is clearly decided (either side ahead > 4.0 pawns),
            // positional development coaching is a distraction. Suppress those triggers
            // and, when it is the player's turn and they are the winning side, inject a
            // conversion-focused coaching prompt instead.
            val evalFromPlayer = if (playerIsWhite) eval.evalCp else -eval.evalCp
            if (kotlin.math.abs(eval.evalCp) > CONVERSION_ADVANTAGE_THRESHOLD_CP) {
                val suppressed = triggers.filter {
                    it is CoachingTrigger.RookActivation    ||
                    it is CoachingTrigger.CandidateMoves    ||
                    it is CoachingTrigger.WorstPiece        ||
                    it is CoachingTrigger.CandidateSearch   ||
                    it is CoachingTrigger.OpponentPlan      ||
                    it is CoachingTrigger.CoordinatedAttack ||
                    it is CoachingTrigger.PieceHarmony
                }
                if (suppressed.isNotEmpty()) {
                    Log.d(TAG, "$pfx ConversionSuppression: removing ${suppressed.map { it.typeName() }} (|evalCp|=${kotlin.math.abs(eval.evalCp)} > $CONVERSION_ADVANTAGE_THRESHOLD_CP)")
                }
                triggers.removeAll { it in suppressed }
            }
            // Only coach conversion when it is the player's own turn — not after an opponent blunder.
            // Firing on the opponent's record produces a mismatched coaching moment: the player
            // hasn't yet had a chance to act on the advantage they now hold.
            if (evalFromPlayer > CONVERSION_ADVANTAGE_THRESHOLD_CP && isWhite == playerIsWhite) {
                triggers.add(CoachingTrigger.ConversionStrategy(eval.moveIndex, eval.evalCp))
                Log.d(TAG, "$pfx ConversionStrategy: FIRE evalFromPlayer=$evalFromPlayer")
            } else {
                Log.d(TAG, "$pfx ConversionStrategy: SKIP evalFromPlayer=$evalFromPlayer playerTurn=${isWhite == playerIsWhite}")
            }

            // ── EvalCalibration ───────────────────────────────────────────────────
            // Only fires on the player's own moves so the question is relevant to what they just played.
            if (isWhite == playerIsWhite) {
                val prevEval3 = (0 until minOf(3, i)).map { k -> sorted[i - 1 - k] }
                val calibration = detectCalibration(eval, prevEval3, lastCalibrationAt, pfx)
                if (calibration != null) {
                    triggers.add(calibration)
                    Log.d(TAG, "$pfx EvalCalibration: FIRE context=${calibration.context} evalCp=${calibration.engineEvalCp}")
                }
            } else {
                Log.d(TAG, "$pfx EvalCalibration: SKIP opponent move")
            }

            // ── CctCheck / OpponentPlan mutual exclusion ──────────────────────────
            // Both describe "opponent played well" but CctCheck is the more specific,
            // habit-reinforcing trigger — suppress OpponentPlan when CctCheck fires.
            if (triggers.any { it is CoachingTrigger.CctCheck }) {
                val removed = triggers.filter { it is CoachingTrigger.OpponentPlan }
                if (removed.isNotEmpty()) {
                    Log.d(TAG, "$pfx CctOpponentMutex: removing OpponentPlan (CctCheck already fired)")
                }
                triggers.removeAll { it is CoachingTrigger.OpponentPlan }
            }

            // ── ForcingMove / PunishBlunder mutual exclusion ──────────────────────
            // ForcingMove is motif-backed and more specific; suppress PunishBlunder when it fires.
            if (triggers.any { it is CoachingTrigger.ForcingMove }) {
                val removed = triggers.filter { it is CoachingTrigger.PunishBlunder }
                if (removed.isNotEmpty()) {
                    Log.d(TAG, "$pfx ForcingPunishMutex: removing PunishBlunder (ForcingMove already fired)")
                }
                triggers.removeAll { it is CoachingTrigger.PunishBlunder }
            }

            // ── PunishBlunder / PreMoveChecklist mutual exclusion ─────────────────
            // PunishBlunder is more specific — the opponent left a piece hanging as part of
            // their blunder. Suppress the generic pre-move habit reminder so the targeted
            // "capitalize on their mistake" message is shown instead.
            if (triggers.any { it is CoachingTrigger.PunishBlunder }) {
                val removed = triggers.filter { it is CoachingTrigger.PreMoveChecklist }
                if (removed.isNotEmpty()) {
                    Log.d(TAG, "$pfx PunishPreMoveMutex: removing PreMoveChecklist (PunishBlunder already fired)")
                }
                triggers.removeAll { it is CoachingTrigger.PreMoveChecklist }
            }

            val preFilterTypes = triggers.map { it.typeName() }
            if (triggers.isNotEmpty()) {
                // Tier-based prioritization: keep only the highest-priority tier that fired.
                // ConversionStrategy (tier 0) is immune — already injected above after clearing
                // lower-priority triggers, so it is never filtered here.
                val nonConversion = triggers.filter { it !is CoachingTrigger.ConversionStrategy }
                if (nonConversion.isNotEmpty()) {
                    val highestTier: Int = nonConversion.minOf { it.tier() }
                    val tierRemoved = triggers.filter { it !is CoachingTrigger.ConversionStrategy && it.tier() != highestTier }
                    if (tierRemoved.isNotEmpty()) {
                        Log.d(TAG, "$pfx TierFilter: removing ${tierRemoved.map { "${it.typeName()}(tier=${it.tier()})" }} keeping tier=$highestTier")
                    }
                    triggers.removeAll { it !is CoachingTrigger.ConversionStrategy && it.tier() != highestTier }
                }

                // Single-voice enforcer: after tier filtering, keep exactly ONE trigger.
                // Uses effectiveSubPriority — triggers matching the player's historical
                // weak areas are promoted (lower effective priority = shown first).
                val singleBest = triggers.filter { it !is CoachingTrigger.ConversionStrategy }
                    .minByOrNull { effectiveSubPriority(it, weakTriggerTypes) }
                if (singleBest != null) {
                    val voiceRemoved = triggers.filter { it !is CoachingTrigger.ConversionStrategy && it != singleBest }
                    if (voiceRemoved.isNotEmpty()) {
                        val promotedNote = if (singleBest.typeName() in weakTriggerTypes) " [WEAK-AREA-BOOST]" else ""
                        Log.d(TAG, "$pfx SingleVoice: removing ${voiceRemoved.map { it.typeName() }} keeping=${singleBest.typeName()}$promotedNote")
                    }
                    triggers.removeAll { it !is CoachingTrigger.ConversionStrategy && it != singleBest }
                }

                if (triggers.isNotEmpty()) {
                    result[eval.moveIndex] = triggers
                    // Update calibration frequency cap only when the calibration trigger actually
                    // survives all filtering (tier + single-voice) so suppressed probes don't
                    // consume a calibration slot.
                    if (triggers.any { it is CoachingTrigger.EvalCalibration }) {
                        lastCalibrationAt = eval.moveIndex
                    }
                }
            }
            Log.d(TAG, "$pfx RESULT pre-filter=$preFilterTypes final=${triggers.map { it.typeName() }}")
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
        pfx: String = "",
    ): CoachingTrigger.Safety? {
        val movingSide   = if (isWhite) Side.WHITE else Side.BLACK
        val opponentSide = if (isWhite) Side.BLACK else Side.WHITE
        val kingSquare   = BoardAttackHelper.kingSquare(board, movingSide) ?: run {
            Log.d(TAG, "$pfx Safety: SUPPRESS no king found for side=$movingSide")
            return null
        }

        val adjacent      = adjacentSquares(kingSquare)
        val defenderCount = adjacent.count { sq ->
            val p = board.getPiece(sq)
            p != Piece.NONE && p.pieceSide == movingSide && p.pieceType != PieceType.KING
        }

        if (defenderCount > KING_MIN_ADJACENT_DEFENDERS) {
            Log.d(TAG, "$pfx Safety: SUPPRESS defenderCount=$defenderCount > min=$KING_MIN_ADJACENT_DEFENDERS king=$kingSquare")
            return null
        }

        val attacksBySquare: Map<Square, Int> = adjacent.associateWith { sq ->
            BoardAttackHelper.attackersOf(board, sq, opponentSide).size
        }
        val mostAttackedEntry = attacksBySquare.maxByOrNull { (_, cnt) -> cnt }
        val hasDirectAttack   = (mostAttackedEntry?.value ?: 0) > 0

        val moverDelta        = if (isWhite) eval.evalDelta else -eval.evalDelta
        val isSignificantDrop = moverDelta <= -SAFETY_MIN_CP_DROP

        if (!isSignificantDrop && !hasDirectAttack) {
            Log.d(TAG, "$pfx Safety: SUPPRESS no significant drop (moverDelta=$moverDelta need<=-$SAFETY_MIN_CP_DROP) and no direct attack")
            return null
        }

        if (hasDirectAttack && !isSignificantDrop && prevBoard != null) {
            val prevKingSquare   = BoardAttackHelper.kingSquare(prevBoard, movingSide) ?: kingSquare
            val prevAdjacent     = adjacentSquares(prevKingSquare)
            val prevMaxAttackers = prevAdjacent.maxOfOrNull { sq ->
                BoardAttackHelper.attackersOf(prevBoard, sq, opponentSide).size
            } ?: 0
            val currentMaxAttackers = mostAttackedEntry?.value ?: 0
            if (currentMaxAttackers <= prevMaxAttackers) {
                Log.d(TAG, "$pfx Safety: SUPPRESS pre-existing attack (currentMaxAttackers=$currentMaxAttackers <= prev=$prevMaxAttackers) — not caused by this move")
                return null
            }
        }

        val threatSquare = if (hasDirectAttack) mostAttackedEntry?.key?.name else null
        return CoachingTrigger.Safety(eval.moveIndex, kingSquare.name, threatSquare)
    }

    private fun detectCandidate(eval: GameEvaluation, isWhite: Boolean, pfx: String = ""): CoachingTrigger.CandidateMoves? {
        val evalFromMover = if (isWhite) eval.evalCp else -eval.evalCp
        val absEval = kotlin.math.abs(evalFromMover)
        return if (eval.motif == "mixed" && absEval <= CANDIDATE_EVAL_THRESHOLD_CP) {
            CoachingTrigger.CandidateMoves(eval.moveIndex, eval.evalCp)
        } else {
            Log.d(TAG, "$pfx CandidateMoves: SUPPRESS motif=${eval.motif}(need mixed) evalFromMover=$evalFromMover absEval=$absEval threshold=$CANDIDATE_EVAL_THRESHOLD_CP")
            null
        }
    }

    private fun detectWorstPiece(
        board: Board,
        moveIndex: Int,
        isWhite: Boolean,
        streakTracker: MutableMap<String, Int>,
        pfx: String = "",
    ): CoachingTrigger.WorstPiece? {
        val side         = if (isWhite) Side.WHITE else Side.BLACK
        val opponentSide = if (isWhite) Side.BLACK else Side.WHITE

        val candidatePieceTypes = setOf(
            PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN,
        )
        val restricted = BoardAttackHelper.piecesOf(board, side)
            .filter { (_, piece) -> piece.pieceType in candidatePieceTypes }
            .map    { (sq, _)   -> sq to BoardAttackHelper.attacksFrom(board, sq).size }
            .filter { (_, mob)  -> mob <= WORST_PIECE_MAX_MOBILITY }

        if (restricted.isEmpty()) {
            Log.d(TAG, "$pfx WorstPiece: SUPPRESS no piece has mobility <= $WORST_PIECE_MAX_MOBILITY")
            streakTracker.clear()
            return null
        }

        val (worstSq, mobility) = restricted.minByOrNull { (_, mob) -> mob } ?: return null

        // If the restricted piece is under attack it is a tactical emergency, not a strategic
        // positional issue. Suppress here so PreMoveChecklist can handle it instead.
        val isUnderAttack = BoardAttackHelper.attackersOf(board, worstSq, opponentSide).isNotEmpty()
        if (isUnderAttack) {
            Log.d(TAG, "$pfx WorstPiece: SUPPRESS sq=$worstSq is under attack — deferring to PreMoveChecklist")
            streakTracker.clear()
            return null
        }

        val key = worstSq.name
        val prevStreak = streakTracker[key] ?: 0
        streakTracker.clear()
        val newStreak = prevStreak + 1
        streakTracker[key] = newStreak

        return if (newStreak >= WORST_PIECE_STREAK_NEEDED) {
            CoachingTrigger.WorstPiece(moveIndex, key, mobility)
        } else {
            Log.d(TAG, "$pfx WorstPiece: SUPPRESS streak=$newStreak/$WORST_PIECE_STREAK_NEEDED for sq=$key mobility=$mobility")
            null
        }
    }

    private fun detectForcingMove(
        eval: GameEvaluation,
        isWhite: Boolean,
        playerIsWhite: Boolean,
        moverLoss: Int,
        pfx: String = "",
    ): CoachingTrigger.ForcingMove? {
        // "mixed" encompasses compound tactical patterns such as check+capture (e.g. Qe5+ winning
        // a piece), which the motif classifier cannot resolve to a single label. Allow it through
        // here; the moverLoss and playerAdvantage gates below prevent false positives.
        val isForcingMotif = eval.motif in listOf("fork", "hanging", "checkmate", "mixed")
        if (!isForcingMotif) {
            Log.d(TAG, "$pfx ForcingMove: SUPPRESS motif=${eval.motif} (need fork/hanging/checkmate/mixed)")
            return null
        }
        val isPlayerToMove = isWhite != playerIsWhite
        if (!isPlayerToMove) {
            Log.d(TAG, "$pfx ForcingMove: SUPPRESS it's the player's own move (isWhite=$isWhite playerIsWhite=$playerIsWhite)")
            return null
        }
        if (moverLoss < FORCING_MOVE_MIN_CP_LOSS) {
            Log.d(TAG, "$pfx ForcingMove: SUPPRESS moverLoss=$moverLoss < threshold=$FORCING_MOVE_MIN_CP_LOSS motif=${eval.motif}")
            return null
        }
        val playerEvalAdvantage = if (playerIsWhite) eval.evalCp else -eval.evalCp
        if (playerEvalAdvantage < FORCING_MOVE_MIN_PLAYER_ADVANTAGE_CP) {
            Log.d(TAG, "$pfx ForcingMove: SUPPRESS playerAdvantage=$playerEvalAdvantage < $FORCING_MOVE_MIN_PLAYER_ADVANTAGE_CP (opponent made a bad exchange but position is not yet tactically decisive)")
            return null
        }
        return CoachingTrigger.ForcingMove(eval.moveIndex, eval.motif)
    }

    private fun detectOpponentPlan(
        eval: GameEvaluation,
        prevEval: GameEvaluation,
        isWhite: Boolean,
        pfx: String = "",
    ): CoachingTrigger.OpponentPlan? {
        val moverGain = if (isWhite) eval.evalDelta else -eval.evalDelta
        return if (moverGain in OPPONENT_PLAN_MIN_CP..OPPONENT_PLAN_MAX_CP) {
            CoachingTrigger.OpponentPlan(eval.moveIndex, moverGain)
        } else {
            Log.d(TAG, "$pfx OpponentPlan: SUPPRESS moverGain=$moverGain not in [$OPPONENT_PLAN_MIN_CP..$OPPONENT_PLAN_MAX_CP]")
            null
        }
    }

    private fun detectPreMoveChecklist(
        board: Board,
        eval: GameEvaluation,
        moverLoss: Int,
        pfx: String = "",
    ): CoachingTrigger.PreMoveChecklist? {
        if (moverLoss < PRE_MOVE_CHECKLIST_MIN_CP_LOSS) {
            Log.d(TAG, "$pfx PreMoveChecklist: SUPPRESS moverLoss=$moverLoss < threshold=$PRE_MOVE_CHECKLIST_MIN_CP_LOSS")
            return null
        }
        if (moverLoss >= PRE_MOVE_CHECKLIST_MAX_CP_LOSS) {
            Log.d(TAG, "$pfx PreMoveChecklist: SUPPRESS terminal blunder moverLoss=$moverLoss >= max=$PRE_MOVE_CHECKLIST_MAX_CP_LOSS — hanging piece is the blunder itself, not a pre-move oversight")
            return null
        }

        val hangingSquare = BoardAttackHelper.allPieces(board)
            .filter { (_, piece) -> piece.pieceType != PieceType.KING }
            .firstOrNull { (sq, piece) -> isGenuinelyHanging(board, sq, piece) }
            ?.first

        return if (hangingSquare != null) {
            CoachingTrigger.PreMoveChecklist(eval.moveIndex, hangingSquare.name)
        } else {
            Log.d(TAG, "$pfx PreMoveChecklist: SUPPRESS no genuinely hanging piece found (moverLoss=$moverLoss was enough)")
            null
        }
    }

    private fun isGenuinelyHanging(board: Board, sq: Square, piece: Piece) =
        BoardAttackHelper.isGenuinelyHanging(board, sq, piece)

    private fun detectRookActivation(board: Board, moveIndex: Int, isWhite: Boolean, pfx: String = ""): CoachingTrigger.RookActivation? {
        val side      = if (isWhite) Side.WHITE else Side.BLACK
        val rookPiece = if (side == Side.WHITE) com.github.bhlangonijr.chesslib.Piece.WHITE_ROOK
                        else                    com.github.bhlangonijr.chesslib.Piece.BLACK_ROOK

        val rooks = BoardAttackHelper.piecesOf(board, side)
            .filter { (_, piece) -> piece == rookPiece }

        if (rooks.isEmpty()) {
            Log.d(TAG, "$pfx RookActivation: SUPPRESS no rooks found for side=$side")
            return null
        }

        for ((rookSq, _) in rooks) {
            val rookFile = BoardAttackHelper.fileOf(rookSq)
            val rookRank = BoardAttackHelper.rankOf(rookSq)

            val homeRank = if (side == Side.WHITE) 0 else 7
            val isOnStartSquare = rookRank == homeRank && (rookFile == 0 || rookFile == 7)
            val developedMinors = countDevelopedMinors(board, side)
            if (isOnStartSquare && developedMinors < ROOK_ACTIVATION_MIN_DEVELOPED_MINORS) {
                Log.d(TAG, "$pfx RookActivation: SUPPRESS rook=$rookSq on start square, developedMinors=$developedMinors < $ROOK_ACTIVATION_MIN_DEVELOPED_MINORS")
                continue
            }

            val rookFileHasPawn = (0..7).any { rank ->
                val sq = BoardAttackHelper.squareAt(rookFile, rank) ?: return@any false
                val p  = board.getPiece(sq)
                p != Piece.NONE && p.pieceType == PieceType.PAWN
            }
            if (!rookFileHasPawn) {
                Log.d(TAG, "$pfx RookActivation: SUPPRESS rook=$rookSq already on open file=$rookFile")
                continue
            }

            val mobility = BoardAttackHelper.attacksFrom(board, rookSq).size
            if (mobility >= 6) {
                Log.d(TAG, "$pfx RookActivation: SUPPRESS rook=$rookSq mobility=$mobility >= 6 (not stuck)")
                continue
            }

            val betterFile = (0..7).firstOrNull { file ->
                if (file == rookFile) return@firstOrNull false
                val hasFriendlyPawn = (0..7).any { rank ->
                    val sq = BoardAttackHelper.squareAt(file, rank) ?: return@any false
                    val p  = board.getPiece(sq)
                    p != Piece.NONE && p.pieceType == PieceType.PAWN && p.pieceSide == side
                }
                !hasFriendlyPawn
            }

            if (betterFile != null) {
                return CoachingTrigger.RookActivation(moveIndex, rookSq.name, betterFile)
            } else {
                Log.d(TAG, "$pfx RookActivation: SUPPRESS rook=$rookSq no better (half-)open file found")
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
        pfx: String = "",
    ): CoachingTrigger.ImpulseControl? {
        val timeSpent = timeByMoveIndex(eval.moveIndex) ?: run {
            Log.d(TAG, "$pfx ImpulseControl: SUPPRESS no clock data for move=${eval.moveIndex}")
            return null
        }
        if (timeSpent >= IMPULSE_TIME_THRESHOLD_SECONDS) {
            Log.d(TAG, "$pfx ImpulseControl: SUPPRESS timeSpent=${timeSpent}s >= threshold=${IMPULSE_TIME_THRESHOLD_SECONDS}s")
            return null
        }
        val moverEvalDelta = if (isWhite) eval.evalDelta else -eval.evalDelta
        return if (moverEvalDelta <= -IMPULSE_CP_LOSS_THRESHOLD) {
            CoachingTrigger.ImpulseControl(eval.moveIndex, timeSpent, kotlin.math.abs(moverEvalDelta))
        } else {
            Log.d(TAG, "$pfx ImpulseControl: SUPPRESS fast move (${timeSpent}s) but moverEvalDelta=$moverEvalDelta not <= -$IMPULSE_CP_LOSS_THRESHOLD")
            null
        }
    }

    private fun detectCalculationBlunder(
        eval: GameEvaluation,
        isWhite: Boolean,
        timeByMoveIndex: (Int) -> Int?,
        pfx: String = "",
    ): CoachingTrigger.CalculationBlunder? {
        val timeSpent = timeByMoveIndex(eval.moveIndex) ?: run {
            Log.d(TAG, "$pfx CalculationBlunder: SUPPRESS no clock data for move=${eval.moveIndex}")
            return null
        }
        if (timeSpent < CALCULATION_BLUNDER_TIME_THRESHOLD_SECONDS) {
            Log.d(TAG, "$pfx CalculationBlunder: SUPPRESS timeSpent=${timeSpent}s < threshold=${CALCULATION_BLUNDER_TIME_THRESHOLD_SECONDS}s")
            return null
        }
        val moverEvalDelta = if (isWhite) eval.evalDelta else -eval.evalDelta
        return if (moverEvalDelta <= -IMPULSE_CP_LOSS_THRESHOLD) {
            CoachingTrigger.CalculationBlunder(eval.moveIndex, timeSpent, kotlin.math.abs(moverEvalDelta))
        } else {
            Log.d(TAG, "$pfx CalculationBlunder: SUPPRESS slow move (${timeSpent}s) but moverEvalDelta=$moverEvalDelta not <= -$IMPULSE_CP_LOSS_THRESHOLD")
            null
        }
    }

    private fun detectTacticalOversight(
        eval: GameEvaluation,
        isWhite: Boolean,
        timeByMoveIndex: (Int) -> Int?,
        pfx: String = "",
    ): CoachingTrigger.TacticalOversight? {
        val timeSpent = timeByMoveIndex(eval.moveIndex) ?: run {
            Log.d(TAG, "$pfx TacticalOversight: SUPPRESS no clock data for move=${eval.moveIndex}")
            return null
        }
        if (timeSpent < TACTICAL_OVERSIGHT_MIN_SECONDS || timeSpent >= TACTICAL_OVERSIGHT_MAX_SECONDS) {
            Log.d(TAG, "$pfx TacticalOversight: SUPPRESS timeSpent=${timeSpent}s not in [${TACTICAL_OVERSIGHT_MIN_SECONDS}..${TACTICAL_OVERSIGHT_MAX_SECONDS})")
            return null
        }
        val moverEvalDelta = if (isWhite) eval.evalDelta else -eval.evalDelta
        return if (moverEvalDelta <= -IMPULSE_CP_LOSS_THRESHOLD) {
            CoachingTrigger.TacticalOversight(eval.moveIndex, timeSpent, kotlin.math.abs(moverEvalDelta))
        } else {
            Log.d(TAG, "$pfx TacticalOversight: SUPPRESS moverEvalDelta=$moverEvalDelta not <= -$IMPULSE_CP_LOSS_THRESHOLD")
            null
        }
    }

    private fun detectCandidateSearch(
        eval: GameEvaluation,
        isWhite: Boolean,
        playerIsWhite: Boolean,
        moverLoss: Int,
        pfx: String = "",
    ): CoachingTrigger.CandidateSearch? {
        val evalFromMover  = if (isWhite) eval.evalCp else -eval.evalCp
        val absMover       = kotlin.math.abs(evalFromMover)
        val evalFromPlayer = if (playerIsWhite) eval.evalCp else -eval.evalCp

        // Tactical Presence gate: if one move was clearly head-and-shoulders above the rest
        // (large moverLoss = the played move was obviously wrong), this is a calculation problem,
        // not a "search for the best plan" moment. Suppress so ForcingMove / PreMoveChecklist can speak.
        if (moverLoss >= CANDIDATE_SEARCH_CLARITY_CP) {
            Log.d(TAG, "$pfx CandidateSearch: SUPPRESS moverLoss=$moverLoss >= $CANDIDATE_SEARCH_CLARITY_CP (clear best move exists)")
            return null
        }

        return if (eval.motif == "mixed" && absMover in CANDIDATE_SEARCH_MIN_CP..CANDIDATE_SEARCH_MAX_CP) {
            CoachingTrigger.CandidateSearch(eval.moveIndex, evalFromPlayer)
        } else {
            Log.d(TAG, "$pfx CandidateSearch: SUPPRESS motif=${eval.motif}(need mixed) absMoverEval=$absMover range=[$CANDIDATE_SEARCH_MIN_CP..$CANDIDATE_SEARCH_MAX_CP]")
            null
        }
    }

    private fun detectCctCheck(
        eval: GameEvaluation,
        isWhite: Boolean,
        playerIsWhite: Boolean,
        moverLoss: Int,
        pfx: String = "",
    ): CoachingTrigger.CctCheck? {
        // Only relevant when the opponent moved — coaching the player to anticipate CCT replies.
        if (isWhite == playerIsWhite) {
            Log.d(TAG, "$pfx CctCheck: SUPPRESS player's own move")
            return null
        }
        // moverLoss > 0 means the opponent (mover) lost eval: they blundered, no CCT was played.
        val opponentGain = -moverLoss
        return if (opponentGain > CCT_CHECK_EVAL_SHIFT_CP) {
            CoachingTrigger.CctCheck(eval.moveIndex, eval.evalDelta)
        } else {
            Log.d(TAG, "$pfx CctCheck: SUPPRESS opponentGain=$opponentGain <= threshold=$CCT_CHECK_EVAL_SHIFT_CP (opponent did not gain, moverLoss=$moverLoss)")
            null
        }
    }

    private fun detectPunishBlunder(
        eval: GameEvaluation,
        moverLoss: Int,
        pfx: String = "",
    ): CoachingTrigger.PunishBlunder? {
        return if (moverLoss >= PUNISH_BLUNDER_MIN_CP_LOSS) {
            CoachingTrigger.PunishBlunder(eval.moveIndex, moverLoss)
        } else {
            Log.d(TAG, "$pfx PunishBlunder: SUPPRESS moverLoss=$moverLoss < threshold=$PUNISH_BLUNDER_MIN_CP_LOSS")
            null
        }
    }

    // ── Game Narrative Context helpers ────────────────────────────────────────

    /**
     * Constructs a [GameNarrativeContext] for the current position using rolling windows
     * and the per-position board geometry.
     */
    private fun buildNarrativeContext(
        evalCp: Int,
        moverLoss: Int,
        motif: String,
        recentDeltas: List<Int>,
        recentCps: List<Int>,
        pressure: Float,
        playerIsWhite: Boolean,
        isWhiteMove: Boolean,
        weakTriggerTypes: Set<String>,
    ): GameNarrativeContext {
        // Volatility: rolling average of |evalDelta| over the window
        val volatility = if (recentDeltas.isEmpty()) 0f
                         else recentDeltas.map { kotlin.math.abs(it) }.average().toFloat()

        // Decisiveness slope: rate of eval change from the player's perspective
        val slopeWhite = if (recentCps.size >= 2)
            (recentCps.last() - recentCps.first()).toFloat() / (recentCps.size - 1)
        else 0f
        val playerEvalSlope = if (playerIsWhite) slopeWhite else -slopeWhite

        // Complexity gap: heuristic from moverLoss + motif
        val complexityGap = when {
            motif != "mixed"                    -> maxOf(moverLoss, FORCED_MOVE_GAP_CP)  // tactical motif → forced
            moverLoss >= CANDIDATE_SEARCH_CLARITY_CP -> moverLoss                        // large blunder → forced
            else                                -> minOf(moverLoss, STRATEGIC_GAP_MAX_CP) // calm position
        }

        val playerEvalCp = if (playerIsWhite) evalCp else -evalCp

        return GameNarrativeContext(
            volatility       = volatility,
            pressure         = pressure,
            playerEvalSlope  = playerEvalSlope,
            complexityGap    = complexityGap,
            playerEvalCp     = playerEvalCp,
            weakTriggerTypes = weakTriggerTypes,
        )
    }

    /**
     * Computes the average number of opponent attackers per piece on the board.
     * High values indicate a congested, high-tension position.
     */
    private fun computePressureScore(board: Board): Float {
        val pieces = BoardAttackHelper.allPieces(board)
        if (pieces.isEmpty()) return 0f
        val totalAttackers = pieces.sumOf { (sq, piece) ->
            val attackerSide = if (piece.pieceSide == Side.WHITE) Side.BLACK else Side.WHITE
            BoardAttackHelper.attackersOf(board, sq, attackerSide).size
        }
        return totalAttackers.toFloat() / pieces.size
    }

    /**
     * Sub-priority adjusted for the player's weak areas.
     * Triggers matching a historically missed pattern are promoted within their tier.
     */
    private fun effectiveSubPriority(trigger: CoachingTrigger, weakTypes: Set<String>): Int =
        if (trigger.typeName() in weakTypes) trigger.subPriority() - 5 else trigger.subPriority()

    /**
     * Calibration trigger: quizzes the user's positional assessment at stable moments.
     *
     * Fires when:
     *  - Position is past the opening gate and not lopsided
     *  - Recent evaluation is not volatile (no tactical storm)
     *  - At least [CALIBRATION_FREQUENCY_CAP] half-moves have passed since the last calibration
     *  - A recognised context exists: POST_OPENING window or a significant evaluation jump
     *
     * The returned trigger carries the hidden engine eval; it is only revealed after the user
     * locks in their own assessment in the UI.
     */
    private fun detectCalibration(
        eval: GameEvaluation,
        prevEvals: List<GameEvaluation>,    // up to 3 most-recent preceding evaluations
        lastCalibrationAt: Int,
        pfx: String = "",
    ): CoachingTrigger.EvalCalibration? {
        val moveIndex = eval.moveIndex

        // Opening gate
        if (moveIndex < TIER4_OPENING_GATE) {
            Log.d(TAG, "$pfx Calibration: SUPPRESS opening (moveIndex=$moveIndex < $TIER4_OPENING_GATE)")
            return null
        }

        // Frequency cap
        if (moveIndex - lastCalibrationAt < CALIBRATION_FREQUENCY_CAP) {
            Log.d(TAG, "$pfx Calibration: SUPPRESS frequency cap (lastAt=$lastCalibrationAt gap=${moveIndex - lastCalibrationAt})")
            return null
        }

        // Don't quiz in lopsided positions — the student isn't practicing estimation, they're just reading a number
        if (kotlin.math.abs(eval.evalCp) > CALIBRATION_MAX_EVAL_CP) {
            Log.d(TAG, "$pfx Calibration: SUPPRESS lopsided evalCp=${eval.evalCp}")
            return null
        }

        // Tactical clarity: if the board motif is sharply tactical, it's the wrong moment to pause
        if (eval.motif in listOf("fork", "hanging", "checkmate")) {
            Log.d(TAG, "$pfx Calibration: SUPPRESS tactical motif=${eval.motif}")
            return null
        }

        // Volatility check: average |evalDelta| over the last 3 half-moves must be low
        val avgVolatility = if (prevEvals.isEmpty()) 0
                            else prevEvals.sumOf { kotlin.math.abs(it.evalDelta) } / prevEvals.size
        if (avgVolatility > CALIBRATION_VOLATILITY_MAX_CP) {
            Log.d(TAG, "$pfx Calibration: SUPPRESS high volatility avg=$avgVolatility > $CALIBRATION_VOLATILITY_MAX_CP")
            return null
        }

        // Determine context
        val isPostOpening = moveIndex in CALIBRATION_POST_OPENING_MIN..CALIBRATION_POST_OPENING_MAX
        val prevEvalCp    = prevEvals.firstOrNull()?.evalCp ?: 0
        val wasInEqualZone  = kotlin.math.abs(prevEvalCp) < CALIBRATION_EVAL_JUMP_FROM_CP
        val isNowAdvantage  = kotlin.math.abs(eval.evalCp) >= CALIBRATION_EVAL_JUMP_TO_CP
        val isEvalJump      = wasInEqualZone && isNowAdvantage

        val context = when {
            isEvalJump    -> CalibrationContext.EVAL_JUMP
            isPostOpening -> CalibrationContext.POST_OPENING
            else          -> {
                Log.d(TAG, "$pfx Calibration: SUPPRESS no context (postOpening=$isPostOpening evalJump=$isEvalJump prevEvalCp=$prevEvalCp evalCp=${eval.evalCp})")
                return null
            }
        }

        return CoachingTrigger.EvalCalibration(moveIndex, eval.evalCp, context)
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
