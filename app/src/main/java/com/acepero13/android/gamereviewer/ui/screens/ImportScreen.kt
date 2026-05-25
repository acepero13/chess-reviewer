package com.acepero13.android.gamereviewer.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.CorrectGreen
import com.acepero13.chess.core.ui.theme.WCDark
import com.acepero13.chess.core.ui.theme.WrongRed
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    vm: ImportViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? -> uri?.let { vm.importFromUri(it, context) } },
    )

    var chessComUsername by remember { mutableStateOf("") }
    var lichessUsername  by remember { mutableStateOf("") }

    Scaffold(
        containerColor = WCDark,
        topBar = {
            TopAppBar(
                title = { Text("Import Games", color = ChessGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.isImporting) {
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {

            // ── PGN file ───────────────────────────────────────────────────
            SectionLabel("PGN File")
            Button(
                onClick = { filePicker.launch(arrayOf("*/*")) },
                enabled = !state.isImporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.FileOpen, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Choose PGN file")
            }

            // ── Chess.com ──────────────────────────────────────────────────
            SectionLabel("Chess.com")
            OutlinedTextField(
                value = chessComUsername,
                onValueChange = { chessComUsername = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (chessComUsername.isNotBlank()) vm.importFromChessCom(chessComUsername)
                }),
            )
            Button(
                onClick = { vm.importFromChessCom(chessComUsername) },
                enabled = !state.isImporting && chessComUsername.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Import from Chess.com") }

            // ── Lichess ────────────────────────────────────────────────────
            SectionLabel("Lichess")
            OutlinedTextField(
                value = lichessUsername,
                onValueChange = { lichessUsername = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (lichessUsername.isNotBlank()) vm.importFromLichess(lichessUsername)
                }),
            )
            Button(
                onClick = { vm.importFromLichess(lichessUsername) },
                enabled = !state.isImporting && lichessUsername.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Import from Lichess") }

            // ── Progress / results ─────────────────────────────────────────
            if (state.isImporting) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.currentGame, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    if (state.totalGames > 0) {
                        LinearProgressIndicator(
                            progress = { state.importedCount.toFloat() / state.totalGames },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("${state.importedCount} / ${state.totalGames} games")
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }

            if (state.done) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = CorrectGreen, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${state.importedCount} games imported (${state.skippedCount} skipped)",
                        color = CorrectGreen,
                    )
                }
                Button(onClick = vm::reset, modifier = Modifier.fillMaxWidth()) { Text("Import more") }
            }

            val err = state.error
            if (err != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ErrorOutline, null, tint = WrongRed, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(err, color = WrongRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = ChessGold, fontWeight = FontWeight.SemiBold)
}
