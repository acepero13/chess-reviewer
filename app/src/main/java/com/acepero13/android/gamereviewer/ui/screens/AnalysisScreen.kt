package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.ui.board.ChessBoard
import com.acepero13.chess.core.ui.components.EvalBar
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.WCDark
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    gameId: Long,
    onBack: () -> Unit,
    vm: AnalysisViewModel = koinViewModel(parameters = { parametersOf(gameId) }),
) {
    val state by vm.uiState.collectAsState()
    val game = state.game

    Scaffold(
        containerColor = WCDark,
        topBar = {
            TopAppBar(
                title = {
                    if (game != null) {
                        Column {
                            Text(
                                "${game.whitePlayer} vs ${game.blackPlayer}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ChessGold,
                            )
                            if (game.openingName.isNotEmpty()) {
                                Text(
                                    "${game.openingEco} · ${game.openingName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, "Back", tint = ChessGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WCDark),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            // ── Eval bar ────────────────────────────────────────────────────
            EvalBar(
                evalCp   = state.engineResult?.score ?: 0,
                thinking = state.isAnalysing,
                modifier = Modifier.fillMaxWidth().height(18.dp),
            )

            // ── Chess board ─────────────────────────────────────────────────
            ChessBoard(
                boardState    = state.boardState,
                onSquareTap   = { /* read-only in analysis */ },
                onArrowDrawn  = { from, to -> vm.onArrowDrawn(from, to) },
                onSquareMarked = { sq -> vm.onSquareMarked(sq) },
                modifier      = Modifier.fillMaxWidth(),
            )

            // ── Move navigation ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = vm::goToStart) {
                    Icon(Icons.Outlined.FastRewind, "Start", tint = ChessGold)
                }
                IconButton(onClick = vm::stepBackward) {
                    Icon(Icons.Outlined.SkipPrevious, "Previous", tint = ChessGold)
                }
                Text(
                    text = "${state.moveIndex} / ${state.totalMoves}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = vm::stepForward) {
                    Icon(Icons.Outlined.SkipNext, "Next", tint = ChessGold)
                }
                IconButton(onClick = vm::goToEnd) {
                    Icon(Icons.Outlined.FastForward, "End", tint = ChessGold)
                }
            }

            // ── Engine line ─────────────────────────────────────────────────
            val result = state.engineResult
            if (state.isAnalysing) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            } else if (result != null) {
                Text(
                    text = "Best: ${result.pv.take(5).joinToString(" ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Move comment ────────────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            var comment by remember(state.moveIndex) {
                mutableStateOf("")   // TODO: load from annotation
            }
            OutlinedTextField(
                value = comment,
                onValueChange = {
                    comment = it
                    vm.updateMoveComment(it)
                },
                label = { Text("Move comment") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )
        }
    }
}
