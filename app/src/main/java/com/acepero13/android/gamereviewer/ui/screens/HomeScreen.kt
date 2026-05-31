package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.WCDark
import org.koin.androidx.compose.koinViewModel

// ── Design tokens ────────────────────────────────────────────────────────────
private val CardSurface   = Color(0xFF1A1A1A)
private val CardBorder    = Color(0xFF2A2A2A)
private val TextPrimary   = Color(0xFFF0F0F0)
private val TextSecondary = Color(0xFF888888)
private val DividerColor  = Color(0xFF2A2A2A)

@Composable
fun HomeScreen(
    onOpenGameList:  () -> Unit,
    onOpenImport:    () -> Unit,
    onOpenDashboard: () -> Unit = {},
    onOpenSettings:  () -> Unit = {},
    onOpenDebrief:   () -> Unit = {},
    vm: HomeViewModel = koinViewModel(),
) {
    val gameCount        by vm.gameCount.collectAsState()
    val hasRecentSession by vm.hasRecentSession.collectAsState()

    Scaffold(containerColor = WCDark) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Hero title ────────────────────────────────────────────────────
            Text(
                text      = "♟",
                style     = MaterialTheme.typography.displaySmall,
                color     = ChessGold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text          = "GAME REVIEWER",
                style         = MaterialTheme.typography.titleLarge,
                fontWeight    = FontWeight.Black,
                color         = ChessGold,
                letterSpacing = 4.sp,
                textAlign     = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text          = "ANALYSE  ·  REFLECT  ·  IMPROVE",
                style         = MaterialTheme.typography.labelSmall,
                color         = TextSecondary,
                letterSpacing = 1.5.sp,
                textAlign     = TextAlign.Center,
            )

            Spacer(Modifier.height(48.dp))

            // ── Primary actions ───────────────────────────────────────────────
            HomeCard(
                title    = "My Games",
                subtitle = "$gameCount ${if (gameCount == 1) "game" else "games"} imported",
                icon     = Icons.Outlined.SportsEsports,
                onClick  = onOpenGameList,
            )
            Spacer(Modifier.height(10.dp))
            HomeCard(
                title    = "Import Games",
                subtitle = "PGN file · Chess.com · Lichess",
                icon     = Icons.Outlined.FileOpen,
                onClick  = onOpenImport,
            )
            Spacer(Modifier.height(10.dp))
            HomeCard(
                title    = "Cognitive Dashboard",
                subtitle = "Your behavioural failure patterns",
                icon     = Icons.Outlined.Insights,
                onClick  = onOpenDashboard,
            )

            AnimatedVisibility(
                visible = hasRecentSession,
                enter   = fadeIn() + expandVertically(),
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    DebriefBannerCard(onClick = onOpenDebrief)
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
            Spacer(Modifier.height(20.dp))

            HomeCard(
                title    = "Settings",
                subtitle = "Board theme · Piece set · Appearance",
                icon     = Icons.Outlined.Settings,
                onClick  = onOpenSettings,
            )
        }
    }
}

// ── Debrief banner ────────────────────────────────────────────────────────────

@Composable
private fun DebriefBannerCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF163B2A)),
        shape    = RoundedCornerShape(10.dp),
        border   = BorderStroke(1.dp, Color(0xFF2D6A4F)),
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
                    "Session Complete",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = ChessGold,
                )
                Text(
                    "Tap to view your coaching debrief",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCCE8D9).copy(alpha = 0.75f),
                )
            }
            Text("→", color = ChessGold, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Home card ─────────────────────────────────────────────────────────────────

@Composable
private fun HomeCard(
    title:    String,
    subtitle: String,
    icon:     ImageVector,
    onClick:  () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = CardSurface),
        shape    = RoundedCornerShape(10.dp),
        border   = BorderStroke(1.dp, CardBorder),
        onClick  = onClick,
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left gold accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ChessGold),
            )
            Spacer(Modifier.width(14.dp))
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = ChessGold,
                modifier           = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = title,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                )
                Text(
                    text  = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}
