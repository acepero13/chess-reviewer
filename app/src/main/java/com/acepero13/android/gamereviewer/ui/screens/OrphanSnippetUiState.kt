package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.ui.graphics.Color
import com.acepero13.chess.core.ui.board.BoardState
import com.acepero13.chess.core.ui.components.TreeDisplayItem

data class OrphanSnippetUiState(
    val boardState: BoardState = BoardState(),
    val isEditMode: Boolean = false,
    val engineVisible: Boolean = false,
    val evalBarVisible: Boolean = false,
    val engineThinking: Boolean = false,
    val evalCp: Int? = null,
    val currentComment: String = "",
    val currentArrowColor: Color = Color(0xFFFFD700),
    val canGoBack: Boolean = false,
    val startFen: String = "",
    val treeItems: List<TreeDisplayItem> = emptyList(),
)
