package com.fazza.pdfeditor.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = FazzaBlue,
    onPrimary = CardLight,
    primaryContainer = FazzaBlueLight,
    onPrimaryContainer = FazzaBlueDark,
    secondary = FazzaAccent,
    onSecondary = CardLight,
    secondaryContainer = FazzaAccentDark,
    background = SurfaceLight,
    surface = CardLight,
    onBackground = AnnotationBlack,
    onSurface = AnnotationBlack,
    error = Error,
)

private val DarkColorScheme = darkColorScheme(
    primary = FazzaBlueLight,
    onPrimary = FazzaBlueDark,
    primaryContainer = FazzaBlue,
    onPrimaryContainer = FazzaBlueLight,
    secondary = FazzaAccent,
    onSecondary = FazzaAccentDark,
    background = SurfaceDark,
    surface = CardDark,
    onBackground = CardLight,
    onSurface = CardLight,
    error = Error,
)

@Composable
fun FazzaPDFEditorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
