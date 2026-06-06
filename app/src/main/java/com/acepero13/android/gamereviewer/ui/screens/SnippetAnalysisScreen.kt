package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.data.model.Snippet
import com.acepero13.chess.core.ui.board.BoardState
import com.acepero13.chess.core.ui.board.ChessBoard
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetAnalysisScreen(
    snippetId:          Long,
    onBack:             () -> Unit,
    onOpenOriginalGame: (gameId: Long, moveIndex: Int) -> Unit,
    vm:                 SnippetAnalysisViewModel = koinViewModel(parameters = { parametersOf(snippetId) }),
) {
    val state by vm.uiState.collectAsState()

    val snippet = state.snippet
    when {
        state.loading       -> SnippetLoadingView()
        snippet == null     -> SnippetNotFoundView(onBack = onBack)
        snippet.sourceGameId == null -> OrphanSnippetView(snippet = snippet, onBack = onBack)
        else -> {
            val gameId = snippet.sourceGameId
            AnalysisScreen(
                gameId             = gameId,
                onBack             = onBack,
                initialMoveIndex   = snippet.moveIndex,
                snippetTitle       = snippet.title,
                onOpenOriginalGame = { onOpenOriginalGame(gameId, snippet.moveIndex) },
            )
        }
    }
}

@Composable
private fun SnippetLoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ChessGold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SnippetNotFoundView(onBack: () -> Unit) {
    val appColors = LocalAppColors.current
    Scaffold(
        containerColor = appColors.background,
        topBar = {
            TopAppBar(
                title          = { Text("Snippet not found", color = ChessGold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, "Back", tint = ChessGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appColors.background),
            )
        },
    ) { padding ->
        Box(
            modifier           = Modifier.fillMaxSize().padding(padding),
            contentAlignment   = Alignment.Center,
        ) {
            Text(
                text      = "This snippet no longer exists.",
                color     = appColors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrphanSnippetView(snippet: Snippet, onBack: () -> Unit) {
    val appColors = LocalAppColors.current
    Scaffold(
        containerColor = appColors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = snippet.title,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = ChessGold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, "Back", tint = ChessGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appColors.background),
            )
        },
    ) { padding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ChessBoard(
                boardState    = BoardState(fen = snippet.fen),
                onSquareTap   = {},
                modifier      = Modifier.fillMaxWidth(),
                thumbnailMode = false,
            )
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = appColors.surface),
                border   = BorderStroke(1.dp, appColors.border),
                shape    = RoundedCornerShape(8.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(
                        imageVector        = Icons.Outlined.Bookmark,
                        contentDescription = null,
                        tint               = ChessGold.copy(alpha = 0.6f),
                        modifier           = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text  = "Source game was deleted",
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.textTertiary,
                    )
                    if (snippet.notes.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text  = snippet.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = appColors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}
