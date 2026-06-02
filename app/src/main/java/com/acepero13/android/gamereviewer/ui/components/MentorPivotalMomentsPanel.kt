package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.PivotalMoment
import com.acepero13.android.gamereviewer.domain.PivotalMomentRole
import com.acepero13.android.gamereviewer.domain.PivotalMoments
import com.acepero13.chess.core.ui.theme.ChessGold

private val PanelBg     = Color(0xFF0D1B14)
private val PanelBorder = Color(0xFF2A5C3F)
private val LabelColor  = Color(0xFFCCE8D9)

private val RoleAccent = mapOf(
    PivotalMomentRole.TURNING_POINT      to Color(0xFF4FC3F7),  // blue
    PivotalMomentRole.MISSED_OPPORTUNITY to Color(0xFFFFB74D),  // amber
    PivotalMomentRole.EDUCATIONAL_MOMENT to Color(0xFF81C784),  // green
)

@Composable
fun MentorPivotalMomentsPanel(
    moments:  PivotalMoments,
    onReview: (moveIndex: Int) -> Unit,
    onSkip:   () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        Text(
            text       = "The Big Three",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = ChessGold,
        )
        Text(
            text  = "Three moments that shaped this game. Review each to understand what happened.",
            style = MaterialTheme.typography.bodySmall,
            color = LabelColor.copy(alpha = 0.7f),
        )

        HorizontalDivider(color = PanelBorder.copy(alpha = 0.5f))

        moments.all.forEach { moment ->
            PivotalMomentCard(
                moment   = moment,
                onReview = { onReview(moment.moveIndex) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (moments.all.isEmpty()) {
            Text(
                text  = "Not enough analyzed data to identify pivotal moments yet.",
                style = MaterialTheme.typography.bodySmall,
                color = LabelColor.copy(alpha = 0.6f),
            )
        }

        HorizontalDivider(color = PanelBorder.copy(alpha = 0.5f))

        OutlinedButton(
            onClick  = onSkip,
            modifier = Modifier.fillMaxWidth(),
            border   = BorderStroke(1.dp, ChessGold.copy(alpha = 0.4f)),
            colors   = ButtonDefaults.outlinedButtonColors(
                contentColor = ChessGold,
            ),
        ) {
            Text(
                text  = "Review in order",
                style = MaterialTheme.typography.labelMedium,
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PivotalMomentCard(
    moment:   PivotalMoment,
    onReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = RoleAccent[moment.role] ?: ChessGold

    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(containerColor = PanelBg),
        border   = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text  = moment.role.emoji,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text       = moment.role.label,
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = accent,
                )
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text  = moment.role.tagline,
                style = MaterialTheme.typography.bodySmall,
                color = LabelColor.copy(alpha = 0.6f),
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text       = "Move ${moment.fullMoveNumber}. (${moment.sideLabel})",
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color      = LabelColor,
                    )
                    Text(
                        text  = buildMomentSubtitle(moment),
                        style = MaterialTheme.typography.labelSmall,
                        color = LabelColor.copy(alpha = 0.55f),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onReview,
                    shape   = RoundedCornerShape(6.dp),
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = accent.copy(alpha = 0.18f),
                        contentColor   = accent,
                    ),
                ) {
                    Text(
                        text  = "Review",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (moment.role == PivotalMomentRole.EDUCATIONAL_MOMENT && moment.recurringCategory.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "Pattern: ${formatCategory(moment.recurringCategory)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.copy(alpha = 0.75f),
                )
            }
        }
    }
}

private fun buildMomentSubtitle(moment: PivotalMoment): String {
    val severity = moment.severityLabel
    val motif    = when (moment.motif) {
        "checkmate" -> "forced mate available"
        "fork"      -> "fork was available"
        "hanging"   -> "hanging material"
        else        -> null
    }
    return if (motif != null) "$severity · $motif" else severity
}

private fun formatCategory(raw: String): String =
    raw.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
