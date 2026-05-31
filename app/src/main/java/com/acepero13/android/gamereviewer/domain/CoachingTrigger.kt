package com.acepero13.android.gamereviewer.domain

import com.github.bhlangonijr.chesslib.Side

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
    ) : CoachingTrigger(moveIndex)

    /** Multiple non-pawn pieces share overlapping target squares — general piece coordination changed. */
    class PieceHarmony(
        moveIndex: Int,
        val isPlayerSide: Boolean,
        val isLoss: Boolean,
        val score: Int,
    ) : CoachingTrigger(moveIndex)

    // ── Identity ───────────────────────────────────────────────────────────────

    fun typeName(): String = when (this) {
        is Safety              -> "SAFETY"
        is CandidateMoves      -> "CANDIDATE_MOVES"
        is WorstPiece          -> "WORST_PIECE"
        is ForcingMove         -> "FORCING_MOVE"
        is OpponentPlan        -> "OPPONENT_PLAN"
        is PreMoveChecklist    -> "PRE_MOVE_CHECKLIST"
        is RookActivation      -> "ROOK_ACTIVATION"
        is ImpulseControl      -> "IMPULSE_CONTROL"
        is CandidateSearch     -> "CANDIDATE_SEARCH"
        is CctCheck            -> "CCT_CHECK"
        is ConversionStrategy  -> "CONVERSION_STRATEGY"
        is CoordinatedAttack   -> "COORDINATED_ATTACK"
        is PieceHarmony        -> "PIECE_HARMONY"
    }

    fun emoji(): String = when (this) {
        is Safety              -> "♔"
        is CandidateMoves      -> "⚖️"
        is WorstPiece          -> "♟"
        is ForcingMove         -> "⚔️"
        is OpponentPlan        -> "🔭"
        is PreMoveChecklist    -> "✅"
        is RookActivation      -> "♜"
        is ImpulseControl      -> "⚡"
        is CandidateSearch     -> "🔍"
        is CctCheck            -> "✔️"
        is ConversionStrategy  -> "♛"
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
    }

    fun title(): String = when (this) {
        is Safety              -> "King Safety Check"
        is CandidateMoves      -> "Choose Your Plan"
        is WorstPiece          -> "Worst Piece Scan"
        is ForcingMove         -> "Forcing Moves First"
        is OpponentPlan        -> "Opponent's Intent"
        is PreMoveChecklist    -> "Pre-Move Checklist"
        is RookActivation      -> "Activate Your Rook"
        is ImpulseControl      -> "Impulse Control Check"
        is CandidateSearch     -> "Candidate Search"
        is CctCheck            -> "CCT Self-Check"
        is ConversionStrategy  -> "Convert the Advantage"
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
    }

    fun coachingQuestion(): String = when (this) {
        is Safety           ->
            if (threatSquare != null)
                "The $threatSquare square is a focal point for your opponent's pieces. Is your King under immediate tactical danger there?"
            else
                "Your King has very few defenders nearby. Are there any forcing lines that could exploit that exposure right now?"
        is CandidateMoves   ->
            "This is a balanced position. Instead of finding the 'best' move, can you identify two different plans here?"
        is WorstPiece       ->
            "Your pieces are fighting for space. Can you point to your most underdeveloped or restricted piece?"
        is ForcingMove      ->
            "Always check for forcing moves first. Are there any checks, captures, or threats in this position?"
        is OpponentPlan     ->
            "Stop. What is the strategic idea behind that last move?"
        is PreMoveChecklist ->
            "Before you move: are there any loose pieces on the board? Does your opponent have an immediate threat?"
        is RookActivation   ->
            "Your rook is on a closed file. Can you find an open or half-open file where it would be far more powerful?"
        is ImpulseControl   ->
            "You played that move in under ${timeSpentSeconds}s. What was the first candidate move you saw, and why did you stop looking after that?"
        is CandidateSearch  ->
            "The position is rich with possibilities. Can you find 2 different plans here? Don't decide until you can say why Plan A is better than Plan B."
        is CctCheck         ->
            "Before we see what the engine thinks: are there any Checks, Captures, or Threats your opponent can play on their turn?"
        is ConversionStrategy ->
            "You are significantly ahead in material. What is the simplest path to victory from here — and how do you avoid unnecessary complications?"
        is CoordinatedAttack -> when {
            isPlayerSide && !isLoss  ->
                "Your pieces are converging on the opponent's king — which piece is the most dangerous attacker right now?"
            isPlayerSide && isLoss   ->
                "Your attack has dissolved. Which piece drifted away from the attack, and can you bring it back?"
            !isPlayerSide && !isLoss ->
                "Your opponent's pieces are coordinating toward your king. Which of their pieces is the most dangerous attacker?"
            else                     ->
                "Your opponent's attack has broken down. Can you identify why, and how can you exploit the disorganization?"
        }
        is PieceHarmony -> when {
            isPlayerSide && !isLoss  ->
                "Your pieces are working in harmony. Can you name the plan they are all contributing to?"
            isPlayerSide && isLoss   ->
                "Your pieces have lost their coordination. Which piece is now misplaced, and can you re-route it in one move?"
            !isPlayerSide && !isLoss ->
                "Your opponent's pieces are well coordinated. What plan do they support, and how can you disrupt it?"
            else                     ->
                "Your opponent's piece coordination has broken down. Can you open the position now to exploit the disorganization?"
        }
    }

    // ── Display label used in Reflection Mode selection lists ─────────────────

    fun displayLabel(): String = when (this) {
        is Safety              -> "Safety Issue"
        is CandidateMoves      -> "Multiple Plans"
        is WorstPiece          -> "Restricted Piece"
        is ForcingMove         -> "Forcing Move"
        is OpponentPlan        -> "Opponent's Plan"
        is PreMoveChecklist    -> "Pre-Move Check"
        is RookActivation      -> "Rook Activation"
        is ImpulseControl      -> "Impulse Move"
        is CandidateSearch     -> "Depth Search"
        is CctCheck            -> "CCT Check"
        is ConversionStrategy  -> "Conversion Strategy"
        is CoordinatedAttack   -> "Coordinated Attack"
        is PieceHarmony        -> "Piece Harmony"
    }

    companion object {
        /** All display labels in canonical order — used by the Reflection Mode quiz. */
        val ALL_LABELS = listOf(
            "Safety Issue", "Multiple Plans", "Restricted Piece",
            "Forcing Move", "Opponent's Plan", "Pre-Move Check", "Rook Activation",
            "Impulse Move", "Depth Search", "CCT Check", "Conversion Strategy",
            "Coordinated Attack", "Piece Harmony",
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
            "CANDIDATE_SEARCH"     -> CandidateSearch(moveIndex, 0)
            "CCT_CHECK"            -> CctCheck(moveIndex, 0)
            "CONVERSION_STRATEGY"  -> ConversionStrategy(moveIndex, 0)
            "COORDINATED_ATTACK"   -> CoordinatedAttack(moveIndex, isPlayerSide = true, isLoss = false, pieceCount = 0)
            "PIECE_HARMONY"        -> PieceHarmony(moveIndex, isPlayerSide = true, isLoss = false, score = 0)
            else                   -> null
        }
    }
}
