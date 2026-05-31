package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StoryBg     = Color(0xFF181400)
private val StoryBorder = Color(0xFF3D3000)
private val StoryText   = Color(0xFFD4B870)

@Composable
fun GameStoryCard(
    story:     String,
    visible:   Boolean,
    onDismiss: () -> Unit,
    modifier:  Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible && story.isNotBlank(),
        enter   = fadeIn() + expandVertically(),
        exit    = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        Card(
            modifier = Modifier.border(1.dp, StoryBorder, RoundedCornerShape(8.dp)),
            shape    = RoundedCornerShape(8.dp),
            colors   = CardDefaults.cardColors(containerColor = StoryBg),
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text     = "📖  ",
                    style    = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 1.dp),
                )
                Text(
                    text     = story,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = StoryText,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick  = onDismiss,
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Dismiss",
                        tint     = StoryText.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}
