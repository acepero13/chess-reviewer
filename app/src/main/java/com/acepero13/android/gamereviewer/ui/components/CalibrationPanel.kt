package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acepero13.android.gamereviewer.domain.CalibrationContext
import com.acepero13.android.gamereviewer.domain.CoachingTrigger
import com.acepero13.chess.core.ui.theme.LocalAppColors

private val CalibAccent   = Color(0xFF9B7FD4)
private val CalibPositive = Color(0xFF2D6A4F)
private val CalibNegative = Color(0xFF6B3A2A)
private val CalibBorder   = Color(0xFF4A3F6B)

/** Labels and colours for the 5-point evaluation scale. */
private data class EvalOption(val value: Int, val shortLabel: String, val fullLabel: String, val tint: Color)

private val EVAL_OPTIONS = listOf(
    EvalOption(-2, "−−", "Strong Black",  Color(0xFF4A90D9)),
    EvalOption(-1, "−",  "Slight Black",  Color(0xFF7AB4E8)),
    EvalOption( 0, "=",  "Equal",         Color(0xFFB0A060)),
    EvalOption( 1, "+",  "Slight White",  Color(0xFFE8C880)),
    EvalOption( 2, "++", "Strong White",  Color(0xFFF5E06A)),
)

/**
 * Self-assessment calibration quiz panel.
 *
 * Shown when an [CoachingTrigger.EvalCalibration] trigger fires.  The user selects one of five
 * evaluation categories and locks in their answer before the engine's verdict is revealed.
 */
@Composable
fun CalibrationPanel(
    trigger:       CoachingTrigger.EvalCalibration,
    visible:       Boolean,
    selectedValue: Int,
    locked:        Boolean,
    feedback:      String,
    feedbackPositive: Boolean,
    onValueChange: (Int) -> Unit,
    onLockIn:      () -> Unit,
    onDismiss:     () -> Unit,
    modifier:      Modifier = Modifier,
) {
    AnimatedVisibility(
        visible       = visible,
        enter         = expandVertically() + fadeIn(),
        exit          = shrinkVertically() + fadeOut(),
        modifier      = modifier,
    ) {
        val appColors = LocalAppColors.current
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = CardDefaults.cardColors(containerColor = appColors.surface),
            border   = BorderStroke(1.dp, CalibBorder),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // ── Header ──────────────────────────────────────────────────
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment   = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text       = trigger.emoji(),
                            fontSize   = 16.sp,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text       = trigger.title(),
                            color      = CalibAccent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp,
                        )
                    }
                    IconButton(
                        onClick  = onDismiss,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Dismiss",
                            tint = appColors.textTertiary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = CalibBorder.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))

                AnimatedContent(
                    targetState    = locked,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label          = "calibContent",
                ) { isLocked ->
                    if (!isLocked) {
                        // ── Quiz phase ───────────────────────────────────────
                        Column {
                            Text(
                                text     = trigger.coachingQuestion(),
                                color    = appColors.textPrimary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                            )

                            Spacer(Modifier.height(10.dp))

                            // 5-button evaluation selector
                            Row(
                                modifier            = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                EVAL_OPTIONS.forEach { option ->
                                    val isSelected = selectedValue == option.value
                                    OutlinedButton(
                                        onClick  = { onValueChange(option.value) },
                                        modifier = Modifier.weight(1f),
                                        shape    = RoundedCornerShape(8.dp),
                                        border   = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) option.tint else CalibBorder,
                                        ),
                                        colors   = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSelected) option.tint.copy(alpha = 0.18f) else Color.Transparent,
                                            contentColor   = if (isSelected) option.tint else appColors.textTertiary,
                                        ),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text       = option.shortLabel,
                                                fontSize   = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            )
                                            Text(
                                                text     = option.fullLabel,
                                                fontSize = 8.sp,
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Button(
                                onClick  = onLockIn,
                                modifier = Modifier.fillMaxWidth(),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = CalibAccent,
                                    contentColor   = Color.White,
                                ),
                                shape    = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text       = "Lock In My Assessment",
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    } else {
                        // ── Feedback phase ───────────────────────────────────
                        Column {
                            val selectedOption = EVAL_OPTIONS.find { it.value == selectedValue }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (feedbackPositive) Icons.Outlined.CheckCircle else Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint     = if (feedbackPositive) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text       = "You said: ${selectedOption?.fullLabel ?: ""}",
                                    color      = selectedOption?.tint ?: appColors.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 13.sp,
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            val feedbackBg = if (feedbackPositive) CalibPositive.copy(alpha = 0.25f)
                                             else CalibNegative.copy(alpha = 0.25f)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(8.dp),
                                colors   = CardDefaults.cardColors(containerColor = feedbackBg),
                            ) {
                                Text(
                                    text      = feedback,
                                    color     = appColors.textPrimary,
                                    fontSize  = 12.sp,
                                    lineHeight = 17.sp,
                                    fontStyle = FontStyle.Italic,
                                    modifier  = Modifier.padding(10.dp),
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            Button(
                                onClick  = onDismiss,
                                modifier = Modifier.fillMaxWidth(),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = CalibBorder,
                                    contentColor   = Color.White,
                                ),
                                shape    = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text     = "Got it",
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
