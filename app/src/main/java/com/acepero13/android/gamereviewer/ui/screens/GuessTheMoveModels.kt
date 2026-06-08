package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.ui.graphics.Color
import com.acepero13.android.gamereviewer.data.model.GuessMoveProgress
import com.acepero13.chess.core.ui.board.Arrow
import com.acepero13.chess.core.ui.board.BoardState
import com.acepero13.chess.core.ui.components.TreeDisplayItem

enum class GuessingSide { BOTH, WHITE_ONLY, BLACK_ONLY }

enum class GuessTheMovePhase { CHOOSING_SIDE, SELECTING, LOADING, GUESSING, MOVE_REVEALED, GAME_COMPLETE, REVIEWING }

sealed class MasterGameSource {
    object Offline : MasterGameSource()
    data class OnlineFamousPlayer(
        val displayName: String,
        val username: String,
        val platform: String,
    ) : MasterGameSource()
    data class OnlineCustom(val username: String, val platform: String) : MasterGameSource()
}

val FAMOUS_MASTERS = listOf(
    MasterGameSource.OnlineFamousPlayer("Magnus Carlsen",     "DrNykterstein",   "lichess"),
    MasterGameSource.OnlineFamousPlayer("Hikaru Nakamura",    "Hikaru",           "lichess"),
    MasterGameSource.OnlineFamousPlayer("Fabiano Caruana",    "FabianoCaruana",   "lichess"),
    MasterGameSource.OnlineFamousPlayer("Ding Liren",         "DingLiren",        "lichess"),
    MasterGameSource.OnlineFamousPlayer("Praggnanandhaa",     "rpchess",          "lichess"),
    MasterGameSource.OnlineFamousPlayer("Alireza Firouzja",   "Alireza2003",      "lichess"),
    MasterGameSource.OnlineFamousPlayer("Ian Nepomniachtchi", "lachesisQ",        "lichess"),
    MasterGameSource.OnlineFamousPlayer("Anish Giri",         "anishgiri",        "lichess"),
    MasterGameSource.OnlineFamousPlayer("Wesley So",          "GMWesleyso",       "chesscom"),
    MasterGameSource.OnlineFamousPlayer("Levon Aronian",      "LevonAronian",     "chesscom"),
)

data class GuessTheMoveUiState(
    val phase: GuessTheMovePhase = GuessTheMovePhase.SELECTING,
    val selectedSource: MasterGameSource = MasterGameSource.Offline,
    val customUsername: String = "",
    val selectedPlatform: String = "lichess",
    val selectedSide: GuessingSide = GuessingSide.BOTH,
    val fetchError: String? = null,
    val gameDescription: String = "",
    val sourceLabel: String = "",
    val whitePlayer: String = "",
    val blackPlayer: String = "",
    val masterMoves: List<String> = emptyList(),
    val moveAnnotations: Map<Int, String> = emptyMap(),
    val currentMoveIndex: Int = 0,
    val boardState: BoardState = BoardState(),
    val isEditorMode: Boolean = false,
    val userMoveSan: String = "",
    val masterMoveSan: String = "",
    val wasExactMatch: Boolean = false,
    val originalAnnotation: String? = null,
    val opponentAnnotation: String? = null,
    val preambleAnnotation: String? = null,
    val preambleDismissed: Boolean = false,
    val treeItems: List<TreeDisplayItem> = emptyList(),
    val currentUserComment: String = "",
    val currentArrowColor: Color = Color(0xFFFFD700),
    val exactMatches: Int = 0,
    val totalPresented: Int = 0,
    val engineThinking: Boolean = false,
    val engineArrow: Arrow? = null,
    val engineEvalCp: Int? = null,
    val fenHistory: List<String> = emptyList(),
    val masterSanHistory: List<String> = emptyList(),
    val reviewIndex: Int = 0,
    val browseIndex: Int? = null,
    val browseBoardState: BoardState? = null,
    val showBookmarkSheet: Boolean = false,
    val showExplorer: Boolean = false,
    val pendingResume: GuessMoveProgress? = null,
)
