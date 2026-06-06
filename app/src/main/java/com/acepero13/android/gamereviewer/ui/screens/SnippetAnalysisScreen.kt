package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
        state.loading                -> SnippetLoadingView()
        snippet == null              -> SnippetNotFoundView(onBack = onBack)
        snippet.sourceGameId == null -> OrphanSnippetScreen(snippet = snippet, onBack = onBack)
        else -> {
            val gameId = snippet.sourceGameId
            AnalysisScreen(
                gameId             = gameId,
                onBack             = onBack,
                initialMoveIndex   = snippet.moveIndex,
                startInExploreMode = true,
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
            modifier         = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text      = "This snippet no longer exists.",
                color     = appColors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
