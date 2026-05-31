package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.chess.core.middlegame.MiddlegamePlan
import com.acepero13.chess.core.middlegame.PlanType

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

    // ── By coaching trigger (Proactive Board Scan) ────────────────────────────

    /**
     * Returns a lightweight coaching [Insight] for a proactive [CoachingTrigger].
     *
     * Unlike [forReason] (mistake-based), these insights are never tied to a blunder.
     * They encourage the user to scan the board habitually — the "Board Scan" habit.
     */
    fun forTrigger(trigger: CoachingTrigger): Insight = when (trigger) {

        is CoachingTrigger.Safety -> Insight(
            emoji       = "♔",
            title       = "King Safety Check",
            description = "Your King has few pieces guarding the surrounding squares. " +
                "Strong players check King safety after every move — theirs and the opponent's.",
            questions   = listOf(
                "You just moved a piece away from your King. Can you see any immediate threats to your King's safety?",
                "Are there any open files or diagonals that could be used to attack your King?",
                "Does your opponent have any pieces that could quickly join an attack?",
            ),
            conceptualHint = "King safety is non-negotiable. Count the pawns still shielding your King. " +
                "If your King has fewer than two friendly pieces nearby, consider reinforcing before pursuing other plans.",
        )

        is CoachingTrigger.CandidateMoves -> Insight(
            emoji       = "⚖️",
            title       = "Choose Your Plan",
            description = "The position is balanced — no forcing moves, no clear advantage. " +
                "This is a decision point that tests strategic understanding, not calculation.",
            questions   = listOf(
                "This is a balanced position. Instead of finding the 'best' move, can you identify two different plans here?",
                "Which of your pieces is least active — and how could you improve it?",
                "What is your opponent's most ambitious plan, and how would you stop it?",
            ),
            conceptualHint = "In balanced positions, ask Silman's question: 'What are the imbalances?' " +
                "Look at pawn structure, piece activity, open files, and key squares. " +
                "The player with the clearer plan usually wins these positions.",
        )

        is CoachingTrigger.WorstPiece -> Insight(
            emoji       = "♟",
            title       = "Worst Piece Scan",
            description = "One of your pieces has very limited mobility — it is your 'worst piece.' " +
                "Improving your worst piece is almost always the right strategic plan.",
            questions   = listOf(
                "Your pieces are fighting for space. Can you point to your most underdeveloped or restricted piece?",
                "Why is that piece restricted — is it blocked by its own pawns or controlled by the opponent?",
                "What one move would improve that piece's scope the most?",
            ),
            conceptualHint = "Jeremy Silman's key rule: always improve your worst piece first. " +
                "Ask 'Which of my pieces would be embarrassed to show its face at a chess club?' " +
                "Then find a plan to reroute or free it.",
        )

        is CoachingTrigger.ForcingMove -> Insight(
            emoji       = "⚔️",
            title       = "Forcing Moves First",
            description = "The engine found a forcing sequence here. Strong players always scan " +
                "for checks, captures, and threats before choosing a quiet move.",
            questions   = listOf(
                "Always check for forcing moves first. Are there any checks, captures, or threats in this position?",
                "Can you find a move that wins material by force — not by hope?",
                "Is there a sequence that starts with a check or capture and leads to a clear advantage?",
            ),
            conceptualHint = "Use the CCT rule before every move: Check, Capture, Threat. " +
                "If any exist, they are your candidate moves. Only consider quiet moves after verifying " +
                "there are no forcing sequences available.",
        )

        is CoachingTrigger.OpponentPlan -> Insight(
            emoji       = "🔭",
            title       = "Opponent's Intent",
            description = "That move improved the opponent's position. Before responding, " +
                "top players always pause to understand the opponent's idea.",
            questions   = listOf(
                "Stop. What is the strategic idea behind that last move?",
                "Did the opponent create a threat, improve a piece, or open a line — which was the priority?",
                "How does your response address their plan rather than just pursuing your own?",
            ),
            conceptualHint = "After every opponent move, ask: 'Why did they play that?' " +
                "Prophylaxis — anticipating and preventing the opponent's threats — is a hallmark " +
                "of advanced play. A move that stops their plan AND improves your position is ideal.",
        )

        is CoachingTrigger.PreMoveChecklist -> Insight(
            emoji       = "✅",
            title       = "Pre-Move Checklist",
            description = "There is at least one undefended piece on the board. " +
                "Strong players run a quick safety check before every single move.",
            questions   = listOf(
                "Before you move: are there any loose pieces on the board — yours or the opponent's?",
                "Does your opponent have an immediate threat you must address first?",
                "After your planned move, will any of your pieces become undefended?",
            ),
            conceptualHint = "Use the LPDO rule (Loose Pieces Drop Off) before every move. " +
                "Scan all pieces for attackers vs. defenders. If attackers > defenders, " +
                "that piece is hanging. This single habit eliminates the majority of tactical losses.",
        )

        is CoachingTrigger.RookActivation -> Insight(
            emoji       = "♜",
            title       = "Activate Your Rook",
            description = "Your rook is stuck on a closed file while better options exist. " +
                "A rook on a closed file contributes almost nothing to the position.",
            questions   = listOf(
                "Your rook is on a closed file. Can you find an open or half-open file where it would be far more powerful?",
                "Which file is most likely to open up in the next few moves — and can you place your rook there now?",
                "Is there a way to double rooks on an open file, or place a rook behind a passed pawn?",
            ),
            conceptualHint = "Rooks belong on open files, behind passed pawns, or on the 7th rank. " +
                "The rule: 'Put your rook where the action is — or where the action will be.' " +
                "Even moving a rook one file to a half-open file can dramatically improve its activity.",
        )

        is CoachingTrigger.ImpulseControl -> Insight(
            emoji       = "⚡",
            title       = "Impulse Control Check",
            description = "You played that move in under ${trigger.timeSpentSeconds} seconds. " +
                "In complex positions, intuition must be verified before being trusted.",
            questions   = listOf(
                "What was the first candidate move you saw, and why did you stop looking after that?",
                "Did you consider at least two different plans before deciding on this move?",
                "What is your opponent's best reply to this move — did you check it before playing?",
            ),
            conceptualHint = "Fast moves in complex positions usually indicate 'hand chess' — " +
                "the hand moves before the mind has finished calculating. " +
                "The rule: if a position demands calculation, spend at least 30 seconds. " +
                "Ask yourself: 'Am I playing the first move I saw, or the best move I found?'",
        )

        is CoachingTrigger.CandidateSearch -> Insight(
            emoji       = "🔍",
            title       = "Candidate Search",
            description = "This position is rich with possibilities — multiple plans exist with " +
                "very different tactical and strategic consequences.",
            questions   = listOf(
                "Can you find two different plans — one more aggressive, one more solid — and name them?",
                "Why is Plan A better than Plan B in this specific pawn structure and piece placement?",
                "What is your opponent's position trying to do — and which of your plans prevents it best?",
            ),
            conceptualHint = "In complex positions, the question is not 'What is the best move?' " +
                "but 'Which plan is best?' Compare concrete variations: " +
                "Plan A may win a pawn but open your king; Plan B may be slower but safer. " +
                "Articulating why one plan beats another is the core skill of positional chess.",
        )

        is CoachingTrigger.CctCheck -> Insight(
            emoji       = "✔️",
            title       = "CCT Self-Check",
            description = "This move caused a significant position change. " +
                "Before seeing the engine's verdict, scan what your opponent can play.",
            questions   = listOf(
                "After your move, can your opponent play any immediate Checks — are they all safe?",
                "After your move, can your opponent make any profitable Captures?",
                "After your move, does your opponent have a strong Threat you have not addressed?",
            ),
            conceptualHint = "The CCT rule (Checks, Captures, Threats) is the most important habit " +
                "in chess. After every move — yours or the opponent's — scan for the three forcing " +
                "move types. Most tactical losses occur not because a player cannot calculate, " +
                "but because they forgot to look.",
        )

        is CoachingTrigger.ConversionStrategy -> {
            val advantagePawns = "%.1f".format(kotlin.math.abs(trigger.evaluationCp) / 100.0)
            Insight(
                emoji       = "♛",
                title       = "Convert the Advantage",
                description = "You are significantly ahead in material (+${advantagePawns} pawns). " +
                    "In winning positions, the priority shifts from finding the best plan to " +
                    "avoiding the mistakes that let opponents back into the game.",
                questions   = listOf(
                    "You are significantly ahead in material — what is the simplest path to victory from here?",
                    "Can you identify any forced trades that would reduce complexity and bring the win closer?",
                    "Are there any unnecessary risks in your position that your opponent could exploit?",
                ),
                conceptualHint = "In winning positions, Silman's rule applies: simplify. " +
                    "Swap off pieces to reduce the opponent's counterplay, keep your King safe, " +
                    "and avoid speculative sacrifices. The goal is not the most brilliant win — " +
                    "it is the most reliable win. Ask: 'Does this move make my advantage harder to convert?'",
            )
        }

        is CoachingTrigger.CoordinatedAttack -> when {
            trigger.isPlayerSide && !trigger.isLoss -> Insight(
                emoji       = "🗡️",
                title       = "Attack is Coming Together",
                description = "${trigger.pieceCount} of your pieces are now targeting the opponent's king zone. " +
                    "Coordinated attacks are decisive when all pieces contribute to the same focal point.",
                questions   = listOf(
                    "Which of your attacking pieces is the most dangerous right now — and is it safe from counterattack?",
                    "Can your opponent disrupt the coordination with a counter-threat or defensive exchange?",
                    "What square does your attack converge on, and can you add one more piece to it?",
                ),
                conceptualHint = "Coordinated attacks win when all pieces aim at the same weakness. " +
                    "The principle: every piece must have a role. Ask 'What is this piece contributing to the attack?' " +
                    "for each of your pieces. Idle pieces that can join the attack should do so immediately.",
            )
            trigger.isPlayerSide && trigger.isLoss -> Insight(
                emoji       = "💨",
                title       = "Attack Has Dissolved",
                description = "Your coordinated attack on the king has broken down. " +
                    "Attacks that lose momentum often leave the attacker overextended.",
                questions   = listOf(
                    "Which piece drifted away from the attack — was it traded off, or did you redirect it?",
                    "Can you reroute a piece back into the attack in one or two moves?",
                    "Did your opponent defend accurately, or did you lose the coordination thread first?",
                ),
                conceptualHint = "When attacks dissolve, the danger is that your pieces become scattered. " +
                    "Regroup before pushing forward: bring all pieces back into harmony first. " +
                    "A second attack wave, if properly coordinated, is often stronger than the first.",
            )
            !trigger.isPlayerSide && !trigger.isLoss -> Insight(
                emoji       = "🛡️",
                title       = "Opponent Building Coordinated Attack",
                description = "${trigger.pieceCount} of your opponent's pieces are now targeting your king zone. " +
                    "Recognising a coordinated attack early is the key to defending successfully.",
                questions   = listOf(
                    "Which of your opponent's pieces is the most dangerous attacker right now?",
                    "Can you trade off their most active attacking piece without weakening your own position?",
                    "Is your king safe enough to ignore this and pursue counterplay, or must you defend first?",
                ),
                conceptualHint = "Against a coordinated attack, the principle is: eliminate the most dangerous attacker. " +
                    "This is often the piece closest to your king. " +
                    "If you cannot trade it off, create a counter-threat — your opponent must then choose between attack and defence.",
            )
            else -> Insight(
                emoji       = "✨",
                title       = "Opponent's Attack Has Broken Down",
                description = "The coordinated attack your opponent was building has fallen apart. " +
                    "Disorganised attacking pieces often become targets themselves.",
                questions   = listOf(
                    "What caused the coordination to fall apart — a piece trade, a defensive move, or a mistake?",
                    "Can you immediately counterattack against the now-scattered pieces?",
                    "Is this the moment to open the position and exploit the disorganization?",
                ),
                conceptualHint = "When an opponent's attack dissolves, their pieces are often misplaced. " +
                    "The counter-principle: transition to counterplay. Open lines with a pawn break, " +
                    "activate your own pieces, and target the pieces that are no longer coordinating.",
            )
        }

        is CoachingTrigger.PieceHarmony -> when {
            trigger.isPlayerSide && !trigger.isLoss -> Insight(
                emoji       = "🎶",
                title       = "Pieces Working in Harmony",
                description = "Your pieces are sharing targets and supporting each other across the board. " +
                    "Maintaining this coordination is as important as building it.",
                questions   = listOf(
                    "Which piece just joined the coordination — what plan does it now contribute to?",
                    "Are all of your pieces pointed at the same strategic goal, or is any piece 'off-key'?",
                    "Can you maintain this harmony for the next few moves, or will your opponent force a disruption?",
                ),
                conceptualHint = "Piece harmony means every piece has a clear role in the same plan. " +
                    "The test: can you name what each piece is doing? " +
                    "If a piece has no role in the current plan, it should be rerouted immediately.",
            )
            trigger.isPlayerSide && trigger.isLoss -> Insight(
                emoji       = "🔀",
                title       = "Pieces Have Lost Coordination",
                description = "Your pieces are no longer working together — they have drifted onto different plans. " +
                    "Disorganised pieces are vulnerable to tactical counterplay.",
                questions   = listOf(
                    "Which piece is now misplaced — can you identify the one that no longer fits the position?",
                    "Can you re-route it in one or two moves to rejoin the coordination?",
                    "Is your opponent able to exploit the current disorganisation with a tactical shot?",
                ),
                conceptualHint = "Lost coordination is often the prelude to tactical problems. " +
                    "The remedy: find your worst-placed piece and improve it first. " +
                    "Nimzowitsch's principle: if you cannot find a plan, improve your worst piece.",
            )
            !trigger.isPlayerSide && !trigger.isLoss -> Insight(
                emoji       = "👁️",
                title       = "Opponent's Pieces Are Well Coordinated",
                description = "Your opponent's pieces are sharing targets and supporting each other. " +
                    "Coordinated pieces are difficult to fight piecemeal — you must address the whole system.",
                questions   = listOf(
                    "What plan do all of your opponent's coordinated pieces support together?",
                    "Which piece is the key to their coordination — the 'conductor' of the plan?",
                    "Can you disrupt the coordination with a pawn break, an exchange sacrifice, or a counter-threat?",
                ),
                conceptualHint = "Coordinated pieces are stronger than the sum of their parts. " +
                    "To fight coordination, disrupt it: force your opponent to react to your threats " +
                    "rather than pursuing their plan. A well-timed pawn break or sacrifice can shatter coordination instantly.",
            )
            else -> Insight(
                emoji       = "🔓",
                title       = "Opponent's Coordination Has Broken Down",
                description = "Your opponent's pieces have lost their harmony and are no longer working together. " +
                    "This is the moment to act decisively before they regroup.",
                questions   = listOf(
                    "Which of your opponent's pieces is now most out of play?",
                    "Can you open the position now to exploit the lack of coordination?",
                    "Is this a good time to create a new weakness — a passed pawn, an outpost, or an open file?",
                ),
                conceptualHint = "Exploit disorganized pieces immediately — they will regroup if given time. " +
                    "Create concrete threats that force your opponent to react on your terms. " +
                    "Open lines, activate your own pieces, and target the misplaced pieces before they find their best squares.",
            )
        }
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

    /**
     * Returns an [Insight] for a specific endgame type detected in the game, enriching
     * the generic [ENDGAME_PRINCIPLE] insight with the exact chapter and endgame name
     * from *100 Endgames You Should Know*.
     */
    fun forEndgame(chapter: Int, name: String): Insight {
        val base = forReason(CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE)
        return base.copy(
            conceptualHint = "Chapter $chapter — $name\n\n${base.conceptualHint}",
        )
    }

    // ── By middlegame pawn structure ──────────────────────────────────────────

    fun forMiddlegamePlan(plan: MiddlegamePlan): Insight = when (plan.type) {

        PlanType.IQP -> Insight(
            emoji       = "♙",
            title       = "Isolated Queen's Pawn",
            description = "You have an IQP on d4/d5 — a pawn with no friendly neighbours on the c or e files. It creates dynamic counterplay but is a long-term structural weakness.",
            questions   = listOf(
                "Who controls e5 and c5? Can you place a knight on one of those outposts?",
                "Should you trade pieces to reduce activity, or keep pieces on the board to maximise your attacking chances?",
                "Is your opponent targeting the d-pawn directly with rooks or pieces?",
            ),
            conceptualHint = "The IQP creates dynamic counterplay — piece activity compensates for the structural weakness. " +
                "Nimzowitsch's rule: blockade the IQP with a knight on d5. The IQP owner should avoid mass exchanges.",
        )

        PlanType.HANGING_PAWNS -> Insight(
            emoji       = "⚠️",
            title       = "Hanging Pawns",
            description = "You have connected pawns on c and d with no friendly support on the adjacent b and e files — the 'hanging pawn' duo. Dynamic but potentially vulnerable.",
            questions   = listOf(
                "Are your c- and d-pawns acting as a strength (attacking wedge) or weakness (both targeted)?",
                "Can you advance one pawn to create a passed pawn before your opponent organises a blockade?",
                "Are your pieces coordinated to support a pawn advance?",
            ),
            conceptualHint = "Hanging pawns are dynamic — advance to create a passed pawn when your pieces are active. " +
                "If under pressure, exchange one pawn to reach a stable IQP structure rather than letting both become targets.",
        )

        PlanType.PAWN_MAJORITY -> Insight(
            emoji       = "⚖️",
            title       = plan.title,
            description = "You have more pawns than your opponent on one wing. A healthy majority creates a passed pawn in the endgame.",
            questions   = listOf(
                "Is your majority mobile, or are your pawns blocked by enemy pieces or pawns?",
                "Can you exchange center pawns to activate the majority and create a passer?",
                "Is your opponent organising a minority attack on the opposite wing to create weaknesses?",
            ),
            conceptualHint = "A healthy pawn majority converts to a passed pawn in the endgame. " +
                "Avoid unnecessary pawn advances that self-block the majority. The plan: liquidate the center, then push.",
        )

        PlanType.OPEN_FILE -> Insight(
            emoji       = "♜",
            title       = plan.title,
            description = "The ${plan.affectedFile}-file has no pawns — it is fully open. Heavy pieces belong here.",
            questions   = listOf(
                "Which rook benefits most from the ${plan.affectedFile}-file — the one closer to the action?",
                "Can you double your rooks on this file to double the pressure?",
                "Does the open file lead toward the opponent's king position or a weak back-rank square?",
            ),
            conceptualHint = "Rooks belong on open files. Place a rook first, then double — two rooks on an open file exert decisive pressure. " +
                "Look for an outpost square or the 7th rank at the end of the open file.",
        )

        PlanType.HALF_OPEN_FILE -> Insight(
            emoji       = "↕",
            title       = plan.title,
            description = "The ${plan.affectedFile}-file has an enemy pawn but no friendly pawn — a half-open file. Use it to pressure the enemy.",
            questions   = listOf(
                "What is the rook's target on this file — the enemy pawn, the seventh rank, or the king?",
                "Can a pawn break open the file further and convert it to a fully open file?",
                "Is the enemy pawn on this file defended? If not, it's a direct target.",
            ),
            conceptualHint = "A rook on a half-open file pressures the enemy pawn and prevents it from advancing. " +
                "Long-term goal: win or trade the blocking pawn to convert the half-open file to a fully open one.",
        )

        PlanType.DOUBLED_PAWNS -> Insight(
            emoji       = "⬆",
            title       = plan.title,
            description = "You have two pawns on the ${plan.affectedFile}-file. Doubled pawns reduce mobility but often open adjacent files.",
            questions   = listOf(
                "Which open or half-open file did your doubled pawns create — can you use it?",
                "Are the doubled pawns a static long-term weakness or a dynamic short-term asset?",
                "Can you use the open file aggressively enough to justify the structural cost?",
            ),
            conceptualHint = "Doubled pawns are not always a weakness — they control squares and open adjacent files. " +
                "Key question: do your pieces compensate for the reduced pawn mobility? Use the open file aggressively.",
        )

        PlanType.BACKWARD_PAWN -> Insight(
            emoji       = "🎯",
            title       = plan.title,
            description = "The ${plan.affectedFile}-pawn cannot advance safely and is behind its neighbours — a backward pawn. It is a long-term structural target.",
            questions   = listOf(
                "Can your opponent place a piece on the square directly in front of the backward pawn as a permanent outpost?",
                "Can you advance or trade off the backward pawn before it becomes permanently fixed?",
                "Is the square in front of the backward pawn a strong outpost for the opponent's knight?",
            ),
            conceptualHint = "A backward pawn on an open file is a classic long-term weakness. " +
                "Target it with rooks, prevent its advance, and use the square in front as a knight outpost. " +
                "The defender must keep it active with piece play to avoid slow suffocation.",
        )

        PlanType.PAWN_CHAIN -> Insight(
            emoji       = "🔗",
            title       = "Pawn Chain",
            description = "You have a diagonal chain of connected pawns. The chain controls space but has a vulnerable base.",
            questions   = listOf(
                "Where is the base of the chain — which pawn, if captured, unravels the entire structure?",
                "How can you attack the enemy's chain base while reinforcing the head of your own?",
                "Is your opponent's counter-play targeting the same chain or a different part of the board?",
            ),
            conceptualHint = "Pawn chain strategy (Nimzowitsch): attack the BASE, not the head. " +
                "The base pawn supports the entire chain — if it falls, the chain collapses. " +
                "The head pawn is the most advanced but is protected by the chain below it.",
        )
    }
}
