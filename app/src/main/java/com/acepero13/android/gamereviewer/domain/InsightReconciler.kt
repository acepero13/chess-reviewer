package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.CriticalMoment

/**
 * Generates targeted conceptual questions and hints for a given [CriticalMoment.ReasonCategory]
 * or tactical motif.
 *
 * Used by:
 * - **Guided Discovery Panel** (Task 3.3): questions guide the user toward the correct move
 *   without revealing engine lines.
 * - **Blunder Guard reflection** (Task 3.1): after a sandbox blunder the user must reflect
 *   before they can retry.
 *
 * All returned text is deliberately concept-level — never mentions engine moves or raw evaluation.
 */
object InsightReconciler {

    data class Insight(
        val emoji: String,
        val title: String,
        val description: String,
        /** 2-3 guiding questions to surface in the panel. */
        val questions: List<String>,
        /** Conceptual hint shown when the user asks for one (HINTED state).
         *  Must not contain the engine's best move. */
        val conceptualHint: String,
    )

    // ── By reason category (Guided Discovery) ─────────────────────────────────

    fun forReason(category: CriticalMoment.ReasonCategory): Insight = when (category) {

        CriticalMoment.ReasonCategory.MISSED_TACTIC -> Insight(
            emoji = "⚔️",
            title = "Tactical Opportunity",
            description = "The engine found a forcing sequence here that changes the game's course.",
            questions = listOf(
                "Are there any undefended pieces on the board right now?",
                "Can you find a move that simultaneously attacks two of the opponent's pieces?",
                "Is there a forcing sequence (check → capture → threat) that wins material?",
            ),
            conceptualHint = "Apply the LPDO rule — Loose Pieces Drop Off. " +
                "Every check and every capture is a candidate move. " +
                "Look for the most unnatural-looking move; those are often the strongest.",
        )

        CriticalMoment.ReasonCategory.HANGING_PIECE -> Insight(
            emoji = "🎣",
            title = "Undefended Material",
            description = "A piece was left without a defender at this moment.",
            questions = listOf(
                "Count defenders vs. attackers on every piece — who is hanging?",
                "Can you win material directly with a capture sequence?",
                "Is there a counter-threat you need to watch for before grabbing?",
            ),
            conceptualHint = "The counting method: for each piece on the board, " +
                "count how many pieces attack it and how many defend it. " +
                "When attackers outnumber defenders, you can win material.",
        )

        CriticalMoment.ReasonCategory.KING_SAFETY -> Insight(
            emoji = "♔",
            title = "King Safety",
            description = "King safety was the decisive factor at this position.",
            questions = listOf(
                "How well is your king shielded? Are pawn cover squares intact?",
                "Is there an open file or diagonal pointing toward one of the kings?",
                "Can you crack open lines toward the enemy king with a pawn break or sacrifice?",
            ),
            conceptualHint = "King safety problems usually involve open lines — files, diagonals, " +
                "or ranks — leading directly to the king. Look for moves that destroy pawn cover " +
                "or open lines toward the king. Checkmating attacks often start with pawn sacrifices.",
        )

        CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE -> Insight(
            emoji = "🏁",
            title = "Endgame Technique",
            description = "A key endgame principle was the difference in this position.",
            questions = listOf(
                "What is the fundamental rule for this endgame type (K+P, rook, minor piece)?",
                "Should the kings be activated right now, and if so — toward where?",
                "Can you identify the key squares both kings should aim for?",
            ),
            conceptualHint = "In endgames the king becomes a powerful fighting piece — activate it. " +
                "Key principles: create a passed pawn, cut off the opponent's king, use opposition. " +
                "In rook endgames: rook behind the passed pawn, cut off the king on the 7th rank.",
        )

        CriticalMoment.ReasonCategory.OPENING_DEVIATION -> Insight(
            emoji = "📖",
            title = "Opening Deviation",
            description = "You deviated from sound opening principles at this position.",
            questions = listOf(
                "What are the core thematic ideas of your opening setup?",
                "Is your piece development ahead of or behind your opponent's?",
                "Is your king safe and your center controlled?",
            ),
            conceptualHint = "Opening fundamentals: control the center (d4/d5/e4/e5), " +
                "develop minor pieces before queens and rooks, castle early for king safety, " +
                "avoid moving the same piece twice without good reason. " +
                "Where did this game violate these rules?",
        )

        CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE -> Insight(
            emoji = "⚖️",
            title = "Strategic Imbalance",
            description = "A long-term strategic mistake changed the nature of the position.",
            questions = listOf(
                "What are the structural imbalances here — space, pawn weaknesses, piece activity?",
                "Who has the better minor piece (bishop vs. knight), and does the pawn structure support it?",
                "What is your opponent planning, and how can you prevent it (prophylaxis)?",
            ),
            conceptualHint = "Strategic errors usually mean choosing the wrong plan. " +
                "Ask: 'What is my worst-placed piece, and how do I improve it?' " +
                "Also: 'What is my opponent trying to do?' Preventing their plan is " +
                "often stronger than pursuing your own.",
        )

        CriticalMoment.ReasonCategory.TIME_PRESSURE -> Insight(
            emoji = "⏱️",
            title = "Time Pressure Decision",
            description = "This move was played quickly under time pressure.",
            questions = listOf(
                "Looking at this position fresh, what stands out immediately?",
                "What is the simplest, most solid move that doesn't allow immediate tactics against you?",
                "Was there a 'do nothing' move that maintained the position safely?",
            ),
            conceptualHint = "Under time pressure: prioritize safety. " +
                "Ask: 'Does this move lose material? Does it allow checkmate?' " +
                "If neither — it is probably safe enough. Eliminate blunders first, " +
                "then look for the best move among the safe ones.",
        )

        CriticalMoment.ReasonCategory.MISSED_WIN -> Insight(
            emoji = "🏆",
            title = "Winning Opportunity",
            description = "There was a forcing sequence available that wins the game or decisive material.",
            questions = listOf(
                "Look at every check — is any of them forcing the opponent into a losing position?",
                "Is there a combination that wins a piece or more for less in return?",
                "Can you visualize a 2-4 move forcing sequence that changes the evaluation dramatically?",
            ),
            conceptualHint = "Winning combinations often start with the most forcing moves available: " +
                "checks, captures, threats. Use the STOP acronym: " +
                "Sacrifice, Threat, Overloading, Pin/Skewer. " +
                "Often the best move is the most counterintuitive one — don't dismiss it immediately.",
        )
    }

