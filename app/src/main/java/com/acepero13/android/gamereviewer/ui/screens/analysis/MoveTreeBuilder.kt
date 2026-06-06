package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.chess.core.ui.components.TreeDisplayItem
import kotlinx.coroutines.flow.update

internal class MoveTreeBuilder(private val session: GameSession) {

    fun buildTreeItems(currentIndex: Int): List<TreeDisplayItem> {
        val items  = mutableListOf<TreeDisplayItem>()
        var moveNo = 1
        session.sanMoves.forEachIndexed { idx, san ->
            val mIdx    = idx + 1
            val isWhite = idx % 2 == 0
            val fen     = session.fenSequence.getOrElse(mIdx) { session.startFen }
            val annot   = session.annotationCache[fen]
            val userComment = annot?.moveComment ?: ""
            val comment = userComment.ifBlank { session.pgnAnnotations[idx] ?: "" }
            val hasAnnot = comment.isNotBlank() ||
                (annot?.arrowsJson?.length ?: 0) > 2 ||
                (annot?.markedSquaresJson?.length ?: 0) > 2
            items.add(
                TreeDisplayItem.MoveItem(
                    nodeId         = mIdx.toLong(),
                    san            = san,
                    fen            = fen,
                    comment        = comment,
                    hasAnnotations = hasAnnot,
                    isCurrentMove  = mIdx == currentIndex,
                    depth          = 0,
                    moveNumber     = moveNo,
                    isWhiteMove    = isWhite,
                    showMoveNumber = isWhite,
                )
            )
            if (!isWhite) moveNo++
        }
        return items
    }

    fun refreshTreeItems() {
        session.uiState.update { it.copy(treeItems = buildTreeItems(it.moveIndex)) }
    }
}
