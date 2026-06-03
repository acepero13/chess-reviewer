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
            description = "There was a tactical idea available here that could have changed the course of the game. " +
                "It's worth taking the time to find it now.",
            questions = listOf(
                "Take a look at every piece your opponent has left unguarded — is any of them capturable for free, or with a follow-up threat?",
                "Can you find a move that attacks two things at once — a fork, a skewer, or a discovered attack?",
                "Is there a forcing sequence — starting with a check or capture — that wins material or creates a decisive threat?",
            ),
            conceptualHint = "The most important tactical habit is scanning for undefended pieces before every move. " +
                "When you spot a loose piece (one with more attackers than defenders), look for a way to attack it " +
                "while creating a second threat. This idea — Loose Pieces Drop Off — is behind most club-level " +
                "tactical wins.",
        )

        CriticalMoment.ReasonCategory.HANGING_PIECE -> Insight(
            emoji = "🎣",
            title = "Undefended Material",
            description = "A piece was left undefended at this moment — more attackers than defenders. " +
                "That's a simple win available if you see it in time.",
            questions = listOf(
                "Go through each piece on the board and count its attackers and defenders — which one has more attackers?",
                "Is there a way to win that piece directly, or do you need to set up a capture sequence first?",
                "Before grabbing it — does your opponent have a counter-threat you need to watch out for?",
            ),
            conceptualHint = "For every piece on the board, count how many pieces are attacking it and how many " +
                "are defending it. When attackers outnumber defenders, that piece is hanging and can be won. " +
                "Start with captures that are immediately forcing — they give your opponent the least time to respond.",
        )

        CriticalMoment.ReasonCategory.KING_SAFETY -> Insight(
            emoji = "♔",
            title = "King Safety",
            description = "King safety was the key issue in this position. It's easy to miss when you're " +
                "focused on other parts of the board — but a poorly sheltered king invites disaster.",
            questions = listOf(
                "Take a look at the king's pawn cover — how many shield pawns are still in front of it?",
                "Are there open files or diagonals pointing directly toward the king, or on the verge of opening?",
                "Could your opponent crack the position open with a pawn sacrifice or a forcing exchange to expose the king?",
            ),
            conceptualHint = "King safety problems usually involve open lines — files, diagonals, or ranks — " +
                "leading directly to the king. Look for moves that destroy pawn cover or blast open lines. " +
                "Most attacking ideas start not with a brilliant queen sacrifice, but with a quiet pawn move " +
                "that opens a file toward the king.",
        )

        CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE -> Insight(
            emoji = "🏁",
            title = "Endgame Technique",
            description = "The endgame is a different game — and the key principle in this position " +
                "was the difference between winning and drawing.",
            questions = listOf(
                "What type of endgame is this — king and pawn, rook, minor piece? What's the fundamental rule for this type?",
                "Should the kings be activated and marching toward the center? If so, toward which squares?",
                "What are the key squares both kings should be aiming for, and who gets there first?",
            ),
            conceptualHint = "In the endgame, the king becomes a powerful fighting piece — activate it. " +
                "The essential ideas: create a passed pawn, cut off the opponent's king, and use the opposition. " +
                "In rook endgames, put the rook behind the passed pawn and try to cut the opposing king off " +
                "on the 7th rank.",
        )

        CriticalMoment.ReasonCategory.OPENING_DEVIATION -> Insight(
            emoji = "📖",
            title = "Opening Deviation",
            description = "This is the moment where you left the opening principles behind. " +
                "It's worth understanding what the position was asking for at this point.",
            questions = listOf(
                "What are the main ideas your opening setup is trying to achieve — control of the center, piece development, king safety?",
                "At this point in the game, how does your development compare to your opponent's — are you ahead, behind, or equal?",
                "Is your king safe, and is the center stable? What would a textbook response look like here?",
            ),
            conceptualHint = "Opening fundamentals: control the center (d4/d5/e4/e5), develop minor pieces before " +
                "queens and rooks, castle early for king safety, and avoid moving the same piece twice unless " +
                "there's a clear reason. The question isn't just 'what book move was there?' — " +
                "it's 'what principle did this position require?'",
        )

        CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE -> Insight(
            emoji = "⚖️",
            title = "Strategic Imbalance",
            description = "Something went wrong strategically here — a plan or piece placement that changed the " +
                "character of the position in the long run. These are worth understanding because they're " +
                "often invisible in the moment.",
            questions = listOf(
                "What are the structural imbalances in this position — space, pawn weaknesses, piece activity?",
                "Who has the better piece for this specific structure — and does the pawn structure support it?",
                "What was your opponent planning, and was there a way to get in the way of it while improving your own position?",
            ),
            conceptualHint = "Strategic errors usually mean choosing the wrong plan — or missing the opponent's plan entirely. " +
                "Ask: 'What is my worst-placed piece, and how do I improve it?' Then ask: 'What does my opponent want to do next?' " +
                "Sometimes preventing their plan is the whole strategy.",
        )

        CriticalMoment.ReasonCategory.TIME_PRESSURE -> Insight(
            emoji = "⏱️",
            title = "Time Pressure Decision",
            description = "This move was played under time pressure — the clock was a factor here. " +
                "It's worth looking at the position fresh now, without that urgency.",
            questions = listOf(
                "Looking at this position with fresh eyes, what stands out first — any immediate threats or loose pieces?",
                "What's the most solid, trouble-free move here — one that doesn't allow any immediate tactics against you?",
                "Was there a 'do nothing' or consolidating move that would have maintained the position without creating any new risks?",
            ),
            conceptualHint = "Under time pressure, prioritise safety above everything else. Ask: 'Does this move lose material? " +
                "Does it allow a checkmate?' If neither — it's probably safe enough. Eliminate the blunders first, then " +
                "pick the best move from what's left. A safe move played quickly beats a brilliant move played in time trouble.",
        )

        CriticalMoment.ReasonCategory.MISSED_WIN -> Insight(
            emoji = "🏆",
            title = "Winning Opportunity",
            description = "There was a winning idea in this position — a forcing sequence that could have ended " +
                "the game or won decisive material. Let's see if you can find it now.",
            questions = listOf(
                "Look at every check available to you — is any of them followed by a forced win or a massive material gain?",
                "Is there a combination that wins a piece or more with no compensation in return for your opponent?",
                "Can you visualise a 2-4 move sequence — starting with something forcing — that dramatically changes the evaluation?",
            ),
            conceptualHint = "Winning combinations usually start with the most forcing moves: checks, captures, threats. " +
                "A useful lens: look for Sacrifice, Threat, Overloading, Pin/Skewer ideas. Often the winning move " +
                "is the most counterintuitive one — the one that looks strange at first glance. " +
                "If a move makes you stop and wonder, it's worth calculating.",
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
            description = "Your king doesn't have much cover right now. " +
                "That's worth a moment of attention before you look at anything else.",
            questions   = listOf(
                "Take a look at the squares around your king — how many friendly pieces are nearby to defend it?",
                "Are there any open files or diagonals pointing straight at your king right now?",
                "Could your opponent bring a piece to the attack in one or two moves? Which one looks most threatening?",
            ),
            conceptualHint = "King safety is always the first thing to check. If your king has fewer than two " +
                "friendly pieces nearby, that's a warning sign — and any open line pointing at it makes it more urgent. " +
                "Improve the king's shelter before pursuing other plans.",
        )

        is CoachingTrigger.CandidateMoves -> Insight(
            emoji       = "⚖️",
            title       = "Choose Your Plan",
            description = "The position is balanced — no forcing moves, no obvious edge for either side. " +
                "This is the kind of moment where finding the right plan matters more than finding the right move.",
            questions   = listOf(
                "Instead of hunting for the 'best' move, can you sketch two different plans — one more active, one more solid?",
                "Which of your pieces is doing the least right now — and what would give it more purpose?",
                "What is your opponent dreaming of doing in the next few moves, and how would you get in the way?",
            ),
            conceptualHint = "In balanced positions, the player with the clearer plan usually wins. " +
                "Start by asking 'What are the imbalances?' — pawn structure, piece activity, open files, key squares. " +
                "The plan doesn't have to be complicated; it just has to be deliberate.",
        )

        is CoachingTrigger.WorstPiece -> Insight(
            emoji       = "♟",
            title       = "Worst Piece Scan",
            description = "One of your pieces has barely moved in a few turns — it's been stuck with no good squares to go to. " +
                "That piece is holding your position back.",
            questions   = listOf(
                "Take a look at your pieces — which one is the most restricted, with the fewest useful squares to move to?",
                "Why is it stuck? Is it blocked by your own pawns, or is the opponent controlling its best squares?",
                "What single move would do the most to free it up or send it somewhere more useful?",
            ),
            conceptualHint = "The coach's question for any quiet position: 'Which of my pieces would be embarrassed " +
                "to show its face at a chess club?' Find that piece, then build a plan to improve it. " +
                "Often, fixing your worst piece is the whole plan.",
        )

        is CoachingTrigger.ForcingMove -> Insight(
            emoji       = "⚔️",
            title       = "Forcing Moves First",
            description = "There's a forcing idea in this position — something concrete that doesn't give " +
                "your opponent a choice. It's worth finding before you settle on a quieter plan.",
            questions   = listOf(
                "Have a look at every check you can give right now — are any of them followed by a strong threat?",
                "Is there a capture that wins material or forces your opponent into an awkward position?",
                "Can you find a sequence — check, capture, or threat — that puts your opponent on the back foot immediately?",
            ),
            conceptualHint = "Before every move, scan for checks, captures, and threats. If any exist, " +
                "they're your first candidates — quiet moves can wait. This habit (sometimes called a CCT scan) " +
                "catches most of the combinational ideas that get missed under game pressure.",
        )

        is CoachingTrigger.OpponentPlan -> Insight(
            emoji       = "🔭",
            title       = "Opponent's Intent",
            description = "Your opponent just made a move that improved their position. Before you respond, " +
                "it's worth pausing to understand what they're actually planning — otherwise you might " +
                "spend your move solving the wrong problem.",
            questions   = listOf(
                "What was the point of that move? Did it improve a piece, create a threat, open a line — or all three?",
                "Is there something you need to address immediately, or can you continue with your own plan?",
                "What does your opponent want to do on their next move — and how does your response deal with that?",
            ),
            conceptualHint = "After every opponent move, ask: 'Why did they play that?' Great players don't just " +
                "look at their own plans — they try to get inside their opponent's head. A move that stops their " +
                "plan and improves your position is the ideal response.",
        )

        is CoachingTrigger.PreMoveChecklist -> Insight(
            emoji       = "✅",
            title       = "Pre-Move Checklist",
            description = "Something on the board is a bit loose right now. Before you commit to your next move, " +
                "take a second to scan — whose piece is undefended, and can it be taken?",
            questions   = listOf(
                "Go through each piece on the board — which ones have more attackers than defenders right now?",
                "Does your opponent have an immediate threat you need to deal with before anything else?",
                "After the move you're planning, will any of your pieces be left undefended?",
            ),
            conceptualHint = "Before every move, run a quick check: 'Am I leaving anything undefended?' " +
                "Count attackers and defenders on every piece. When attackers outnumber defenders, that piece " +
                "is hanging. This single habit — sometimes called LPDO, Loose Pieces Drop Off — " +
                "eliminates most tactical losses at club level.",
        )

        is CoachingTrigger.RookActivation -> Insight(
            emoji       = "♜",
            title       = "Activate Your Rook",
            description = "That rook on ${trigger.rookSquare} is on a closed file — it's quiet, " +
                "but quiet isn't helpful. Rooks come alive on open files.",
            questions   = listOf(
                "Which file on the board is fully open or half-open right now — and can your rook reach it?",
                "Which file is most likely to open up in the next few moves? Could you position the rook there in advance?",
                "Is there a way to put the rook on the 7th rank, behind a passed pawn, or doubled with the other rook?",
            ),
            conceptualHint = "Think of a rook as a highway driver — it needs a clear road. A closed file is a dead end. " +
                "The question to ask is: 'Which file is about to open, and can I park my rook there first?' " +
                "Even moving one file to a half-open file can transform a passive rook into an active piece.",
        )

        is CoachingTrigger.ImpulseControl -> Insight(
            emoji       = "⚡",
            title       = "Impulse Control Check",
            description = "That move went in under ${trigger.timeSpentSeconds} seconds. Intuition is a great " +
                "starting point in chess — but in a complex position, it needs one more check before you act on it.",
            questions   = listOf(
                "What was the first move you saw — and what made you play it without looking further?",
                "Did you check at least two candidate moves before deciding, or did the first idea feel obvious enough to play immediately?",
                "What's your opponent's best reply to the move you just played — did you verify it before committing?",
            ),
            conceptualHint = "Fast moves in complex positions are often 'hand chess' — the piece moves before " +
                "the mind has finished checking. A useful test: ask yourself 'Am I playing the first move I saw, " +
                "or the best move I found?' If the position requires calculation, try to give it at least 30 seconds " +
                "before committing.",
        )

        is CoachingTrigger.CalculationBlunder -> Insight(
            emoji       = "🧮",
            title       = "Calculation Breakdown",
            description = "You spent ${trigger.timeSpentSeconds} seconds on that move — so this wasn't a time-pressure slip. " +
                "Something in the calculation itself went wrong. That's actually useful information: " +
                "it points to a specific gap worth understanding.",
            questions   = listOf(
                "Walk through the line you were calculating — which move did you think would work out well, and what did you see afterward?",
                "At what point in your calculation did things diverge from what actually happened? Can you pinpoint the exact move?",
                "Did you calculate all the way to a quiet, assessable position — or did you stop and assume it was safe before fully checking it?",
            ),
            conceptualHint = "Long-think blunders aren't about habits — they're about a specific calculation error. " +
                "The most common cause: stopping one move too early and assuming the resulting position is fine " +
                "without checking the opponent's best reply. After any forcing sequence you calculate, always ask: " +
                "'What does my opponent play when this ends — and have I checked that final reply?'",
        )

        is CoachingTrigger.TacticalOversight -> Insight(
            emoji       = "👁",
            title       = "Tactical Oversight",
            description = "You had ${trigger.timeSpentSeconds} seconds on that move — enough time to spot the issue, " +
                "but something slipped past the radar. These are the hardest blunders to accept, " +
                "because the time was there.",
            questions   = listOf(
                "What was your plan when you played that move — did you check your opponent's most forcing reply first?",
                "Which piece or square did you overlook? Looking at it now, was the threat visible before you moved?",
                "Is there a quick scan habit — checks, captures, threats — that might have caught this before committing?",
            ),
            conceptualHint = "Tactical oversights at normal thinking speed are usually caused by tunnel vision — " +
                "you found an idea you liked and stopped searching. The remedy: after finding your candidate move, " +
                "pause and ask 'What does my opponent play next?' just once. That single question catches the vast " +
                "majority of tactical slips that slip through at 10–30 seconds per move.",
        )

        is CoachingTrigger.CandidateSearch -> {
            val evalContext = when {
                trigger.evalCp > 200  -> "You hold a comfortable advantage."
                trigger.evalCp > 50   -> "You are slightly ahead — the position favors you."
                trigger.evalCp >= -50 -> "The position is roughly equal."
                trigger.evalCp >= -200 -> "You are slightly behind — look for the most resilient plan."
                else                  -> "You are under significant pressure."
            }
            Insight(
                emoji       = "🔍",
                title       = "Candidate Search",
                description = "$evalContext There are several different directions you could take this position — " +
                    "each with a different character. The goal isn't to find the objectively best move; " +
                    "it's to understand why one plan is better than the others.",
                questions   = listOf(
                    "Can you find two plans — one more aggressive, one more careful — and describe them in a sentence each?",
                    "Which plan fits better with the current pawn structure and piece placement — and why?",
                    "What is your opponent hoping to do, and which of your plans gets in the way of that most effectively?",
                ),
                conceptualHint = "In rich, complex positions, the key question isn't 'What is the best move?' — " +
                    "it's 'Which plan is best?' Compare the concrete consequences: Plan A may win a pawn but open " +
                    "your king; Plan B may be slower but much harder to refute. Being able to articulate why one " +
                    "plan beats another is one of the most important skills in chess.",
            )
        }

        is CoachingTrigger.CctCheck -> Insight(
            emoji       = "✔️",
            title       = "CCT Self-Check",
            description = "That move shifted the position quite a bit. Before we see what the engine makes of it, " +
                "take a moment to think about what your opponent can do next — you might spot something useful.",
            questions   = listOf(
                "Can your opponent play a check on their next move that puts you in trouble?",
                "Is there a capture available to them that wins material or creates a serious threat?",
                "Does your opponent have a strong threat that you haven't addressed — something they can play without you getting a chance to react?",
            ),
            conceptualHint = "After every move — yours or your opponent's — it's worth scanning for checks, " +
                "captures, and threats. Most tactical losses happen not because a player can't calculate, " +
                "but because they didn't look. This habit is sometimes called the CCT scan: " +
                "Check, Capture, Threat.",
        )

        is CoachingTrigger.ConversionStrategy -> {
            val advantagePawns = "%.1f".format(kotlin.math.abs(trigger.evaluationCp) / 100.0)
            Insight(
                emoji       = "♛",
                title       = "Convert the Advantage",
                description = "You're up +${advantagePawns} pawns — the game is clearly in your favour. " +
                    "The priority now shifts from finding the best move to making sure you don't hand the " +
                    "advantage back. Winning positions have been thrown away with one careless move.",
                questions   = listOf(
                    "What's the simplest, most reliable path to the win from here — not the most brilliant, just the most solid?",
                    "Are there any piece exchanges you can force that would reduce your opponent's counterplay and bring the win closer?",
                    "Is there any risk in your position right now that your opponent could try to exploit — any loose piece, open king, or tactical trick?",
                ),
                conceptualHint = "In winning positions, the hardest skill is not overplaying. Swap off pieces " +
                    "to reduce your opponent's counterplay, keep your king safe, and avoid speculative sacrifices. " +
                    "The goal isn't the most elegant win — it's the most reliable one. Ask yourself: " +
                    "'Does this move make my advantage easier or harder to convert?'",
            )
        }

        is CoachingTrigger.CoordinatedAttack -> when {
            trigger.isPlayerSide && !trigger.isLoss -> Insight(
                emoji       = "🗡️",
                title       = "Attack is Coming Together",
                description = "${trigger.pieceCount} of your pieces are aimed at your opponent's king zone right now. " +
                    "That's the kind of pressure that's hard to defend against — but only if the coordination holds.",
                questions   = listOf(
                    "Which of your attacking pieces is doing the most work right now — and is it safe from being traded off?",
                    "Can your opponent disrupt the coordination with a counter-threat or a defensive exchange — how would you respond?",
                    "Is there one more piece you could bring into the attack to make it decisive?",
                ),
                conceptualHint = "Coordinated attacks work when every piece has a clear role in the same plan. " +
                    "Ask yourself: 'What is each piece contributing to the attack?' Any piece that isn't contributing " +
                    "should either be brought into the attack — or it's a sign the coordination is thinner than it looks.",
            )
            trigger.isPlayerSide && trigger.isLoss -> Insight(
                emoji       = "💨",
                title       = "Attack Has Dissolved",
                description = "The attack you were building has lost its momentum. This happens — but it's worth " +
                    "understanding why, because overextended attacking pieces can become targets themselves.",
                questions   = listOf(
                    "Which piece drifted away from the attack — was it traded off, redirected, or just left without a good square?",
                    "Is there a way to bring a piece back into the attack in one or two moves without losing too much time?",
                    "Did your opponent defend accurately, or did the coordination break down before they had to respond?",
                ),
                conceptualHint = "When an attack dissolves, the danger is that your pieces become scattered and loose. " +
                    "Before pushing forward again, take time to regroup — bring all the pieces back into harmony first. " +
                    "A second wave, properly coordinated, is often stronger than the first.",
            )
            !trigger.isPlayerSide && !trigger.isLoss -> Insight(
                emoji       = "🛡️",
                title       = "Opponent Building Coordinated Attack",
                description = "${trigger.pieceCount} of your opponent's pieces are pointing at your king's side right now. " +
                    "Recognising this early is the key — defences that come too late rarely hold.",
                questions   = listOf(
                    "Which of your opponent's pieces is the most dangerous right now — the one closest to breaking through?",
                    "Is there a way to trade off their most active attacker without weakening your own position in the process?",
                    "Can your king run to safety, or is it better to create a counter-threat that forces your opponent to pause?",
                ),
                conceptualHint = "Against a coordinated attack, the best approach is to find and remove the most " +
                    "dangerous piece — often the one closest to your king. If you can't trade it off cleanly, " +
                    "look for a counter-threat: forcing your opponent to react gives you breathing room.",
            )
            else -> Insight(
                emoji       = "✨",
                title       = "Opponent's Attack Has Broken Down",
                description = "Your opponent's attack has run out of steam. Their pieces are no longer working together — " +
                    "which means they may be poorly placed and vulnerable if you act quickly.",
                questions   = listOf(
                    "What caused the attack to fall apart — a piece trade, a well-timed defensive move, or did it just lose momentum?",
                    "Can you counterattack immediately against any of the now-scattered pieces?",
                    "Is this the moment to open the position and take advantage of the disorganisation?",
                ),
                conceptualHint = "When an opponent's attack dissolves, their pieces are often misplaced and not " +
                    "supporting each other. This is the moment to transition to counterplay: open lines with a pawn " +
                    "break, activate your own pieces, and target the pieces that have lost their coordination " +
                    "before they can find good squares again.",
            )
        }

        is CoachingTrigger.PieceHarmony -> when {
            trigger.isPlayerSide && !trigger.isLoss -> Insight(
                emoji       = "🎶",
                title       = "Pieces Working in Harmony",
                description = "Your pieces are working together right now — sharing targets, supporting each other. " +
                    "That's not easy to achieve, and it's worth protecting.",
                questions   = listOf(
                    "Which piece just joined the coordination — what does it add to the plan?",
                    "Is every piece pointing at the same strategic goal, or is one of them slightly 'off-key'?",
                    "Can this harmony be maintained for the next few moves, or will your opponent be able to force a disruption?",
                ),
                conceptualHint = "Piece harmony means every piece has a clear role in the same plan. " +
                    "The test: can you name what each piece is doing right now? If one piece has no clear role, " +
                    "it should be rerouted. Coordination that can't be articulated is usually more fragile than it looks.",
            )
            trigger.isPlayerSide && trigger.isLoss -> Insight(
                emoji       = "🔀",
                title       = "Pieces Have Lost Coordination",
                description = "Your pieces have drifted a bit — they're no longer pulling in the same direction. " +
                    "That's a vulnerable state, because disorganised pieces can become targets.",
                questions   = listOf(
                    "Which piece is now most out of place — the one that no longer fits the current position?",
                    "Can you reroute it in one or two moves to bring it back into the plan?",
                    "Does your opponent have a chance to exploit the current disorganisation with a tactical shot before you regroup?",
                ),
                conceptualHint = "Lost coordination is often the warning sign that comes before a tactical problem. " +
                    "The remedy: find your worst-placed piece and improve it first. As Nimzowitsch said: " +
                    "if you can't find a plan, improve your worst piece — that is the plan.",
            )
            !trigger.isPlayerSide && !trigger.isLoss -> Insight(
                emoji       = "👁️",
                title       = "Opponent's Pieces Are Coordinating",
                description = "Your opponent's pieces are working well together right now — pointing at the same squares, " +
                    "reinforcing each other. That kind of coordination is a real strategic weapon.",
                questions   = listOf(
                    "Which squares or pieces are all of your opponent's active pieces aiming at right now?",
                    "Is there an immediate forcing threat — a capture, check, or fork — hidden in that coordination?",
                    "What's the piece at the heart of their coordination — the one that holds it all together? Can you challenge it?",
                ),
                conceptualHint = "Coordinated pieces are more dangerous than their individual values suggest. " +
                    "Look for the key piece holding the coordination together — challenging or trading it off " +
                    "can disrupt the entire plan. A well-timed counter-threat is often more effective than passive defence.",
            )
            else -> Insight(
                emoji       = "🔓",
                title       = "Opponent's Coordination Has Broken Down",
                description = "Your opponent's pieces have lost their harmony — they're no longer supporting each other. " +
                    "This is the moment to act before they find their footing again.",
                questions   = listOf(
                    "Which of your opponent's pieces is most out of play right now — furthest from where it needs to be?",
                    "Can you open the position immediately to exploit the lack of coordination?",
                    "Is this a good time to create a new weakness — a passed pawn, an outpost, or an open file — while they're busy regrouping?",
                ),
                conceptualHint = "Disorganised pieces will regroup if given time. The principle: create concrete threats " +
                    "that force your opponent to react on your terms before they can consolidate. Open lines, activate " +
                    "your own pieces, and target the misplaced pieces before they find their best squares.",
            )
        }

        is CoachingTrigger.PunishBlunder -> Insight(
            emoji       = "🎯",
            title       = "Capitalize on the Mistake",
            description = "Your opponent just played a move that worsens their position. " +
                "Before we move on, take a moment — there's a way to take advantage of this.",
            questions   = listOf(
                "What is the tactical justification for their move — why do you think they played it, and what did they overlook?",
                "Take your time: can you punish their inaccuracy? Look for checks, captures, and threats before deciding.",
            ),
            conceptualHint = "When your opponent makes a mistake, the key habit is recognising it immediately and responding with the most forcing move available. " +
                "Ask yourself: 'What did they leave behind with that move?' — an undefended piece, an open line, a tactical weakness. " +
                "The punishment is often straightforward once you stop and look.",
        )

        is CoachingTrigger.EvalCalibration -> Insight(
            emoji       = "🎯",
            title       = trigger.title(),
            description = "Take a moment to read the board with your own eyes — the engine bar will stay hidden until you've committed to your assessment.",
            questions   = listOf(
                trigger.coachingQuestion(),
                "What specific feature of the position led you to that conclusion — piece activity, pawn structure, or king safety?",
            ),
            conceptualHint = "Board reading is a skill built by practice, not by checking the eval bar. " +
                "When you commit to an assessment and then compare it to the engine, any gap tells you exactly which structural features you're not yet seeing instinctively.",
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
                description = "If you play that move, your opponent may have a way to attack two of your pieces at once. " +
                    "Let's figure out which ones before you commit.",
                questions = listOf(
                    "After your move, does your opponent have a piece that can jump to a square where it attacks two of your pieces simultaneously?",
                    "Are your pieces defending each other, or are they 'loose' — sitting on unrelated squares without support?",
                    "Can you rearrange your pieces to close off the fork square, or is there an alternative move that avoids the whole problem?",
                ),
                conceptualHint = "Forks work because one piece attacks multiple targets at once and the opponent " +
                    "can't save both. To prevent them, keep your valuable pieces on defended squares and avoid " +
                    "placing two pieces on squares that share a knight jump or a bishop diagonal. " +
                    "If you can see the fork square, you can steer around it.",
            )

            "hanging" -> Insight(
                emoji = "🎣",
                title = "Piece Left Hanging",
                description = "That move might leave one of your pieces undefended — free for your opponent to take. " +
                    "Let's check which one before you play it.",
                questions = listOf(
                    "After your move, which of your pieces would have zero defenders — can your opponent just take it for free?",
                    "Is there a way to achieve the same goal with a different move that keeps all your pieces protected?",
                    "Is the idea behind this move worth the risk of leaving something undefended — or is there a safer way to get the same thing?",
                ),
                conceptualHint = "Before every move, ask: 'Am I leaving anything undefended?' It takes five seconds " +
                    "and prevents most tactical losses. A piece is hanging when it has more attackers than defenders — " +
                    "and a hanging piece is almost always taken. This habit, sometimes called LPDO " +
                    "(Loose Pieces Drop Off), is the first thing every coach teaches.",
            )

            "checkmate" -> Insight(
                emoji = "♚",
                title = "King Safety Alert",
                description = "That move might expose your king to a checkmate threat. It's worth double-checking " +
                    "the king's safety before playing it — material means nothing if the king is mated.",
                questions = listOf(
                    "After your move, is your king safe? Can your opponent deliver check immediately — and is there a safe response?",
                    "Could your opponent set up a mating net in the next two moves if you go ahead with this?",
                    "Do you need to deal with the king's safety first, or can you continue with your plan and handle it later?",
                ),
                conceptualHint = "King safety always takes priority over material gain. If there's any doubt about " +
                    "checkmate threats, eliminate them before making aggressive moves. A single mating attack can " +
                    "cancel out a several-piece material advantage — it's happened to every player at some point.",
            )

            else -> Insight(
                emoji = "⚖️",
                title = "Evaluation Drop Detected",
                description = "That move drops the evaluation by ${cpLoss} centipawns — that's a $severityLabel shift. " +
                    "Let's figure out what the position was really asking for.",
                questions = listOf(
                    "What does the position look like after your move — does your opponent have an immediate improvement?",
                    "Does this move create a long-term weakness — an open file toward your king, a weak square, a loose piece?",
                    "Is there a calmer alternative that achieves the same idea but avoids the downside?",
                ),
                conceptualHint = "A significant evaluation drop usually means one of two things: the move allows " +
                    "an immediate tactic, or it creates a long-term structural weakness. To diagnose which, ask: " +
                    "'What is my opponent's best reply to this move?' If you can't answer that, the move may be " +
                    "premature — recalculate with the opponent's best response in mind.",
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
            description = "That isolated pawn in the center is a double-edged sword — it gives your pieces " +
                "open lines and active squares, but your opponent will try to blockade and target it as a long-term weakness.",
            questions   = listOf(
                "Who controls the blockade square directly in front of the isolated pawn — could a knight or bishop land there permanently?",
                "Are you better served by trading pieces to reduce the attack on your pawn, or keeping pieces active to maximise your dynamics?",
                "Is your opponent targeting the pawn directly with rooks or heavy pieces — and can you defend it while pursuing your own play?",
            ),
            conceptualHint = "The IQP is a structural trade-off: dynamic piece activity now, potential long-term weakness later. " +
                "The owner should keep pieces active and avoid mass exchanges — each trade makes the pawn weaker. " +
                "The opponent's strategy is to blockade the pawn (usually with a knight) and then target it with rooks. " +
                "Whoever controls the tempo of that race usually decides the game.",
        )

        PlanType.HANGING_PAWNS -> Insight(
            emoji       = "⚠️",
            title       = "Hanging Pawns",
            description = "Those connected pawns on c and d are an interesting structural feature — they control a lot of space, " +
                "but with no friendly pawns beside them, they can become targets if the position closes down.",
            questions   = listOf(
                "Right now, are those pawns acting as a strength — an attacking wedge pushing forward — or a weakness being targeted by your opponent?",
                "Can you advance one of them to create a passed pawn before your opponent gets a blockade in place?",
                "Are your pieces coordinated well enough to support a pawn advance right now?",
            ),
            conceptualHint = "Hanging pawns are dynamic — advance them when your pieces are active and your opponent " +
                "hasn't organised a blockade. Once your pieces lose their activity, the pawns can become a liability. " +
                "If the position becomes defensive, consider trading one pawn to reach a stable IQP structure " +
                "rather than letting both become targets.",
        )

        PlanType.PAWN_MAJORITY -> Insight(
            emoji       = "⚖️",
            title       = plan.title,
            description = "You have an extra pawn on one side of the board — a majority that, if handled well, " +
                "creates a passed pawn heading into the endgame.",
            questions   = listOf(
                "Is your majority mobile — can the pawns advance freely — or are they blocked by your opponent's pieces or pawns?",
                "Is this the right moment to exchange center pawns and activate the majority?",
                "Is your opponent trying to organise a minority attack on the opposite wing to create weaknesses before you can cash in?",
            ),
            conceptualHint = "A healthy pawn majority converts to a passed pawn in the endgame — a powerful asset. " +
                "But an immobile majority is worth nothing. The plan: liquidate the center to reduce piece complications, " +
                "then push the majority. Avoid unnecessary pawn advances that self-block your own pawns.",
        )

        PlanType.OPEN_FILE -> Insight(
            emoji       = "♜",
            title       = plan.title,
            description = "The ${plan.affectedFile}-file is fully open right now — no pawns blocking it on either side. " +
                "That's an invitation for rooks, and the player who gets there first usually controls it.",
            questions   = listOf(
                "Which of your rooks benefits most from this file — the one closest to the action?",
                "Can you double both rooks on this file to double the pressure?",
                "Where does the open file lead — toward the opponent's king, a weak back-rank square, or an entry point for your pieces?",
            ),
            conceptualHint = "Open files are highways for rooks. Place a rook first, then double — two rooks on an open file " +
                "exert decisive pressure and are very hard to dislodge. Look for the outpost square or the 7th rank " +
                "waiting at the far end of the file.",
        )

        PlanType.HALF_OPEN_FILE -> Insight(
            emoji       = "↕",
            title       = plan.title,
            description = "You have a half-open ${plan.affectedFile}-file — no friendly pawn in the way, just an enemy pawn sitting there. " +
                "That pawn is a target, and your rook would love this file.",
            questions   = listOf(
                "What's the rook's target on this file — the enemy pawn, the seventh rank, or the king behind it?",
                "Is there a pawn break that could blow this file open further and convert it into a fully open file?",
                "Is the pawn on this file defended? If not, it's already a direct target.",
            ),
            conceptualHint = "A rook on a half-open file pressures the enemy pawn and prevents it from advancing. " +
                "The long-term goal: win or exchange that blocking pawn to convert the file to a fully open one, " +
                "where your rook's activity doubles.",
        )

        PlanType.DOUBLED_PAWNS -> Insight(
            emoji       = "⬆",
            title       = plan.title,
            description = "You have doubled pawns on the ${plan.affectedFile}-file — they're less mobile than normal, " +
                "but they usually came with a compensation: an adjacent open or half-open file that your pieces can use.",
            questions   = listOf(
                "Which open or half-open file did those doubled pawns create — and are you using it aggressively?",
                "Are these doubled pawns a long-term static weakness, or do they give you enough dynamic compensation to make it worthwhile?",
                "Is there an aggressive plan that uses the open file enough to justify the structural cost?",
            ),
            conceptualHint = "Doubled pawns aren't always a weakness — they control squares and often open adjacent files for rooks. " +
                "The key question: do your pieces have enough activity to compensate for the reduced pawn mobility? " +
                "If yes, play actively and use the open file. If not, look for a way to trade one of the doubled pawns.",
        )

        PlanType.BACKWARD_PAWN -> Insight(
            emoji       = "🎯",
            title       = plan.title,
            description = "That pawn on the ${plan.affectedFile}-file can't safely advance — it's behind its neighbours and stuck in place. " +
                "That makes it a long-term target for your opponent.",
            questions   = listOf(
                "Can your opponent place a piece permanently on the square directly in front of that backward pawn as an outpost?",
                "Is there a way to advance or trade off the backward pawn before your opponent fixes it in place for good?",
                "What square does the backward pawn leave weak — and can your opponent put a piece there that can't be dislodged?",
            ),
            conceptualHint = "A backward pawn on an open file is a classic long-term weakness. The opponent's strategy: " +
                "target it with rooks, place a piece on the square in front as a permanent blockade, and let it slowly suffocate. " +
                "The defender needs active piece play to create counter-threats and keep the position dynamic — " +
                "passive defence rarely holds.",
        )

        PlanType.PAWN_CHAIN -> Insight(
            emoji       = "🔗",
            title       = "Pawn Chain",
            description = "You have a diagonal pawn chain — it controls a lot of space and is hard to break from the front. " +
                "But every chain has a weak link: its base.",
            questions   = listOf(
                "Where is the base of the chain — which pawn, if captured, would unravel the entire structure?",
                "Can you attack the base of your opponent's chain while reinforcing the base of your own?",
                "Where is the action on the board — which side of the pawn chain are you aiming for?",
            ),
            conceptualHint = "Pawn chain strategy comes from Nimzowitsch: attack the base, not the head. " +
                "The head pawn is the most advanced but is supported by the pawns below it; the base pawn " +
                "supports the entire chain but has no pawn protecting it. Attacking the base collapses the chain from underneath.",
        )
    }
}
