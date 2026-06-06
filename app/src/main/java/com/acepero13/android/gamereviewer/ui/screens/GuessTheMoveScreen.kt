package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.automirrored.outlined.LastPage
import androidx.compose.material.icons.automirrored.outlined.NavigateBefore
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.outlined.FirstPage
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acepero13.android.gamereviewer.ui.components.ANNOTATION_COLORS
import com.acepero13.android.gamereviewer.ui.screens.analysis.BookmarkPositionSheet
import com.acepero13.chess.core.ui.board.ChessBoard
import com.acepero13.chess.core.ui.components.MoveTree
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.CorrectGreen
import com.acepero13.chess.core.ui.theme.LocalAppColors
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuessTheMoveScreen(
    onBack: () -> Unit,
    initialGameIndex: Int = -1,
    vm: GuessTheMoveViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsState()
    val appColors = LocalAppColors.current
    var showNotesDialog by remember { mutableStateOf(false) }
    val bookmarkSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(initialGameIndex) {
        if (initialGameIndex >= 0) vm.startWithGameAtIndex(initialGameIndex)
    }

    if (showNotesDialog) {
        NotesDialog(
            text         = state.currentUserComment,
            onTextChange = vm::updateUserComment,
            onDismiss    = { showNotesDialog = false },
        )
    }

    if (state.showBookmarkSheet) {
        BookmarkPositionSheet(
            sheetState = bookmarkSheetState,
            onSave     = { title, tags, notes -> vm.saveSnippet(title, tags, notes) },
            onDismiss  = vm::dismissBookmarkSheet,
        )
    }

    Scaffold(
        containerColor = appColors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "GUESS THE MOVE",
                            style         = MaterialTheme.typography.titleMedium,
                            fontWeight    = FontWeight.SemiBold,
                            color         = ChessGold,
                            letterSpacing = 1.sp,
                        )
                        when (state.phase) {
                            GuessTheMovePhase.GUESSING, GuessTheMovePhase.MOVE_REVEALED ->
                                Text(
                                    "${state.exactMatches} / ${state.totalPresented} matched · " +
                                        "Move ${state.currentMoveIndex + 1} / ${state.masterMoves.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appColors.textSecondary,
                                )
                            GuessTheMovePhase.REVIEWING ->
                                Text(
                                    "Reviewing · ${state.reviewIndex} / ${state.fenHistory.lastIndex}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appColors.textSecondary,
                                )
                            else -> Unit
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Outlined.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = appColors.textPrimary,
                        )
                    }
                },
                actions = {
                    if (state.phase == GuessTheMovePhase.MOVE_REVEALED ||
                        state.phase == GuessTheMovePhase.REVIEWING) {
                        IconButton(onClick = vm::openBookmarkSheet) {
                            Icon(
                                Icons.Outlined.Bookmark,
                                contentDescription = "Bookmark position",
                                tint = appColors.textSecondary,
                            )
                        }
                    }
                    if (state.phase == GuessTheMovePhase.MOVE_REVEALED) {
                        IconButton(onClick = { showNotesDialog = true }) {
                            Icon(
                                Icons.Outlined.RateReview,
                                contentDescription = "Notes",
                                tint = if (state.currentUserComment.isNotBlank()) ChessGold
                                       else appColors.textSecondary,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appColors.background),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (state.phase) {
                GuessTheMovePhase.CHOOSING_SIDE ->
                    SideSelectionContent(state = state, vm = vm)

                GuessTheMovePhase.SELECTING ->
                    GameSelectorContent(state = state, vm = vm)

                GuessTheMovePhase.LOADING ->
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ChessGold)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Loading game…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = appColors.textSecondary,
                            )
                        }
                    }

                GuessTheMovePhase.GUESSING, GuessTheMovePhase.MOVE_REVEALED ->
                    GuessingContent(state = state, vm = vm)

                GuessTheMovePhase.GAME_COMPLETE ->
                    SessionCompleteContent(state = state, vm = vm, onBack = onBack)

                GuessTheMovePhase.REVIEWING ->
                    ReviewContent(state = state, vm = vm)
            }
        }
    }
}

