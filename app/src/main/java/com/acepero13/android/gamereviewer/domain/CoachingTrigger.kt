package com.acepero13.android.gamereviewer.domain

import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square

/**
 * Represents a proactive coaching moment detected in a game position.
 *
 * Unlike [com.acepero13.android.gamereviewer.data.model.CriticalMoment] which only flags
 * mistakes, coaching triggers fire on ANY teachable structural pattern — balanced positions,
 * restricted pieces, forcing opportunities, etc. — regardless of whether the user blundered.
 *
 * Each subtype carries enough data to render a contextual question in [InsightReconciler.forTrigger].
 */
sealed class CoachingTrigger(val moveIndex: Int) {

    /** King has ≤ 1 friendly adjacent pieces AND a significant eval drop or direct attacker — tactical exposure. */
    class Safety(moveIndex: Int, val kingSquare: String, val threatSquare: String? = null) : CoachingTrigger(moveIndex)

    /** Position is balanced (±30cp, no tactical motif) — multiple plans available. */
    class CandidateMoves(moveIndex: Int, val evalCp: Int) : CoachingTrigger(moveIndex)

    /** A friendly non-pawn piece has ≤ 2 legal moves for 3+ consecutive positions. */
    class WorstPiece(moveIndex: Int, val pieceSquare: String, val mobility: Int) : CoachingTrigger(moveIndex)

    /** Engine's best move is a forcing move (fork / hanging piece / checkmate threat). */
    class ForcingMove(moveIndex: Int, val motif: String) : CoachingTrigger(moveIndex)

    /** A move improved the mover's position by 30–100cp — strategic intent worth noting. */
    class OpponentPlan(moveIndex: Int, val evalGain: Int) : CoachingTrigger(moveIndex)

    /** Any piece on the board has more attackers than defenders — pre-move checklist habit. */
    class PreMoveChecklist(moveIndex: Int, val hangingSquare: String?) : CoachingTrigger(moveIndex)

    /** A rook is trapped on a closed file while an open/half-open file is available nearby. */
    class RookActivation(moveIndex: Int, val rookSquare: String, val openFileIndex: Int) : CoachingTrigger(moveIndex)

    /** Move played in < 5 seconds AND evaluation dropped > 200 cp — impulsive, unverified move. */
    class ImpulseControl(moveIndex: Int, val timeSpentSeconds: Int, val cpLoss: Int) : CoachingTrigger(moveIndex)

    /** Move played after > 30 seconds of thinking AND evaluation dropped > 200 cp — visualization or calculation breakdown. */
    class CalculationBlunder(moveIndex: Int, val timeSpentSeconds: Int, val cpLoss: Int) : CoachingTrigger(moveIndex)

    /** Move played in 5–30 seconds AND evaluation dropped > 200 cp — had time to spot the tactic but missed it. */
    class TacticalOversight(moveIndex: Int, val timeSpentSeconds: Int, val cpLoss: Int) : CoachingTrigger(moveIndex)

    /** Position is complex with multiple plans of different tactical character — compare, don't guess. */
    class CandidateSearch(moveIndex: Int, val evalCp: Int) : CoachingTrigger(moveIndex)

    /** Any move that shifted the evaluation > 100 cp — CCT habit reinforcement before the engine reveal. */
    class CctCheck(moveIndex: Int, val evalDelta: Int) : CoachingTrigger(moveIndex)

    /** Player holds a significant material advantage (≥ +4.0 pawns) — focus on converting the win. */
    class ConversionStrategy(moveIndex: Int, val evaluationCp: Int) : CoachingTrigger(moveIndex)

    /** 3+ non-pawn pieces of one side can legally move into the opponent's king zone simultaneously. */
    class CoordinatedAttack(
        moveIndex: Int,
        val isPlayerSide: Boolean,
        val isLoss: Boolean,
        val pieceCount: Int,
        val attackerSquares: List<Square> = emptyList(),
        val targetSquare: Square? = null,
    ) : CoachingTrigger(moveIndex)

    /** Multiple non-pawn pieces share overlapping target squares — general piece coordination changed. */
    class PieceHarmony(
        moveIndex: Int,
        val isPlayerSide: Boolean,
        val isLoss: Boolean,
        val score: Int,
        val attackerSquares: List<Square> = emptyList(),
        val targetSquares: List<Square> = emptyList(),
    ) : CoachingTrigger(moveIndex)

