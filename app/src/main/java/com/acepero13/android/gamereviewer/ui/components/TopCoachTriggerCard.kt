package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.data.repository.TriggerMasteryRepository
import com.acepero13.android.gamereviewer.ui.screens.TopCoachTrigger
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors

private val TIER_LABEL = mapOf(
    0 to "Tactical",
    1 to "Tactical",
    2 to "Strategic",
    3 to "Coordination",
    4 to "Positional",
)

private val TYPE_TIPS = mapOf(
    "SAFETY"               to "Scan for king exposure threats before every move you play.",
    "CANDIDATE_MOVES"      to "List at least two candidate moves and weigh them before deciding.",
    "WORST_PIECE"          to "Ask yourself: which of my pieces is doing the least — and how can I improve it?",
    "FORCING_MOVE"         to "Always check for checks, captures, and threats before settling on a quiet move.",
    "OPPONENT_PLAN"        to "After every opponent move, ask: what are they threatening on the next turn?",
    "PRE_MOVE_CHECKLIST"   to "Before committing, scan the whole board for hanging pieces and unguarded squares.",
    "ROOK_ACTIVATION"      to "Move your rooks to open or half-open files — a rook on a closed file is a sleeping piece.",
    "IMPULSE_CONTROL"      to "Pause before playing fast — verify your opponent's best forcing reply before moving.",
    "CALCULATION_BLUNDER"  to "When you spend time calculating, map out the critical line fully before committing.",
    "TACTICAL_OVERSIGHT"   to "With time on the clock, there is no excuse — always check forcing moves before deciding.",
    "CANDIDATE_SEARCH"     to "Compare your top two candidates explicitly: one sharp, one solid — then choose.",
    "CCT_CHECK"            to "After every move, ask yourself: can my opponent Check, Capture, or Threaten something?",
    "CONVERSION_STRATEGY"  to "When ahead, simplify. Calculate the safest path to the win before going for complications.",
    "COORDINATED_ATTACK"   to "Ensure all your attacking pieces point at the same target before launching an attack.",
    "PIECE_HARMONY"        to "Look for moves that improve two pieces at once — good moves serve multiple purposes.",
    "PUNISH_BLUNDER"       to "When your opponent errs, find the most concrete and immediate refutation.",
)

/**
 * Dashboard card highlighting the single coaching habit the user should focus on next.
 *
 * Selection logic (in DashboardViewModel): lowest mastery streak among non-mastered triggers,
 * with coaching tier as tiebreaker so Tier-1 tactical habits are prioritised over positional ones.
 */
@Composable
fun TopCoachTriggerCard(
    trigger:  TopCoachTrigger,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    val progress = (trigger.streak.toFloat() / TriggerMasteryRepository.MASTERY_THRESHOLD)
        .coerceIn(0f, 1f)
    val tierLabel = TIER_LABEL[trigger.tier] ?: "Positional"
    val tip = TYPE_TIPS[trigger.typeName]
        ?: "Identify this pattern correctly ${TriggerMasteryRepository.MASTERY_THRESHOLD}× in a row in Reflection Mode."

    Card(
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = appColors.surface),
        border   = BorderStroke(1.dp, ChessGold.copy(alpha = 0.25f)),
        modifier = modifier,
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Trigger header ─────────────────────────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text  = trigger.emoji,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = trigger.title,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = ChessGold,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        TierBadge(tierLabel)
                        Text(
                            text  = "·  ${trigger.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = appColors.textTertiary,
                        )
                    }
                }
            }

            // ── Study tip ──────────────────────────────────────────────────────
            Text(
                text  = tip,
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textSecondary,
            )

            // ── Mastery progress ───────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        text  = "Mastery streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.textTertiary,
                    )
                    Text(
                        text       = "${trigger.streak} / ${TriggerMasteryRepository.MASTERY_THRESHOLD} correct in a row",
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = ChessGold,
                    )
                }
                LinearProgressIndicator(
                    progress   = { progress },
                    modifier   = Modifier.fillMaxWidth().height(6.dp),
                    color      = ChessGold,
                    trackColor = ChessGold.copy(alpha = 0.15f),
                    strokeCap  = StrokeCap.Round,
                )
            }

            // ── Instruction nudge ──────────────────────────────────────────────
            Text(
                text  = "Build this streak in Reflection Mode — the Coach Lamp goes silent once you master it.",
                style = MaterialTheme.typography.labelSmall,
                color = appColors.textTertiary,
            )
        }
    }
}

@Composable
private fun TierBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ChessGold.copy(alpha = 0.12f),
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelSmall,
            color    = ChessGold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
