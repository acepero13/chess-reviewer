package com.acepero13.android.gamereviewer.ui.screens.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors

internal val BtnActive     = ChessGold
internal val BtnSelectedBg = Color(0x1FC9A84C)

@Composable
internal fun BottomBarButton(
    icon:            ImageVector,
    label:           String,
    onClick:         () -> Unit,
    enabled:         Boolean = true,
    selected:        Boolean = false,
    onDisabledClick: (() -> Unit)? = null,
) {
    val appColors = LocalAppColors.current
    val color = when {
        !enabled -> appColors.textDisabled
        selected -> BtnActive
        else     -> appColors.iconSubtle
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) BtnSelectedBg else Color.Transparent)
            .then(when {
                enabled                 -> Modifier.clickable(onClick = onClick)
                onDisabledClick != null -> Modifier.clickable(onClick = onDisabledClick)
                else                    -> Modifier
            })
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