    // ── By motif (Blunder Guard reflection) ───────────────────────────────────

    /**
     * Returns reflection insight after a sandbox blunder is detected.
     *
     * @param motif  MotifClassifier result: "checkmate", "fork", "hanging", "mixed"
     * @param cpLoss Absolute centipawn loss from the player's perspective.
     */
    fun forBlunder(motif: String, cpLoss: Int): Insight {
        val severityLabel = when {
            cpLoss >= 500 -> "decisive"
            cpLoss >= 300 -> "major"
            else          -> "significant"
        }
        return when (motif) {
            "fork" -> Insight(
                emoji = "⚔️",
                title = "Fork Opportunity for Opponent",
                description = "Your move may give the opponent a fork — attacking two pieces at once.",
                questions = listOf(
                    "After your move, does the opponent have a piece that can attack two of your pieces simultaneously?",
                    "Are your pieces defending each other, or are they 'loose' and scattered?",
                    "Can you rearrange your pieces to eliminate the fork square?",
                ),
                conceptualHint = "Forks succeed because one piece attacks multiple targets at once. " +
                    "Prevent forks by keeping pieces on defended squares and avoiding " +
                    "placing two valuable pieces on squares that share a knight/bishop attack diagonal.",
            )

            "hanging" -> Insight(
                emoji = "🎣",
                title = "Piece Left Hanging",
                description = "Your move may leave one of your pieces undefended and capturable.",
                questions = listOf(
                    "After your move, which of your pieces has zero defenders?",
                    "Can you find an alternative move that achieves the same idea while keeping all pieces protected?",
                    "Is the gain from this move worth the risk of leaving something en prise?",
                ),
                conceptualHint = "Before every move, run a quick check: 'Am I leaving anything undefended?' " +
                    "This habit — LPDO (Loose Pieces Drop Off) — prevents the majority of tactical losses " +
                    "at club level.",
            )

            "checkmate" -> Insight(
                emoji = "♚",
                title = "King Safety Alert",
                description = "Your move may expose your king to a checkmating threat.",
                questions = listOf(
                    "Is your king safe after this move?",
                    "Can the opponent deliver check immediately, and is it dangerous?",
                    "Do you need to address the king's safety before pursuing your plan?",
                ),
                conceptualHint = "King safety must always take priority over material gain. " +
                    "If in any doubt, eliminate all opponent checks before making aggressive moves. " +
                    "A single mating attack often negates a material advantage of several pieces.",
            )

            else -> Insight(
                emoji = "⚖️",
                title = "Evaluation Drop Detected",
                description = "This move causes a $severityLabel evaluation drop (${cpLoss}cp). " +
                    "Take a moment to reconsider.",
                questions = listOf(
                    "What does the position look like after your move — is your opponent immediately better?",
                    "Does your move create a weakness (open file, weak square, loose piece)?",
                    "Is there a calmer alternative that achieves your goal more safely?",
                ),
                conceptualHint = "When an evaluation drops significantly, it often means the move " +
                    "either allows a tactic or creates a long-term weakness. " +
                    "Ask: 'What is my opponent's best reply to this move?' " +
                    "If you cannot answer that, the move may be premature.",
            )
        }
    }
}
