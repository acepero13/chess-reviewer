package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.CorrectGreen
import com.acepero13.chess.core.ui.theme.LocalAppColors
import com.acepero13.chess.core.ui.theme.WrongRed
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onOpenGameList:  () -> Unit,
    onOpenImport:    () -> Unit,
    onOpenDashboard: () -> Unit = {},
    onOpenSettings:  () -> Unit = {},
    onOpenDebrief:   () -> Unit = {},
    onOpenAnalysis:  (Long) -> Unit = {},
    vm: HomeViewModel = koinViewModel(),
) {
    val gameCount        by vm.gameCount.collectAsState()
    val hasRecentSession by vm.hasRecentSession.collectAsState()
    val recentGames      by vm.recentGames.collectAsState()
    val appColors = LocalAppColors.current

    Scaffold(containerColor = appColors.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            item {
                HomeHeader(
                    modifier = Modifier.padding(
                        start  = 20.dp,
                        end    = 20.dp,
                        top    = 28.dp,
                        bottom = 20.dp,
                    ),
                )
            }

            // ── Session debrief banner ─────────────────────────────────────────
            if (hasRecentSession) {
                item {
                    DebriefBannerCard(
                        onClick  = onOpenDebrief,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── Next to review / onboarding ────────────────────────────────────
            item {
                if (recentGames.isNotEmpty()) {
                    NextToReviewCard(
                        game        = recentGames.first(),
                        totalGames  = gameCount,
                        onAnalyze   = { onOpenAnalysis(recentGames.first().id) },
                        onBrowseAll = onOpenGameList,
                        modifier    = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                    )
                } else {
                    OnboardingCard(
                        onImport = onOpenImport,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Recent games carousel ──────────────────────────────────────────
            if (recentGames.isNotEmpty()) {
                item {
                    Row(
                        modifier              = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        SectionLabel("RECENT GAMES")
                        Text(
                            text     = "All →",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = ChessGold,
                            modifier = Modifier.clickable(onClick = onOpenGameList),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(recentGames, key = { it.id }) { game ->
                            RecentGameCard(
                                game    = game,
                                onClick = { onOpenAnalysis(game.id) },
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            // ── Quick actions ──────────────────────────────────────────────────
            item {
                SectionLabel(
                    text     = "ACTIONS",
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier              = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ActionCard(
                        category = "IMPORT",
                        title    = "Add Games",
                        subtitle = "PGN · Chess.com · Lichess",
                        icon     = Icons.Outlined.FileOpen,
                        onClick  = onOpenImport,
                        modifier = Modifier.weight(1f),
                    )
                    ActionCard(
                        category = "ANALYSIS",
                        title    = "Dashboard",
                        subtitle = "Cognitive patterns",
                        icon     = Icons.Outlined.Insights,
                        onClick  = onOpenDashboard,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                SettingsCard(
                    onClick  = onOpenSettings,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(modifier: Modifier = Modifier) {
    val appColors = LocalAppColors.current
    Row(
        modifier          = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text  = "♟",
            style = MaterialTheme.typography.headlineMedium,
            color = ChessGold,
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text          = "GAME REVIEWER",
                style         = MaterialTheme.typography.titleLarge,
                fontWeight    = FontWeight.Black,
                color         = appColors.textPrimary,
                letterSpacing = 3.sp,
            )
            Text(
                text          = "ANALYSE  ·  REFLECT  ·  IMPROVE",
                style         = MaterialTheme.typography.labelSmall,
                color         = appColors.textSecondary,
                letterSpacing = 1.sp,
            )
        }
    }
}

// ── Next to review card ───────────────────────────────────────────────────────

@Composable
private fun NextToReviewCard(
    game:        ReviewGame,
    totalGames:  Int,
    onAnalyze:   () -> Unit,
    onBrowseAll: () -> Unit,
    modifier:    Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = appColors.surface),
        shape    = RoundedCornerShape(12.dp),
        border   = BorderStroke(1.dp, ChessGold.copy(alpha = 0.25f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Outlined.SportsEsports,
                    contentDescription = null,
                    tint               = ChessGold,
                    modifier           = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text          = "NEXT TO REVIEW",
                    style         = MaterialTheme.typography.labelSmall,
                    color         = ChessGold,
                    letterSpacing = 1.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text       = "${game.whitePlayer} vs ${game.blackPlayer}",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = appColors.textPrimary,
            )
            val detail = buildString {
                if (game.openingName.isNotBlank()) append(game.openingName)
                val cleanDate = game.date.trim()
                if (cleanDate.isNotBlank() && !cleanDate.startsWith("?")) {
                    if (isNotEmpty()) append("  ·  ")
                    append(cleanDate.replace('.', '/'))
                }
                if (totalGames > 1) {
                    if (isNotEmpty()) append("  ·  ")
                    append("$totalGames games")
                }
            }
            if (detail.isNotBlank()) {
                Text(
                    text  = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = appColors.textSecondary,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick  = onAnalyze,
                    colors   = ButtonDefaults.buttonColors(containerColor = ChessGold),
                    shape    = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text       = "Analyze Now",
                        color      = Color.Black,
                        fontWeight = FontWeight.Bold,
                        style      = MaterialTheme.typography.labelMedium,
                    )
                }
                OutlinedButton(
                    onClick  = onBrowseAll,
                    shape    = RoundedCornerShape(8.dp),
                    border   = BorderStroke(1.dp, ChessGold),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text       = "Browse All",
                        color      = ChessGold,
                        fontWeight = FontWeight.SemiBold,
                        style      = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

// ── Onboarding card (empty state) ─────────────────────────────────────────────

@Composable
private fun OnboardingCard(
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = appColors.surface),
        shape    = RoundedCornerShape(12.dp),
        border   = BorderStroke(1.dp, ChessGold.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier            = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text  = "♟",
                style = MaterialTheme.typography.displaySmall,
                color = ChessGold.copy(alpha = 0.35f),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text       = "No games yet",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = appColors.textPrimary,
                textAlign  = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text      = "Import a game from PGN, Chess.com, or Lichess to start your self-analysis journey.",
                style     = MaterialTheme.typography.bodySmall,
                color     = appColors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onImport,
                colors  = ButtonDefaults.buttonColors(containerColor = ChessGold),
                shape   = RoundedCornerShape(8.dp),
            ) {
                Icon(
                    imageVector        = Icons.Outlined.FileOpen,
                    contentDescription = null,
                    tint               = Color.Black,
                    modifier           = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = "Import Game",
                    color      = Color.Black,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ── Recent game carousel card ─────────────────────────────────────────────────

@Composable
private fun RecentGameCard(
    game:    ReviewGame,
    onClick: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val (resultLabel, resultColor) = when (game.result) {
        "1-0"     -> "W" to CorrectGreen
        "0-1"     -> "L" to WrongRed
        "1/2-1/2" -> "½" to ChessGold
        else      -> "?" to appColors.textTertiary
    }
    Card(
        modifier = Modifier.width(152.dp),
        colors   = CardDefaults.cardColors(containerColor = appColors.surface),
        shape    = RoundedCornerShape(10.dp),
        border   = BorderStroke(1.dp, appColors.border),
        onClick  = onClick,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(resultColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text       = resultLabel,
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color      = resultColor,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text       = game.whitePlayer.take(15),
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = appColors.textPrimary,
                maxLines   = 1,
            )
            Text(
                text  = "vs",
                style = MaterialTheme.typography.labelSmall,
                color = appColors.textTertiary,
            )
            Text(
                text       = game.blackPlayer.take(15),
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = appColors.textPrimary,
                maxLines   = 1,
            )
            if (game.openingName.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text     = game.openingName,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = appColors.textSecondary,
                    maxLines = 2,
                )
            }
            if (game.date.isNotBlank() && !game.date.startsWith("?")) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = game.date.replace('.', '/'),
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.textTertiary,
                )
            }
        }
    }
}

// ── Action card (2-column grid) ───────────────────────────────────────────────

@Composable
private fun ActionCard(
    category: String,
    title:    String,
    subtitle: String,
    icon:     ImageVector,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = modifier.height(118.dp),
        colors   = CardDefaults.cardColors(containerColor = appColors.surface),
        shape    = RoundedCornerShape(10.dp),
        border   = BorderStroke(1.dp, appColors.border),
        onClick  = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
        ) {
            Column {
                Text(
                    text          = category,
                    style         = MaterialTheme.typography.labelSmall,
                    color         = ChessGold,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = title,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = appColors.textPrimary,
                )
                Text(
                    text     = subtitle,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = appColors.textSecondary,
                    maxLines = 2,
                )
            }
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = appColors.cardDecorPiece,
                modifier           = Modifier
                    .align(Alignment.BottomEnd)
                    .size(34.dp),
            )
        }
    }
}

// ── Settings card ─────────────────────────────────────────────────────────────

@Composable
private fun SettingsCard(
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = appColors.surface),
        shape    = RoundedCornerShape(10.dp),
        border   = BorderStroke(1.dp, appColors.border),
        onClick  = onClick,
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector        = Icons.Outlined.Settings,
                contentDescription = null,
                tint               = appColors.iconSubtle,
                modifier           = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "Settings",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = appColors.textPrimary,
                )
                Text(
                    text  = "Board theme · Piece set · Appearance",
                    style = MaterialTheme.typography.bodySmall,
                    color = appColors.textSecondary,
                )
            }
            Text(
                text  = "›",
                style = MaterialTheme.typography.titleMedium,
                color = appColors.iconSubtle,
            )
        }
    }
}

// ── Debrief banner ────────────────────────────────────────────────────────────

@Composable
private fun DebriefBannerCard(
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = ChessGold.copy(alpha = 0.10f)),
        shape    = RoundedCornerShape(10.dp),
        border   = BorderStroke(1.dp, ChessGold.copy(alpha = 0.35f)),
        onClick  = onClick,
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector        = Icons.Outlined.Psychology,
                contentDescription = null,
                tint               = ChessGold,
                modifier           = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "Session Complete",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = ChessGold,
                )
                Text(
                    text  = "Tap to view your coaching debrief",
                    style = MaterialTheme.typography.bodySmall,
                    color = appColors.textSecondary,
                )
            }
            Text("→", color = ChessGold, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Section label ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(
    text:     String,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    Text(
        text          = text,
        style         = MaterialTheme.typography.labelSmall,
        color         = appColors.textTertiary,
        letterSpacing = 1.5.sp,
        modifier      = modifier,
    )
}
