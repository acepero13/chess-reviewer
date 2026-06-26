package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.data.model.NotablePosition
import com.acepero13.chess.core.ui.board.BoardState
import com.acepero13.chess.core.ui.board.ChessBoard
import com.acepero13.chess.core.ui.theme.LocalAppColors

/**
 * A horizontally scrollable strip of small board thumbnails for [NotablePosition]s — the
 * "Missed Simplifications" / tactic-position carousel. Reuses the chess-core board in
 * `thumbnailMode`, so no coordinates / interaction chrome is drawn.
 */
@Composable
fun NotablePositionCarousel(
    title: String,
    positions: List<NotablePosition>,
    modifier: Modifier = Modifier,
    boardSize: Int = 132,
) {
    InsightCard(title = title, modifier = modifier) {
        if (positions.isEmpty()) {
            val appColors = LocalAppColors.current
            Text(
                text = "Nothing flagged yet — analyze more games to populate this.",
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textSecondary,
            )
            return@InsightCard
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            items(positions, key = { it.id }) { pos ->
                Column(modifier = Modifier.width(boardSize.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ChessBoard(
                        boardState    = BoardState(fen = pos.fen),
                        onSquareTap   = {},
                        modifier      = Modifier.size(boardSize.dp),
                        thumbnailMode = true,
                    )
                    Caption(played = pos.playedMove, best = pos.bestMove)
                }
            }
        }
    }
}

@Composable
private fun Caption(played: String, best: String) {
    val appColors = LocalAppColors.current
    Column {
        Text(
            text = "Played: $played",
            style = MaterialTheme.typography.labelSmall,
            color = appColors.textSecondary,
        )
        if (best.isNotBlank()) {
            Text(
                text = "Best: $best",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = appColors.gold,
            )
        }
    }
}
