package com.acepero13.android.gamereviewer.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.CorrectGreen
import com.acepero13.chess.core.ui.theme.WCDark
import com.acepero13.chess.core.ui.theme.WrongRed
import org.koin.androidx.compose.koinViewModel

// ── Design tokens ─────────────────────────────────────────────────────────────
private val CardSurface       = Color(0xFF1A1A1A)
private val CardBorder        = Color(0xFF2A2A2A)
private val FieldBorderFocused   = ChessGold
private val FieldBorderUnfocused = Color(0xFF3A3A3A)
private val TextPrimary       = Color(0xFFF0F0F0)
private val TextSecondary     = Color(0xFF888888)
private val ButtonText        = Color(0xFF1A1A1A)
private val SuccessBg         = Color(0xFF0D2A0D)
private val ErrorBg           = Color(0xFF2A0D0D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    vm: ImportViewModel = koinViewModel(),
) {
    val state   by vm.uiState.collectAsState()
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
                title = {
                    Text("Import Games", color = ChessGold, fontWeight = FontWeight.Bold)
                },
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
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

            // ── PGN file ───────────────────────────────────────────────────
            ImportSection(title = "PGN File", icon = Icons.Outlined.FileOpen) {
                GoldButton(
                    text     = "Choose PGN File",
                    onClick  = { filePicker.launch(arrayOf("*/*")) },
                    enabled  = !state.isImporting,
                    icon     = Icons.Outlined.FileOpen,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Chess.com ──────────────────────────────────────────────────
            ImportSection(title = "Chess.com", icon = Icons.Outlined.AccountCircle) {
                StyledTextField(
                    value         = chessComUsername,
                    onValueChange = { chessComUsername = it },
                    label         = "Username",
                    onImeAction   = {
                        if (chessComUsername.isNotBlank()) vm.importFromChessCom(chessComUsername)
                    },
                )
                GoldButton(
                    text     = "Import from Chess.com",
                    onClick  = { vm.importFromChessCom(chessComUsername) },
                    enabled  = !state.isImporting && chessComUsername.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Lichess ────────────────────────────────────────────────────
            ImportSection(title = "Lichess", icon = Icons.Outlined.AccountCircle) {
                StyledTextField(
                    value         = lichessUsername,
                    onValueChange = { lichessUsername = it },
                    label         = "Username",
                    onImeAction   = {
                        if (lichessUsername.isNotBlank()) vm.importFromLichess(lichessUsername)
                    },
                )
                GoldButton(
                    text     = "Import from Lichess",
                    onClick  = { vm.importFromLichess(lichessUsername) },
                    enabled  = !state.isImporting && lichessUsername.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Importing progress ─────────────────────────────────────────
            if (state.isImporting) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = CardSurface),
                    shape    = RoundedCornerShape(10.dp),
                    border   = BorderStroke(1.dp, ChessGold.copy(alpha = 0.3f)),
                ) {
                    Column(
                        modifier            = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (state.currentGame.isNotEmpty()) {
                            Text(
                                text  = state.currentGame,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        if (state.totalGames > 0) {
                            LinearProgressIndicator(
                                progress   = { state.importedCount.toFloat() / state.totalGames },
                                modifier   = Modifier.fillMaxWidth(),
                                color      = ChessGold,
                                trackColor = CardBorder,
                            )
                            Text(
                                text  = "${state.importedCount} / ${state.totalGames} games",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                            )
                        } else {
                            CircularProgressIndicator(
                                color       = ChessGold,
                                strokeWidth = 2.5.dp,
                                modifier    = Modifier.size(32.dp),
                            )
                        }
                    }
                }
            }

            // ── Done state ─────────────────────────────────────────────────
            if (state.done) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = SuccessBg),
                    shape    = RoundedCornerShape(10.dp),
                    border   = BorderStroke(1.dp, CorrectGreen.copy(alpha = 0.4f)),
                ) {
                    Column(
                        modifier            = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint     = CorrectGreen,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text       = "${state.importedCount} games imported (${state.skippedCount} skipped)",
                                color      = CorrectGreen,
                                fontWeight = FontWeight.SemiBold,
                                style      = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        GoldButton(
                            text     = "Import More",
                            onClick  = vm::reset,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // ── Error state ────────────────────────────────────────────────
            val err = state.error
            if (err != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = ErrorBg),
                    shape    = RoundedCornerShape(10.dp),
                    border   = BorderStroke(1.dp, WrongRed.copy(alpha = 0.4f)),
                ) {
                    Row(
                        modifier          = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint     = WrongRed,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text  = err,
                            color = WrongRed,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Reusable import section card ──────────────────────────────────────────────

@Composable
private fun ImportSection(
    title:   String,
    icon:    ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = CardSurface),
        shape    = RoundedCornerShape(12.dp),
        border   = BorderStroke(1.dp, CardBorder),
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Section header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = ChessGold,
                    modifier           = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = title,
                    style      = MaterialTheme.typography.titleSmall,
                    color      = ChessGold,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            content()
        }
    }
}

// ── Styled text field ─────────────────────────────────────────────────────────

@Composable
private fun StyledTextField(
    value:         String,
    onValueChange: (String) -> Unit,
    label:         String,
    imeAction:     ImeAction = ImeAction.Done,
    onImeAction:   () -> Unit = {},
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        singleLine    = true,
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(8.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = FieldBorderFocused,
            unfocusedBorderColor = FieldBorderUnfocused,
            focusedLabelColor    = ChessGold,
            unfocusedLabelColor  = TextSecondary,
            focusedTextColor     = TextPrimary,
            unfocusedTextColor   = TextPrimary,
            cursorColor          = ChessGold,
        ),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onImeAction() }),
    )
}

// ── Gold-themed primary button ────────────────────────────────────────────────

@Composable
private fun GoldButton(
    text:     String,
    onClick:  () -> Unit,
    modifier: Modifier  = Modifier,
    enabled:  Boolean   = true,
    icon:     ImageVector? = null,
) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = modifier,
        shape    = RoundedCornerShape(8.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = ChessGold,
            contentColor           = ButtonText,
            disabledContainerColor = Color(0xFF3A3A3A),
            disabledContentColor   = TextSecondary,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}