// ── Side selection ────────────────────────────────────────────────────────────

@Composable
private fun SideSelectionContent(
    state: GuessTheMoveUiState,
    vm:    GuessTheMoveViewModel,
) {
    val appColors = LocalAppColors.current

    Column(
        modifier              = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally,
    ) {
        Text(
            "Which side would you like to play?",
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color      = appColors.textPrimary,
            textAlign  = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Choose whose moves you will guess throughout the game.",
            style     = MaterialTheme.typography.bodySmall,
            color     = appColors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (state.gameDescription.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                state.gameDescription,
                style     = MaterialTheme.typography.labelSmall,
                color     = appColors.textTertiary,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(32.dp))

        val whiteLabel = if (state.whitePlayer.isNotBlank()) state.whitePlayer else "White"
        val blackLabel = if (state.blackPlayer.isNotBlank()) state.blackPlayer else "Black"

        listOf(
            Triple(GuessingSide.WHITE_ONLY, whiteLabel, "Guess every White move"),
            Triple(GuessingSide.BLACK_ONLY, blackLabel, "Guess every Black move"),
            Triple(GuessingSide.BOTH,       "Both sides", "Guess moves for both players"),
        ).forEach { (side, title, subtitle) ->
            val isSelected = state.selectedSide == side
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) ChessGold.copy(alpha = 0.12f) else appColors.surface,
                ),
                shape  = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) ChessGold else appColors.border,
                ),
                onClick = { vm.updateSelectedSide(side) },
            ) {
                Row(
                    modifier            = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier         = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) ChessGold else appColors.border),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint     = Color.Black,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    Column {
                        Text(
                            title,
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = if (isSelected) ChessGold else appColors.textPrimary,
                        )
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = appColors.textSecondary,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick  = { vm.confirmSide(state.selectedSide) },
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(8.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = ChessGold,
                contentColor   = Color.Black,
            ),
        ) {
            Text("Continue", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Game selector ─────────────────────────────────────────────────────────────

@Composable
private fun GameSelectorContent(
    state: GuessTheMoveUiState,
    vm:    GuessTheMoveViewModel,
) {
    val appColors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Study a grandmaster's game move by move. Make your decision before the master's move is revealed.",
            style = MaterialTheme.typography.bodySmall,
            color = appColors.textSecondary,
        )

        // ── Offline classics section ───────────────────────────────────────────
        SelectorSectionLabel("BUNDLED CLASSICS")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(
                containerColor = if (state.selectedSource is MasterGameSource.Offline)
                    ChessGold.copy(alpha = 0.10f) else appColors.surface
            ),
            shape  = RoundedCornerShape(10.dp),
            border = BorderStroke(
                1.dp,
                if (state.selectedSource is MasterGameSource.Offline) ChessGold else appColors.border
            ),
            onClick = { vm.selectSource(MasterGameSource.Offline) },
        ) {
            Row(
                modifier            = Modifier.padding(14.dp),
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Outlined.AutoFixHigh,
                    contentDescription = null,
                    tint   = ChessGold,
                    modifier = Modifier.size(22.dp),
                )
                Column {
                    Text(
                        "Play a Classic",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = appColors.textPrimary,
                    )
                    Text(
                        "Morphy, Kasparov, Fischer, Tal and more — no network required",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = appColors.textSecondary,
                        maxLines = 2,
                    )
                }
            }
        }

        // ── Famous GMs section ────────────────────────────────────────────────
        SelectorSectionLabel("GRANDMASTERS")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(FAMOUS_MASTERS) { master ->
                val isSelected = state.selectedSource == master
                FilterChip(
                    selected = isSelected,
                    onClick  = { vm.selectSource(master) },
                    label    = {
                        Text(
                            master.displayName,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ChessGold.copy(alpha = 0.15f),
                        selectedLabelColor     = ChessGold,
                    ),
                )
            }
        }

        // ── Custom username section ────────────────────────────────────────────
        SelectorSectionLabel("CUSTOM PLAYER")
        val isCustomSelected = state.selectedSource is MasterGameSource.OnlineCustom
        OutlinedTextField(
            value         = state.customUsername,
            onValueChange = {
                vm.updateCustomUsername(it)
                if (it.isNotBlank()) vm.selectSource(
                    MasterGameSource.OnlineCustom(it, state.selectedPlatform)
                )
            },
            label         = { Text("Username") },
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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {}),
        )
        // Platform toggle — only relevant for custom / online sources
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("lichess" to "Lichess", "chesscom" to "Chess.com").forEach { (value, label) ->
                FilterChip(
                    selected = state.selectedPlatform == value,
                    onClick  = {
                        vm.updateSelectedPlatform(value)
                        if (state.customUsername.isNotBlank()) {
                            vm.selectSource(MasterGameSource.OnlineCustom(state.customUsername, value))
                        }
                    },
                    label  = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ChessGold.copy(alpha = 0.15f),
                        selectedLabelColor     = ChessGold,
                    ),
                )
            }
        }

        // ── Error ──────────────────────────────────────────────────────────────
        AnimatedVisibility(visible = state.fetchError != null) {
            Text(
                state.fetchError ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        // ── Start button ───────────────────────────────────────────────────────
        Button(
            onClick  = { vm.startSession() },
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(8.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = ChessGold,
                contentColor   = Color.Black,
            ),
        ) {
            Text("Start Session", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Guessing board ────────────────────────────────────────────────────────────

@Composable
private fun GuessingContent(
    state: GuessTheMoveUiState,
    vm:    GuessTheMoveViewModel,
) {
    val appColors = LocalAppColors.current
    val engineArrows = if (state.engineArrow != null) listOf(state.engineArrow) else emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Game label
        Text(
            state.gameDescription,
            style    = MaterialTheme.typography.labelSmall,
            color    = appColors.textSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        // Board
        val displayBoardState = state.boardState.copy(
            isEditorMode = state.isEditorMode,
            arrows       = engineArrows,
        )
        ChessBoard(
            boardState     = displayBoardState,
            onSquareTap    = vm::onSquareTap,
            onArrowDrawn   = vm::onArrowDrawn,
            onSquareMarked = vm::onSquareMarked,
            modifier       = Modifier.fillMaxWidth(),
        )

        // Controls row: Skip (guessing only) + Edit toggle
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            if (state.phase == GuessTheMovePhase.GUESSING) {
                TextButton(onClick = vm::skipMove) {
                    Icon(
                        Icons.Outlined.SkipNext,
                        contentDescription = "Skip",
                        modifier = Modifier.size(16.dp),
                        tint     = appColors.textSecondary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Skip",
                        style = MaterialTheme.typography.labelMedium,
                        color = appColors.textSecondary,
                    )
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            OutlinedButton(
                onClick = vm::toggleEditorMode,
                border  = BorderStroke(
                    1.dp,
                    if (state.isEditorMode) ChessGold else appColors.border,
                ),
                colors  = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (state.isEditorMode) ChessGold.copy(alpha = 0.10f)
                                     else Color.Transparent,
                    contentColor   = if (state.isEditorMode) ChessGold else appColors.textSecondary,
                ),
                shape   = RoundedCornerShape(8.dp),
            ) {
                Icon(
                    Icons.Outlined.Brush,
                    contentDescription = "Edit",
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("Edit", style = MaterialTheme.typography.labelMedium)
            }
        }

        // Color picker — visible only when editor mode is active
        AnimatedVisibility(
            visible = state.isEditorMode,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut(),
        ) {
            Row(
                modifier              = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    "Color:",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.textSecondary,
                )
                ANNOTATION_COLORS.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(if (state.currentArrowColor == color) 22.dp else 18.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { vm.updateArrowColor(color) }
                    )
                }
            }
        }

        // Move tree breadcrumb (played moves so far — read-only during guessing)
        if (state.treeItems.isNotEmpty()) {
            MoveTree(
                entries     = state.treeItems,
                onNodeClick = { /* read-only during guessing */ },
                modifier    = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            )
        }

        // Preamble annotation card (game notes from the annotator)
        if (state.preambleAnnotation != null && !state.preambleDismissed) {
            PreambleAnnotationCard(
                text      = state.preambleAnnotation,
                onDismiss = vm::dismissPreamble,
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            )
        }

        // Opponent auto-move annotation (shown during GUESSING when opponent has an annotated reply)
        AnimatedVisibility(
            visible = state.phase == GuessTheMovePhase.GUESSING && state.opponentAnnotation != null,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut(),
        ) {
            if (state.opponentAnnotation != null) {
                OpponentAnnotationCard(
                    text     = state.opponentAnnotation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                )
            }
        }

        // Move reveal panel (shown only in MOVE_REVEALED)
        AnimatedVisibility(
            visible = state.phase == GuessTheMovePhase.MOVE_REVEALED,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut(),
        ) {
            MoveRevealPanel(state = state, vm = vm)
        }
    }
}

// ── Move reveal panel ─────────────────────────────────────────────────────────

@Composable
private fun MoveRevealPanel(
    state: GuessTheMoveUiState,
    vm:    GuessTheMoveViewModel,
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.surface),
        shape  = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, appColors.border),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ── Move comparison row ────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.weight(1f),
                ) {
                    Text(
                        "You played",
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.textSecondary,
                    )
                    Row(
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (state.wasExactMatch) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint     = CorrectGreen,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Text(
                            state.userMoveSan.ifBlank { "—" },
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = if (state.wasExactMatch) CorrectGreen else appColors.textPrimary,
                        )
                    }
                }

                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color    = appColors.border,
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.weight(1f),
                ) {
                    Text(
                        "Master played",
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.textSecondary,
                    )
                    Text(
                        state.masterMoveSan.ifBlank { "—" },
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = ChessGold,
                    )
                }
            }

            // ── Original PGN annotation ────────────────────────────────────────
            if (state.originalAnnotation != null) {
                HorizontalDivider(color = appColors.border)
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Outlined.RateReview,
                        contentDescription = null,
                        tint     = appColors.textTertiary,
                        modifier = Modifier.size(14.dp).padding(top = 2.dp),
                    )
                    Text(
                        state.originalAnnotation,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = appColors.textSecondary,
                    )
                }
            }

            HorizontalDivider(color = appColors.border)

            // ── Engine check (only shown if user's move differs from master) ───
            AnimatedVisibility(visible = !state.wasExactMatch) {
                Column {
                    TextButton(
                        onClick  = { vm.requestEngineAnalysis() },
                        enabled  = !state.engineThinking,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.engineThinking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color    = ChessGold,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (state.engineThinking) "Analysing…"
                            else if (state.engineArrow != null) "Engine: shown on board"
                            else "Check with engine",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (state.engineArrow != null) ChessGold else appColors.textSecondary,
                        )
                    }
                }
            }

            // ── Next button ────────────────────────────────────────────────────
            Button(
                onClick  = { vm.continueToNextMove() },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = ChessGold,
                    contentColor   = Color.Black,
                ),
            ) {
                Text(
                    if (state.currentMoveIndex + 1 >= state.masterMoves.size) "Finish"
                    else "Next →",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ── Session complete ──────────────────────────────────────────────────────────

@Composable
private fun SessionCompleteContent(
    state:  GuessTheMoveUiState,
    vm:     GuessTheMoveViewModel,
    onBack: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val percentage = if (state.totalPresented > 0)
        (state.exactMatches * 100) / state.totalPresented else 0

    Column(
        modifier              = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally,
    ) {
        Text(
            "${state.exactMatches} / ${state.totalPresented}",
            style      = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color      = ChessGold,
        )
        Text(
            "moves matched",
            style = MaterialTheme.typography.bodyMedium,
            color = appColors.textSecondary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "$percentage%",
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color      = appColors.textPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            state.gameDescription,
            style     = MaterialTheme.typography.bodySmall,
            color     = appColors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Text(
            state.sourceLabel,
            style = MaterialTheme.typography.labelSmall,
            color = appColors.textTertiary,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick  = { vm.startReview() },
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(8.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = ChessGold,
                contentColor   = Color.Black,
            ),
        ) {
            Icon(
                Icons.Outlined.RateReview,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Review Game", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick  = { vm.restartSelection() },
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(8.dp),
            border   = BorderStroke(1.dp, appColors.border),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = appColors.textPrimary),
        ) {
            Text("Play Another Game", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick  = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Back to Home",
                style = MaterialTheme.typography.bodyMedium,
                color = appColors.textSecondary,
            )
        }
    }
}

// ── Review ────────────────────────────────────────────────────────────────────

@Composable
private fun ReviewContent(
    state: GuessTheMoveUiState,
    vm:    GuessTheMoveViewModel,
) {
    val appColors  = LocalAppColors.current
    val totalSteps = state.fenHistory.lastIndex.coerceAtLeast(1)
    val idx        = state.reviewIndex

    val moveLabel = when {
        idx <= 0 -> "Starting position"
        else -> {
            val san      = state.masterSanHistory.getOrElse(idx - 1) { "?" }
            val moveNum  = (idx + 1) / 2
            val isWhite  = idx % 2 == 1
            if (isWhite) "$moveNum. $san" else "$moveNum… $san"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            state.gameDescription,
            style    = MaterialTheme.typography.labelSmall,
            color    = appColors.textSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        ChessBoard(
            boardState     = state.boardState.copy(isEditorMode = false),
            onSquareTap    = {},
            onArrowDrawn   = { _, _ -> },
            onSquareMarked = {},
            modifier       = Modifier.fillMaxWidth(),
        )

        // ── Move breadcrumb tree ──────────────────────────────────────────────
        MoveTree(
            entries     = state.treeItems,
            onNodeClick = vm::onTreeNodeClick,
            modifier    = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )

        // ── Move label ────────────────────────────────────────────────────────
        Text(
            moveLabel,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = appColors.textPrimary,
            modifier   = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            textAlign  = TextAlign.Center,
        )

        // ── Scrub slider ──────────────────────────────────────────────────────
        Slider(
            value         = idx.toFloat(),
            onValueChange = { vm.reviewGoTo(it.toInt()) },
            valueRange    = 0f..totalSteps.toFloat(),
            steps         = (totalSteps - 1).coerceAtLeast(0),
            modifier      = Modifier.padding(horizontal = 16.dp),
            colors        = SliderDefaults.colors(
                thumbColor                = ChessGold,
                activeTrackColor          = ChessGold,
                inactiveTrackColor        = appColors.border,
            ),
        )

        // ── Button row ────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.reviewGoTo(0) }, enabled = idx > 0) {
                Icon(
                    Icons.Outlined.FirstPage,
                    contentDescription = "First position",
                    tint = if (idx > 0) appColors.textPrimary else appColors.border,
                )
            }
            IconButton(onClick = { vm.reviewGoTo(idx - 1) }, enabled = idx > 0) {
                Icon(
                    Icons.AutoMirrored.Outlined.NavigateBefore,
                    contentDescription = "Previous move",
                    tint = if (idx > 0) appColors.textPrimary else appColors.border,
                    modifier = Modifier.size(32.dp),
                )
            }
            IconButton(onClick = { vm.reviewGoTo(idx + 1) }, enabled = idx < totalSteps) {
                Icon(
                    Icons.AutoMirrored.Outlined.NavigateNext,
                    contentDescription = "Next move",
                    tint = if (idx < totalSteps) appColors.textPrimary else appColors.border,
                    modifier = Modifier.size(32.dp),
                )
            }
            IconButton(onClick = { vm.reviewGoTo(totalSteps) }, enabled = idx < totalSteps) {
                Icon(
                    Icons.AutoMirrored.Outlined.LastPage,
                    contentDescription = "Last position",
                    tint = if (idx < totalSteps) appColors.textPrimary else appColors.border,
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color    = appColors.border,
        )

        TextButton(
            onClick  = vm::exitReview,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                "Back to Results",
                style = MaterialTheme.typography.bodyMedium,
                color = appColors.textSecondary,
            )
        }
    }
}

// ── Preamble annotation card ──────────────────────────────────────────────────

@Composable
private fun PreambleAnnotationCard(
    text:      String,
    onDismiss: () -> Unit,
    modifier:  Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }
    var overflows by remember { mutableStateOf(false) }
    androidx.compose.material3.Card(
        modifier = modifier,
        colors   = androidx.compose.material3.CardDefaults.cardColors(containerColor = appColors.surface),
        shape    = RoundedCornerShape(8.dp),
        border   = BorderStroke(1.dp, appColors.border.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    "Game Notes",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = ChessGold,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    onClick  = onDismiss,
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Dismiss",
                        tint     = appColors.textTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text,
                style        = MaterialTheme.typography.bodySmall,
                color        = appColors.textSecondary,
                maxLines     = if (expanded) Int.MAX_VALUE else 3,
                overflow     = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
            )
            if (overflows || expanded) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        if (expanded) "Show less" else "Show more",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChessGold,
                    )
                }
            }
        }
    }
}

