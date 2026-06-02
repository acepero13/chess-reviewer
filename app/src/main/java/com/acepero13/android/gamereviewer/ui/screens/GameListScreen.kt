package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors
import com.acepero13.chess.core.ui.theme.WCDark
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListScreen(
    onBack: () -> Unit,
    onOpenAnalysis: (Long) -> Unit,
    vm: GameListViewModel = koinViewModel(),
) {
    val games    by vm.games.collectAsState()
    val filter   by vm.filter.collectAsState()
    val username by vm.username.collectAsState()
    val appColors = LocalAppColors.current

    var showFilters by remember { mutableStateOf(false) }
    val hasActiveFilters = filter.result != null || filter.source != null

    val snackbar = remember { SnackbarHostState() }
    val scope    = rememberCoroutineScope()

    Scaffold(
        containerColor = WCDark,
        snackbarHost   = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Games",
                        color = ChessGold,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, "Back", tint = ChessGold)
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            if (hasActiveFilters) Icons.Outlined.FilterList else Icons.Outlined.FilterList,
                            contentDescription = "Filters",
                            tint = if (hasActiveFilters) ChessGold else appColors.iconSubtle,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WCDark),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Search bar ────────────────────────────────────────────────────
            OutlinedTextField(
                value         = filter.query,
                onValueChange = vm::setQuery,
                placeholder   = { Text("Search by player, opening, event…", color = appColors.textTertiary) },
                leadingIcon   = {
                    Icon(Icons.Outlined.Search, null, tint = appColors.iconSubtle)
                },
                trailingIcon  = if (filter.query.isNotEmpty()) ({
                    IconButton(onClick = { vm.setQuery("") }) {
                        Icon(Icons.Outlined.Close, "Clear", tint = appColors.iconSubtle, modifier = Modifier.size(18.dp))
                    }
                }) else null,
                singleLine    = true,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = ChessGold,
                    unfocusedBorderColor = appColors.border,
                    focusedTextColor     = appColors.textPrimary,
                    unfocusedTextColor   = appColors.textPrimary,
                    cursorColor          = ChessGold,
                ),
                shape = RoundedCornerShape(10.dp),
            )

            // ── Expandable filter panel ───────────────────────────────────────
            AnimatedVisibility(visible = showFilters) {
                FilterPanel(
                    filter    = filter,
                    onResult  = { vm.setResult(it) },
                    onSource  = { vm.setSource(it) },
                    onClear   = { vm.clearFilters(); showFilters = false },
                    modifier  = Modifier.padding(horizontal = 16.dp),
                )
            }

            if (showFilters) HorizontalDivider(
                color = appColors.border,
                modifier = Modifier.padding(top = 8.dp),
            )

            // ── Game count subtitle ───────────────────────────────────────────
            if (games.isNotEmpty()) {
                Text(
                    text = "${games.size} ${if (games.size == 1) "game" else "games"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.textTertiary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }

            // ── List / empty state ────────────────────────────────────────────
            if (games.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (filter.query.isNotBlank() || hasActiveFilters)
                            "No games match your filters."
                        else
                            "No games imported yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(games, key = { it.id }) { game ->
                        GameRow(
                            game     = game,
                            username = username,
                            onClick  = { onOpenAnalysis(game.id) },
                            onDelete = {
                                scope.launch {
                                    val result = snackbar.showSnackbar(
                                        message     = "Game removed",
                                        actionLabel = "Undo",
                                        duration    = SnackbarDuration.Short,
                                    )
                                    if (result != SnackbarResult.ActionPerformed) {
                                        vm.deleteGame(game)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

// ── Filter panel ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterPanel(
    filter: GameFilter,
    onResult: (String?) -> Unit,
    onSource: (String?) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))

        // Result filters
        Text("Result", style = MaterialTheme.typography.labelSmall, color = appColors.textTertiary)
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(null to "All", "1-0" to "White wins", "0-1" to "Black wins", "1/2-1/2" to "Draw")
                .forEach { (value, label) ->
                    SmallFilterChip(
                        label    = label,
                        selected = filter.result == value,
                        onClick  = { onResult(value) },
                    )
                }
        }

        Spacer(Modifier.height(10.dp))

        // Source filters
        Text("Source", style = MaterialTheme.typography.labelSmall, color = appColors.textTertiary)
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(null to "All", "chesscom" to "Chess.com", "lichess" to "Lichess", "file" to "File")
                .forEach { (value, label) ->
                    SmallFilterChip(
                        label    = label,
                        selected = filter.source == value,
                        onClick  = { onSource(value) },
                    )
                }
        }

        // Clear button — only shown when something is active
        val hasActive = filter.result != null || filter.source != null
        if (hasActive) {
            Spacer(Modifier.height(8.dp))
            Text(
                text     = "Clear filters",
                style    = MaterialTheme.typography.labelSmall,
                color    = ChessGold,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onClear),
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SmallFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val appColors = LocalAppColors.current
    FilterChip(
        selected = selected,
        onClick  = onClick,
        label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = if (selected) ({
            Icon(Icons.Outlined.Check, null, modifier = Modifier.size(14.dp))
        }) else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor   = ChessGold.copy(alpha = 0.20f),
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

// ── Game row ──────────────────────────────────────────────────────────────────

private fun outcomeColor(game: ReviewGame, username: String): Color {
    val user = username.trim().lowercase()
    val white = game.whitePlayer.trim().lowercase()
    val black = game.blackPlayer.trim().lowercase()
    return when (game.result) {
        "1/2-1/2" -> Color(0xFFFFB300)
        "1-0" -> if (user.isNotEmpty() && user == black) Color(0xFFF44336)
                 else Color(0xFF4CAF50)
        "0-1" -> if (user.isNotEmpty() && user == white) Color(0xFFF44336)
                 else Color(0xFF4CAF50)
        else -> Color(0xFF9E9E9E)
    }
}

@Composable
private fun GameRow(game: ReviewGame, username: String, onClick: () -> Unit, onDelete: () -> Unit) {
    val stripColor = outcomeColor(game, username)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(stripColor),
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${game.whitePlayer} vs ${game.blackPlayer}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = buildString {
                            if (game.date.isNotEmpty()) append("${game.date}  ")
                            append(game.result)
                            if (game.openingName.isNotEmpty()) append("  ·  ${game.openingName}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (game.sourceType.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = game.sourceType.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = ChessGold.copy(alpha = 0.7f),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