    /** Opponent blundered ≥ 150 cp — opportunity to capitalize on their mistake. */
    class PunishBlunder(moveIndex: Int, val opponentLoss: Int) : CoachingTrigger(moveIndex)

    // ── Identity ───────────────────────────────────────────────────────────────

    fun typeName(): String = when (this) {
        is Safety               -> "SAFETY"
        is CandidateMoves       -> "CANDIDATE_MOVES"
        is WorstPiece           -> "WORST_PIECE"
        is ForcingMove          -> "FORCING_MOVE"
        is OpponentPlan         -> "OPPONENT_PLAN"
        is PreMoveChecklist     -> "PRE_MOVE_CHECKLIST"
        is RookActivation       -> "ROOK_ACTIVATION"
        is ImpulseControl       -> "IMPULSE_CONTROL"
        is CalculationBlunder   -> "CALCULATION_BLUNDER"
        is TacticalOversight    -> "TACTICAL_OVERSIGHT"
        is CandidateSearch      -> "CANDIDATE_SEARCH"
        is CctCheck             -> "CCT_CHECK"
        is ConversionStrategy   -> "CONVERSION_STRATEGY"
        is CoordinatedAttack    -> "COORDINATED_ATTACK"
        is PieceHarmony         -> "PIECE_HARMONY"
        is PunishBlunder        -> "PUNISH_BLUNDER"
    }

    fun emoji(): String = when (this) {
        is Safety               -> "♔"
        is CandidateMoves       -> "⚖️"
        is WorstPiece           -> "♟"
        is ForcingMove          -> "⚔️"
        is OpponentPlan         -> "🔭"
        is PreMoveChecklist     -> "✅"
        is RookActivation       -> "♜"
        is ImpulseControl       -> "⚡"
        is CalculationBlunder   -> "🧮"
        is TacticalOversight    -> "👁"
        is CandidateSearch      -> "🔍"
        is CctCheck             -> "✔️"
        is ConversionStrategy   -> "♛"
        is CoordinatedAttack   -> when {
            isPlayerSide && !isLoss  -> "🗡️"
            isPlayerSide && isLoss   -> "💨"
            !isPlayerSide && !isLoss -> "🛡️"
            else                     -> "✨"
        }
        is PieceHarmony        -> when {
            isPlayerSide && !isLoss  -> "🎶"
            isPlayerSide && isLoss   -> "🔀"
            !isPlayerSide && !isLoss -> "👁️"
            else                     -> "🔓"
        }
        is PunishBlunder       -> "🎯"
    }

    fun title(): String = when (this) {
        is Safety               -> "King Safety Check"
        is CandidateMoves       -> "Choose Your Plan"
        is WorstPiece           -> "Worst Piece Scan"
        is ForcingMove          -> "Forcing Moves First"
        is OpponentPlan         -> "Opponent's Intent"
        is PreMoveChecklist     -> "Pre-Move Checklist"
        is RookActivation       -> "Activate Your Rook"
        is ImpulseControl       -> "Impulse Control Check"
        is CalculationBlunder   -> "Calculation Breakdown"
        is TacticalOversight    -> "Tactical Oversight"
        is CandidateSearch      -> "Candidate Search"
        is CctCheck             -> "CCT Self-Check"
        is ConversionStrategy   -> "Convert the Advantage"
        is CoordinatedAttack   -> when {
            isPlayerSide && !isLoss  -> "Attack Coming Together"
            isPlayerSide && isLoss   -> "Attack Has Dissolved"
            !isPlayerSide && !isLoss -> "Opponent's Attack Building"
            else                     -> "Opponent's Attack Broken"
        }
        is PieceHarmony        -> when {
            isPlayerSide && !isLoss  -> "Pieces in Harmony"
            isPlayerSide && isLoss   -> "Coordination Lost"
            !isPlayerSide && !isLoss -> "Opponent Well Coordinated"
            else                     -> "Opponent Lost Coordination"
        }
        is PunishBlunder       -> "Capitalize on the Mistake"
    }