// ── Opponent auto-move annotation card ───────────────────────────────────────

@Composable
private fun OpponentAnnotationCard(text: String, modifier: Modifier = Modifier) {
    val appColors = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }
    var overflows by remember { mutableStateOf(false) }
    androidx.compose.material3.Card(
        modifier = modifier,
        colors   = androidx.compose.material3.CardDefaults.cardColors(containerColor = appColors.surface),
        shape    = RoundedCornerShape(8.dp),
        border   = BorderStroke(1.dp, appColors.border.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment      = Alignment.Top,
                horizontalArrangement  = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Outlined.RateReview,
                    contentDescription = null,
                    tint     = appColors.textTertiary,
                    modifier = Modifier.size(14.dp).padding(top = 2.dp),
                )
                Text(
                    text,
                    style        = MaterialTheme.typography.bodySmall,
                    color        = appColors.textSecondary,
                    maxLines     = if (expanded) Int.MAX_VALUE else 3,
                    overflow     = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
                )
            }
            if (overflows || expanded) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        if (expanded) "Show less" else "Show more",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChessGold,
                    )
                }
            }
        }
    }
}

// ── Notes dialog ──────────────────────────────────────────────────────────────

@Composable
private fun NotesDialog(
    text:         String,
    onTextChange: (String) -> Unit,
    onDismiss:    () -> Unit,
) {
    val appColors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Your thoughts",
                style = MaterialTheme.typography.titleMedium,
                color = appColors.textPrimary,
            )
        },
        text = {
            OutlinedTextField(
                value         = text,
                onValueChange = onTextChange,
                placeholder   = {
                    Text(
                        "Your thoughts on this move…",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(8.dp),
                minLines  = 3,
                maxLines  = 6,
                colors    = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = ChessGold,
                    unfocusedBorderColor = appColors.border,
                    focusedTextColor     = appColors.textPrimary,
                    unfocusedTextColor   = appColors.textPrimary,
                    cursorColor          = ChessGold,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = ChessGold, fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor    = appColors.surface,
        titleContentColor = appColors.textPrimary,
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun SelectorSectionLabel(text: String) {
    val appColors = LocalAppColors.current
    Text(
        text          = text,
        style         = MaterialTheme.typography.labelSmall,
        color         = appColors.textTertiary,
        letterSpacing = 1.5.sp,
    )
}
