package com.atakolstudio.sure.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SureDarkColorScheme = darkColorScheme(
    primary = SurePrimaryDark,
    onPrimary = SureOnPrimary,
    secondary = SureSecondary,
    error = SureError,
    background = SureBackgroundDark,
    onBackground = SureOnBackgroundDark,
    surface = SureSurfaceDark,
    onSurface = SureOnBackgroundDark,
    surfaceVariant = SureCardDark
)

private val SureLightColorScheme = lightColorScheme(
    primary = SurePrimary,
    onPrimary = SureOnPrimary,
    secondary = SureSecondary,
    error = SureError,
    background = SureBackgroundLight,
    onBackground = SureOnBackgroundLight,
    surface = SureSurfaceLight,
    onSurface = SureOnBackgroundLight,
    surfaceVariant = SureCardLight
)

@Composable
fun SureTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Marka renklerini korumak için varsayılan kapalı
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> SureDarkColorScheme
        else -> SureLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SureTypography,
        content = content
    )
}
