package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.engine.highlights.GameHighlight
import com.acepero13.android.gamereviewer.engine.highlights.HighlightSeverity
import com.acepero13.chess.core.ui.components.MoveTree
import com.acepero13.chess.core.ui.components.TreeDisplayItem
import com.acepero13.chess.core.ui.theme.ChessGold

// ── Severity colours ─────────────────────────────────────────────────────────
private val ColorCritical  = Color(0xFFE53935)
private val ColorImportant = Color(0xFFFF9800)
private val ColorNotable   = Color(0xFF4CAF50)

private fun severityColor(s: HighlightSeverity) = when (s) {
    HighlightSeverity.CRITICAL  -> ColorCritical
    HighlightSeverity.IMPORTANT -> ColorImportant
    HighlightSeverity.NOTABLE   -> ColorNotable
}

/**
 * Content panel for Navigate mode.
 *
 * Shows the [MoveTree] and, when a comment exists at the current position,
 * a read-only comment card so the annotation is never silently missed.
 * Engine highlights are intentionally withheld here (Human-First philosophy).
 *
 * When [isOverthougt] is true, a subtle warning badge appears indicating the
 * player spent significant time calculating but still blundered — a sign of
 * a flawed calculation process rather than a rushed decision.
 */
@Composable
fun NavigateModeContent(
    entries: List<TreeDisplayItem>,
    onNodeClick: (Long) -> Unit,
    currentComment: String = "",
    isOverthougt: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier            = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MoveTree(
            entries     = entries,
            onNodeClick = onNodeClick,
            modifier    = Modifier.fillMaxWidth(),
        )
        if (isOverthougt) {
            OverthougtBanner(modifier = Modifier.fillMaxWidth())
        }
        if (currentComment.isNotBlank()) {
            CommentBanner(comment = currentComment, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun OverthougtBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFF9800).copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text  = "⏱",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text  = "Overthought — long calculation that still produced a blunder",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFF9800),
        )
    }
}

@Composable
private fun CommentBanner(comment: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF4CAF50).copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector        = Icons.Outlined.Comment,
            contentDescription = null,
            tint               = Color(0xFF4CAF50),
            modifier           = Modifier.size(16.dp),
        )
        Text(
            text      = comment,
            style     = MaterialTheme.typography.bodySmall,
            color     = MaterialTheme.colorScheme.onSurface,
            fontStyle = FontStyle.Italic,
        )
    }
}

/**
 * A single highlight chip showing:
 * - Colored left border (severity)
 * - Move number + SAN
 * - Highlight title
 *
 * Internal so [AnalysePanel] can reuse it without duplicating the card layout.
 */
@Composable
internal fun HighlightChip(
    highlight: GameHighlight,
    onClick:   () -> Unit,
    modifier:  Modifier = Modifier,
) {
    val color = severityColor(highlight.severity)
    val moveLabel = buildString {
        append(highlight.moveNumber)
        if (highlight.isWhiteMove) append(".") else append("…")
        append(highlight.moveSan)
    }

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape    = RoundedCornerShape(6.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row {
            // Colored severity strip on the left
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                    .background(color),
            )
            Column(
                modifier      = Modifier
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text       = moveLabel,
                        style      = MaterialTheme.typography.labelSmall,
                        color      = color,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text  = highlight.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text  = highlight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}

/**
 * Standalone composable that shows a scrollable list of [GameHighlight] chips.
 * Exposed for reuse outside [NavigateModeContent] if needed.
 */
@Composable
fun GameHighlightsTimeline(
    highlights:       List<GameHighlight>,
    onHighlightClick: (Int) -> Unit,
    modifier:         Modifier = Modifier,
) {
    if (highlights.isEmpty()) return

    LazyColumn(
        modifier       = modifier,
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(highlights, key = { "${it.moveIndex}_${it.ruleType}" }) { highlight ->
            HighlightChip(
                highlight = highlight,
                onClick   = { onHighlightClick(highlight.moveIndex) },
                modifier  = Modifier.fillMaxWidth(),
            )
        }
    }
}
