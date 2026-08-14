package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class IosButtonStyle {
    PRIMARY,
    SECONDARY,
    OUTLINED,
    DESTRUCTIVE
}

@Composable
fun IosButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IosButtonStyle = IosButtonStyle.PRIMARY,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String = "ios_button"
) {
    val backgroundColor = when (style) {
        IosButtonStyle.PRIMARY -> if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        IosButtonStyle.SECONDARY -> MaterialTheme.colorScheme.surfaceVariant
        IosButtonStyle.OUTLINED -> Color.Transparent
        IosButtonStyle.DESTRUCTIVE -> Color(0xFFFF3B30)
    }

    val textColor = when (style) {
        IosButtonStyle.PRIMARY -> Color.White
        IosButtonStyle.SECONDARY -> MaterialTheme.colorScheme.primary
        IosButtonStyle.OUTLINED -> MaterialTheme.colorScheme.primary
        IosButtonStyle.DESTRUCTIVE -> Color.White
    }

    val borderModifier = if (style == IosButtonStyle.OUTLINED) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(borderModifier)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            )
        }
    }
}