    fun coachingQuestion(): String = when (this) {
        is Safety           ->
            if (threatSquare != null)
                "That $threatSquare square is getting a lot of attention from your opponent's pieces. Does your king feel safe there right now?"
            else
                "Your king doesn't have many friends nearby at the moment. Take a look — is anything pointing at it?"
        is CandidateMoves   ->
            "The position is up for grabs right now. Before picking a move, can you sketch two different plans — even if one feels a bit risky?"
        is WorstPiece       ->
            "That piece on $pieceSquare hasn't had much room to breathe lately. If you could give it just one move to improve its life, what would it be?"
        is ForcingMove      ->
            "Before settling on a quiet move, take one more look — is there anything forcing available here? A check, a capture, a concrete threat?"
        is OpponentPlan     ->
            "Interesting move by your opponent. What do you think they were trying to set up with that?"
        is PreMoveChecklist ->
            if (hangingSquare != null)
                "Before you commit — take one more look at $hangingSquare. Does anything there feel like it's asking to be taken?"
            else
                "Before you commit — scan the board one more time. Does anything feel like it's asking to be taken?"
        is RookActivation   ->
            "That rook on $rookSquare is sitting on a closed file right now — it's quiet, but quiet isn't helping. If you could move it anywhere, where would it cause the most trouble?"
        is ImpulseControl   ->
            "That one went in pretty fast — under ${timeSpentSeconds} seconds. What was the first idea you saw, and did you get a chance to check your opponent's best reply?"
        is CalculationBlunder ->
            "You took your time on that one — ${timeSpentSeconds} seconds. Walk me through the line you were calculating. Where did it feel like things went wrong?"
        is TacticalOversight ->
            "That one slipped through with ${timeSpentSeconds} seconds on the clock. What was your reasoning — did you check your opponent's most forcing reply before committing?"
        is CandidateSearch  ->
            "Lots of possibilities here. Can you find two different plans — one sharper, one more solid — and explain why you'd choose one over the other?"
        is CctCheck         ->
            "Before we see what the engine says — on your opponent's next turn, can they play any checks, captures, or strong threats?"
        is ConversionStrategy ->
            "You're clearly ahead here. What's the calmest, most reliable path to the win — and is there anything that could let them back into the game?"
        is CoordinatedAttack -> when {
            isPlayerSide && !isLoss  ->
                "Your pieces are all pointing the same direction — that's a good sign. Which one is doing the most damage right now, and is it safe?"
            isPlayerSide && isLoss   ->
                "The attack has faded a bit. Which piece drifted away from the action, and is there a way to bring it back?"
            !isPlayerSide && !isLoss ->
                "Your opponent's pieces are coordinating toward your king. Which of their pieces is the most dangerous right now?"
            else                     ->
                "Your opponent's attack has lost its steam. Can you see why — and how can you make them pay for it?"
        }
        is PieceHarmony -> when {
            isPlayerSide && !isLoss  ->
                "Your pieces are starting to work together nicely. Can you name the plan they're all contributing to?"
            isPlayerSide && isLoss   ->
                "Your pieces seem to have drifted onto different ideas. Which one is most out of place right now?"
            !isPlayerSide && !isLoss ->
                "Your opponent's pieces are well coordinated. What plan do they support, and is there a way to disrupt it?"
            else                     ->
                "Your opponent's coordination has broken down. Can you open things up and take advantage before they regroup?"
        }
        is PunishBlunder ->
            "Your opponent just played a move that worsens their position. What do you think is the tactical justification for their move — and can you punish their inaccuracy?"
    }

    /**
     * Priority tier for display selection.
     * When multiple triggers fire at the same position, only the lowest (most critical) tier is shown.
     *
     * Tier 1 — Tactical/Safety: backed by a concrete eval loss; the most actionable coaching.
     * Tier 2 — Significant shift: large eval change or opponent strategic gain worth noting.
     * Tier 3 — Coordination: requires eval-backed advantage (gated in [CoachingTriggerEvaluator]).
     * Tier 4 — Positional habits: structural observations, lowest urgency.
     * Tier 0 — ConversionStrategy: always displayed independently; immune to tier filtering.
     */
    fun tier(): Int = when (this) {
        is ImpulseControl                                                        -> 0
        is Safety, is ForcingMove, is PreMoveChecklist, is CalculationBlunder,
        is TacticalOversight, is PunishBlunder                                   -> 1
        is CctCheck, is OpponentPlan                                             -> 2
        is CoordinatedAttack, is PieceHarmony                                   -> 3
        is WorstPiece, is RookActivation, is CandidateMoves, is CandidateSearch -> 4
        is ConversionStrategy                                                    -> 0
    }

