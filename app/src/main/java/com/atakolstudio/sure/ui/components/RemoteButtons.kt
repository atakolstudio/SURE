package com.atakolstudio.sure.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Basılı tutunca tekrar başlamadan önceki bekleme süresi (gerçek kumandalardaki gibi). */
private const val REPEAT_INITIAL_DELAY_MS = 420L
/** Tekrar sırasında ardışık gönderimler arası süre. */
private const val REPEAT_INTERVAL_MS = 130L

/**
 * Uzaktan kumandadaki tüm ikon tabanlı tuşlar için temel, yeniden kullanılabilir buton.
 * Basıldığında hafif küçülme (scale) animasyonu + Material ripple + dokunsal geri
 * bildirim (haptic) uygular — gerçek bir fiziksel tuşa basıyormuş hissi verir.
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
    shape: Shape = CircleShape,
    /** Verilirse, düz [backgroundColor] yerine bu gradyan kullanılır (ör. güç butonu). */
    containerBrush: Brush? = null,
    /** true ise, tuş basılı tutulduğunda gerçek kumandalardaki gibi tekrar tekrar tetiklenir
     *  (ör. ses/kanal/sıcaklık artır-azalt tuşları için idealdir). */
    repeatable: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.90f else 1f, label = "buttonScale")
    val haptics = LocalHapticFeedback.current

    if (repeatable) {
        LaunchedEffect(isPressed) {
            if (isPressed) {
                delay(REPEAT_INITIAL_DELAY_MS)
                while (isPressed) {
                    onClick()
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    delay(REPEAT_INTERVAL_MS)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(shape)
            .then(
                if (containerBrush != null) Modifier.background(containerBrush)
                else Modifier.background(backgroundColor)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true, color = iconTint),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
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
    shape: Shape = RoundedCornerShape(14.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.90f else 1f, label = "buttonScale")
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(shape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true, color = textColor),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
            .semantics { this.contentDescription = text },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = textColor, fontWeight = FontWeight.SemiBold)
    }
}
