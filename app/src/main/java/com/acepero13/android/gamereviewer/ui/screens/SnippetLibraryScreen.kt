package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acepero13.android.gamereviewer.data.model.Snippet
import com.acepero13.android.gamereviewer.data.model.parsedTags
import com.acepero13.chess.core.ui.board.BoardState
import com.acepero13.chess.core.ui.board.ChessBoard
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetLibraryScreen(
    onBack:          () -> Unit,
    onOpenSnippet:   (Long) -> Unit,
    vm:              SnippetLibraryViewModel = koinViewModel(),
) {
    val state     by vm.uiState.collectAsState()
    val appColors = LocalAppColors.current

    var snippetToDelete by remember { mutableStateOf<Snippet?>(null) }
    var snippetToManage by remember { mutableStateOf<Snippet?>(null) }

    snippetToManage?.let { snippet ->
        SnippetManageSheet(
            snippet   = snippet,
            onDismiss = { snippetToManage = null },
            onSave    = { title, tags, notes -> vm.updateSnippet(snippet, title, tags, notes) },
        )
    }

    snippetToDelete?.let { snippet ->
        AlertDialog(
            onDismissRequest = { snippetToDelete = null },
            title            = { Text("Delete bookmark?", color = appColors.textPrimary) },
            text             = { Text("\"${snippet.title}\" will be permanently removed.", color = appColors.textSecondary) },
            confirmButton    = {
                TextButton(onClick = { vm.deleteSnippet(snippet); snippetToDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton    = {
                TextButton(onClick = { snippetToDelete = null }) {
                    Text("Cancel", color = appColors.textSecondary)
                }
            },
            containerColor   = appColors.surface,
        )
    }

    Scaffold(
        containerColor = appColors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text       = "Snippet Library",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = ChessGold,
                        )
                        Text(
                            text  = "${state.snippets.size} bookmarks",
                            style = MaterialTheme.typography.bodySmall,
                            color = appColors.textSecondary,
                        )
                    }
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
                .padding(padding),
        ) {
            if (state.allTags.isNotEmpty()) {
                TagFilterRow(
                    tags        = state.allTags,
                    selectedTag = state.selectedTag,
                    onSelectTag = vm::selectTag,
                )
            }
            if (state.snippets.isEmpty()) {
                EmptySnippetsState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.snippets, key = { it.id }) { snippet ->
                        SnippetCard(
                            snippet  = snippet,
                            onClick  = { onOpenSnippet(snippet.id) },
                            onDelete = { snippetToDelete = snippet },
                            onManage = { snippetToManage = snippet },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SnippetCard(
    snippet:  Snippet,
    onClick:  () -> Unit,
    onDelete: () -> Unit,
    onManage: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val tags      = snippet.parsedTags()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = appColors.surface),
        shape    = RoundedCornerShape(10.dp),
        border   = BorderStroke(1.dp, appColors.border),
        onClick  = onClick,
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(6.dp)),
            ) {
                ChessBoard(
                    boardState    = BoardState(fen = snippet.fen),
                    onSquareTap   = {},
                    modifier      = Modifier.fillMaxSize(),
                    thumbnailMode = true,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text       = snippet.title,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = appColors.textPrimary,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick  = onManage,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Edit,
                            contentDescription = "Manage",
                            tint               = ChessGold.copy(alpha = 0.7f),
                            modifier           = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick  = onDelete,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint               = appColors.textTertiary,
                            modifier           = Modifier.size(18.dp),
                        )
                    }
                }
                if (snippet.notes.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text     = snippet.notes,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = appColors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Bottom,
                ) {
                    if (tags.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            tags.take(3).forEach { tag ->
                                TagPill(tag)
                            }
                        }
                    }
                    Text(
                        text  = formatDate(snippet.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.textTertiary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagFilterRow(
    tags:        List<String>,
    selectedTag: String?,
    onSelectTag: (String?) -> Unit,
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            FilterChip(
                selected = selectedTag == null,
                onClick  = { onSelectTag(null) },
                label    = { Text("All") },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor    = ChessGold,
                    selectedLabelColor        = androidx.compose.ui.graphics.Color.Black,
                ),
            )
        }
        items(tags) { tag ->
            FilterChip(
                selected = selectedTag == tag,
                onClick  = { onSelectTag(if (selectedTag == tag) null else tag) },
                label    = { Text(tag) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor    = ChessGold,
                    selectedLabelColor        = androidx.compose.ui.graphics.Color.Black,
                ),
            )
        }
    }
}

@Composable
private fun TagPill(tag: String) {
    val appColors = LocalAppColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text          = "#$tag",
            style         = MaterialTheme.typography.labelSmall,
            color         = ChessGold.copy(alpha = 0.8f),
            letterSpacing = 0.3.sp,
        )
    }
}

@Composable
private fun EmptySnippetsState(modifier: Modifier = Modifier) {
    val appColors = LocalAppColors.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = Icons.Outlined.Bookmark,
                contentDescription = null,
                tint               = ChessGold.copy(alpha = 0.3f),
                modifier           = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text       = "No bookmarks yet",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = appColors.textPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Tap the Bookmark button while reviewing\na game to save a position here.",
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

private fun formatDate(millis: Long): String {
    val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return fmt.format(Date(millis))
}
