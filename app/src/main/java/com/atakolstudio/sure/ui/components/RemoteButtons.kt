package com.atakolstudio.sure.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable

/**
 * Uzaktan kumandadaki tüm ikon tabanlı tuşlar için temel, yeniden kullanılabilir buton.
 * Basıldığında hafif küçülme (scale) animasyonu + Material ripple efekti uygular.
 */
@Composable
fun RemoteIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    shape: androidx.compose.ui.graphics.Shape = CircleShape
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.90f else 1f, label = "buttonScale")

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(shape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true, color = iconTint),
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint)
    }
}

/** Metin tabanlı tuşlar (renkli tuşlar, sayı tuş takımı) için varyant. */
@Composable
fun RemoteTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(14.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.90f else 1f, label = "buttonScale")

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(shape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true, color = textColor),
                onClick = onClick
            )
            .semantics { this.contentDescription = text },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = textColor, fontWeight = FontWeight.SemiBold)
    }
}
