package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.acepero13.android.gamereviewer.BuildConfig
import com.acepero13.chess.core.ui.board.EXTERNAL_THEMES
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors
import com.acepero13.chess.core.ui.theme.boardColorsForTheme
import org.koin.androidx.compose.koinViewModel

// ── Board theme options ───────────────────────────────────────────────────────
private val BOARD_THEMES = listOf("Classic", "Green", "Blue", "Walnut", "Midnight", "Coral", "Stone", "Wood", "Slate", "Citrine", "Ocean", "Sahara", "Crimson", "Pearl")

// ── Piece style list: "Classic" (Unicode) + all EXTERNAL_THEMES ──────────────
private val ALL_PIECE_STYLES: List<String> = listOf("Classic") + EXTERNAL_THEMES

// Danger-zone color — subdued red that works on dark and light backgrounds
private val DangerRed = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = koinViewModel(),
) {
    val appColors            = LocalAppColors.current
    val boardTheme           by vm.boardTheme.collectAsState()
    val pieceStyle           by vm.pieceStyle.collectAsState()
    val themeMode            by vm.themeMode.collectAsState()
    val clearDone            by vm.clearDone.collectAsState()
    val savedUsername        by vm.username.collectAsState()
    val positionCoachEnabled by vm.positionCoachEnabled.collectAsState()
    val developerModeEnabled by vm.developerModeEnabled.collectAsState()
    var usernameInput by remember(savedUsername) { mutableStateOf(savedUsername) }

    // Piece style search state
    var pieceSearch by remember { mutableStateOf("") }
    val filteredStyles = remember(pieceSearch) {
        if (pieceSearch.isBlank()) ALL_PIECE_STYLES
        else ALL_PIECE_STYLES.filter { it.contains(pieceSearch, ignoreCase = true) }
    }
    val pieceListState = rememberLazyListState()
    LaunchedEffect(pieceStyle, filteredStyles) {
        val idx = filteredStyles.indexOf(pieceStyle)
        if (idx >= 0) pieceListState.animateScrollToItem(idx)
    }

    // Confirmation dialog state
    var showClearDialog by remember { mutableStateOf(false) }

    // Snackbar for post-clear feedback
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(clearDone) {
        if (clearDone) {
            snackbar.showSnackbar("All imported games have been removed.")
            vm.onClearDoneConsumed()
        }
    }

    // ── Confirmation dialog ───────────────────────────────────────────────────
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(28.dp),
                )
            },
            title = {
                Text(
                    "Remove all imported games?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    "This will permanently delete all imported games, engine analysis, " +
                        "and critical-moment data. Your board annotations (arrows & comments) " +
                        "will be kept and reattached if you re-import the same games.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.clearAllGames()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = DangerRed),
                ) {
                    Text("Remove all", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        containerColor = appColors.background,
        snackbarHost   = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings", color = ChessGold, fontWeight = FontWeight.Bold)
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {

            // ── Player Identity ───────────────────────────────────────────────
            SectionHeader("Player Identity")
            Spacer(Modifier.height(8.dp))
            Text(
                "Your Chess.com / Lichess username — used to auto-orient the board " +
                    "so you always play from the bottom.",
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textSecondary,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value         = usernameInput,
                onValueChange = { usernameInput = it },
                label         = { Text("Your username") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(8.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = ChessGold,
                    unfocusedBorderColor = appColors.border,
                    focusedLabelColor    = ChessGold,
                    unfocusedLabelColor  = appColors.textSecondary,
                    focusedTextColor     = appColors.textPrimary,
                    unfocusedTextColor   = appColors.textPrimary,
                    cursorColor          = ChessGold,
                ),
                trailingIcon = if (usernameInput != savedUsername) ({
                    TextButton(onClick = { vm.setUsername(usernameInput) }) {
                        Text("Save", color = ChessGold)
                    }
                }) else null,
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = appColors.border)
            Spacer(Modifier.height(24.dp))

            // ── App Theme ─────────────────────────────────────────────────────
            SectionHeader("App Theme")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("dark" to "Dark", "light" to "Light", "system" to "System").forEach { (value, label) ->
                    ThemeModeChip(
                        label    = label,
                        selected = themeMode == value,
                        onClick  = { vm.setThemeMode(value) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = appColors.border)
            Spacer(Modifier.height(24.dp))

            // ── Board Theme ───────────────────────────────────────────────────
            SectionHeader("Board Theme")
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BOARD_THEMES.forEach { theme ->
                    BoardThemeSwatch(
                        themeName = theme,
                        selected  = boardTheme == theme,
                        onClick   = { vm.setBoardTheme(theme) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = appColors.border)
            Spacer(Modifier.height(24.dp))

            // ── Piece Style ───────────────────────────────────────────────────
            SectionHeader("Piece Style")
            Text(
                text  = "Current: $pieceStyle",
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textSecondary,
            )
            Spacer(Modifier.height(10.dp))

            // Search field
            OutlinedTextField(
                value         = pieceSearch,
                onValueChange = { pieceSearch = it },
                placeholder   = { Text("Search styles…", color = appColors.textTertiary) },
                leadingIcon   = {
                    Icon(Icons.Outlined.Search, null, tint = appColors.iconSubtle)
                },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = ChessGold,
                    unfocusedBorderColor = appColors.border,
                    focusedTextColor     = appColors.textPrimary,
                    unfocusedTextColor   = appColors.textPrimary,
                    cursorColor          = ChessGold,
                ),
                shape = RoundedCornerShape(10.dp),
            )
            Spacer(Modifier.height(10.dp))

            // Style list — lazy so only visible rows are composed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, appColors.border, RoundedCornerShape(8.dp)),
            ) {
                LazyColumn(state = pieceListState) {
                    items(filteredStyles, key = { it }) { style ->
                        PieceStyleRow(
                            style    = style,
                            selected = pieceStyle == style,
                            onClick  = { vm.setPieceStyle(style) },
                        )
                        HorizontalDivider(
                            color    = appColors.border.copy(alpha = 0.4f),
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            HorizontalDivider(color = appColors.border)
            Spacer(Modifier.height(24.dp))

            // ── Coaching ──────────────────────────────────────────────────────
            SectionHeader("Coaching")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Structured Analysis Prompts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = appColors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "At flagged positions in Navigate mode, show a checklist of position-specific questions to guide your analysis before you continue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.textSecondary,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked  = positionCoachEnabled,
                    onCheckedChange = vm::setPositionCoachEnabled,
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = appColors.border)
            Spacer(Modifier.height(24.dp))

            // ── Developer Options ─────────────────────────────────────────────
            SectionHeader("Developer Options")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Coach Accuracy Debug",
                        style = MaterialTheme.typography.bodyMedium,
                        color = appColors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Shows a \"Copy LLM Prompt\" button next to active coaching panels so you can paste the position context into an LLM to verify coaching accuracy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.textSecondary,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked         = developerModeEnabled,
                    onCheckedChange = vm::setDeveloperModeEnabled,
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = appColors.border)
            Spacer(Modifier.height(24.dp))

            // ── Danger Zone ───────────────────────────────────────────────────
            SectionHeader("Data")
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Remove all imported games and their analysis from the device.",
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textSecondary,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Remove all imported games", fontWeight = FontWeight.SemiBold)
            }

            // Bottom padding so last item isn't clipped by the nav bar
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = appColors.textTertiary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.titleSmall,
        color    = ChessGold,
        fontWeight = FontWeight.SemiBold,
    )
}

// ── App-theme toggle chip ─────────────────────────────────────────────────────

@Composable
private fun ThemeModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val appColors = LocalAppColors.current
    FilterChip(
        selected = selected,
        onClick  = onClick,
        label    = { Text(label) },
        leadingIcon = if (selected) ({
            Icon(
                Icons.Outlined.Check,
                null,
                modifier = Modifier.size(16.dp),
            )
        }) else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor   = ChessGold.copy(alpha = 0.25f),
            selectedLabelColor       = ChessGold,
            selectedLeadingIconColor = ChessGold,
            containerColor           = appColors.surfaceVariant,
            labelColor               = appColors.textSecondary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled             = true,
            selected            = selected,
            selectedBorderColor = ChessGold,
            borderColor         = appColors.border,
        ),
    )
}

// ── Piece style row ───────────────────────────────────────────────────────────

@Composable
private fun PieceStyleRow(style: String, selected: Boolean, onClick: () -> Unit) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) ChessGold.copy(alpha = 0.10f) else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF769656)),
            contentAlignment = Alignment.Center,
        ) {
            if (style == "Classic") {
                Text(text = "♘", fontSize = 28.sp, color = Color.White)
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("file:///android_asset/pieces/$style/nw.svg")
                        .decoderFactory(SvgDecoder.Factory())
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text       = style.replace("_", " "),
            style      = MaterialTheme.typography.bodyMedium,
            color      = if (selected) ChessGold else appColors.textPrimary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier   = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector        = Icons.Outlined.Check,
                contentDescription = null,
                tint               = ChessGold,
                modifier           = Modifier.size(20.dp),
            )
        }
    }
}

// ── Board color swatch ────────────────────────────────────────────────────────

@Composable
private fun BoardThemeSwatch(
    themeName: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = boardColorsForTheme(themeName)
    val swatchSize = 72.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(swatchSize)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) ChessGold else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                ),
        ) {
            // Mini 2×2 checkered preview
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(colors.light))
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(colors.dark))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(colors.dark))
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(colors.light))
                }
            }
            // Checkmark overlay when selected
            if (selected) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(ChessGold),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text  = themeName,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) ChessGold else LocalAppColors.current.textSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