    /**
     * Sub-priority within a tier for single-voice selection.
     * Lower number = shown preferentially when multiple triggers survive tier filtering.
     */
    fun subPriority(): Int = when (this) {
        is Safety               -> 1
        is ForcingMove          -> 2
        is PreMoveChecklist     -> 3
        is ImpulseControl       -> 4
        is CalculationBlunder   -> 5
        is TacticalOversight    -> 5
        is CctCheck             -> 6
        is OpponentPlan         -> 7
        is CoordinatedAttack    -> 8
        is PieceHarmony         -> 9
        is WorstPiece           -> 10
        is CandidateMoves       -> 11
        is CandidateSearch      -> 12
        is RookActivation       -> 13
        is ConversionStrategy   -> 14
        is PunishBlunder        -> 2
    }

    // ── Display label used in Reflection Mode selection lists ─────────────────

    fun displayLabel(): String = when (this) {
        is Safety               -> "Safety Issue"
        is CandidateMoves       -> "Multiple Plans"
        is WorstPiece           -> "Restricted Piece"
        is ForcingMove          -> "Forcing Move"
        is OpponentPlan         -> "Opponent's Plan"
        is PreMoveChecklist     -> "Pre-Move Check"
        is RookActivation       -> "Rook Activation"
        is ImpulseControl       -> "Impulse Move"
        is CalculationBlunder   -> "Calculation Error"
        is TacticalOversight    -> "Tactical Oversight"
        is CandidateSearch      -> "Depth Search"
        is CctCheck             -> "CCT Check"
        is ConversionStrategy   -> "Conversion Strategy"
        is CoordinatedAttack    -> "Coordinated Attack"
        is PieceHarmony         -> "Piece Harmony"
        is PunishBlunder        -> "Punish Blunder"
    }

    companion object {
        /** All display labels in canonical order — used by the Reflection Mode quiz. */
        val ALL_LABELS = listOf(
            "Safety Issue", "Multiple Plans", "Restricted Piece",
            "Forcing Move", "Opponent's Plan", "Pre-Move Check", "Rook Activation",
            "Impulse Move", "Calculation Error", "Tactical Oversight", "Depth Search", "CCT Check", "Conversion Strategy",
            "Coordinated Attack", "Piece Harmony", "Punish Blunder",
        )

        /** Reconstruct a minimal trigger stub from a stored type name (no geometry data needed). */
        fun fromTypeName(typeName: String, moveIndex: Int): CoachingTrigger? = when (typeName.trim()) {
            "SAFETY"               -> Safety(moveIndex, "")
            "CANDIDATE_MOVES"      -> CandidateMoves(moveIndex, 0)
            "WORST_PIECE"          -> WorstPiece(moveIndex, "", 0)
            "FORCING_MOVE"         -> ForcingMove(moveIndex, "mixed")
            "OPPONENT_PLAN"        -> OpponentPlan(moveIndex, 0)
            "PRE_MOVE_CHECKLIST"   -> PreMoveChecklist(moveIndex, null)
            "ROOK_ACTIVATION"      -> RookActivation(moveIndex, "", 0)
            "IMPULSE_CONTROL"      -> ImpulseControl(moveIndex, 0, 0)
            "CALCULATION_BLUNDER"  -> CalculationBlunder(moveIndex, 0, 0)
            "TACTICAL_OVERSIGHT"   -> TacticalOversight(moveIndex, 0, 0)
            "CANDIDATE_SEARCH"     -> CandidateSearch(moveIndex, 0)
            "CCT_CHECK"            -> CctCheck(moveIndex, 0)
            "CONVERSION_STRATEGY"  -> ConversionStrategy(moveIndex, 0)
            "COORDINATED_ATTACK"   -> CoordinatedAttack(moveIndex, isPlayerSide = true, isLoss = false, pieceCount = 0)
            "PIECE_HARMONY"        -> PieceHarmony(moveIndex, isPlayerSide = true, isLoss = false, score = 0)
            "PUNISH_BLUNDER"       -> PunishBlunder(moveIndex, 0)
            else                   -> null
        }
    }
}
