package com.telvique.phone.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class TelviqueApplePalette(
    val isDark: Boolean,
    val background: Color,
    val groupedBackground: Color,
    val cardSurface: Color,
    val secondaryFill: Color,
    val tertiaryFill: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val separator: Color,
    val accentBlue: Color,
    val callGreen: Color,
    val callRed: Color,
    val keypadKeyFill: Color,
)

private val AppleLightPalette = TelviqueApplePalette(
    isDark = false,
    background = Color(0xFFFFFFFF),
    groupedBackground = Color(0xFFF2F2F7),
    cardSurface = Color(0xFFFFFFFF),
    secondaryFill = Color(0xFFE9E9EE),
    tertiaryFill = Color(0xFFF2F2F7),
    label = Color(0xFF000000),
    secondaryLabel = Color(0xFF8E8E93),
    tertiaryLabel = Color(0xFFAEAEB2),
    separator = Color(0xFFC6C6C8),
    accentBlue = Color(0xFF007AFF),
    callGreen = Color(0xFF34C759),
    callRed = Color(0xFFFF3B30),
    keypadKeyFill = Color(0xFFE5E5EA),
)

private val AppleDarkPalette = TelviqueApplePalette(
    isDark = true,
    background = Color(0xFF000000),
    groupedBackground = Color(0xFF000000),
    cardSurface = Color(0xFF1C1C1E),
    secondaryFill = Color(0xFF2C2C2E),
    tertiaryFill = Color(0xFF3A3A3C),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0xFF8E8E93),
    tertiaryLabel = Color(0xFF636366),
    separator = Color(0xFF38383A),
    accentBlue = Color(0xFF0A84FF),
    callGreen = Color(0xFF30D158),
    callRed = Color(0xFFFF453A),
    keypadKeyFill = Color(0xFF2C2C2E),
)

val LocalApplePalette = staticCompositionLocalOf { AppleLightPalette }

private val LightColors = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F0FF),
    onPrimaryContainer = Color(0xFF003875),
    secondary = Color(0xFF8E8E93),
    onSecondary = Color.White,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF6C6C70),
    outlineVariant = Color(0xFFC6C6C8),
    error = Color(0xFFFF3B30),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0A3B7A),
    onPrimaryContainer = Color(0xFFD6E8FF),
    secondary = Color(0xFF8E8E93),
    onSecondary = Color.Black,
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF38383A),
    error = Color(0xFFFF453A),
    onError = Color.White
)

object TelviqueColors {
    val AppleBlue = Color(0xFF007AFF)
    val AppleBlueDark = Color(0xFF0A84FF)
    val CallGreen = Color(0xFF34C759)
    val CallRed = Color(0xFFFF3B30)
    val MissedRed = Color(0xFFFF3B30)
    val IncomingBlue = Color(0xFF007AFF)
    val OutgoingGreen = Color(0xFF34C759)
    val FavoriteGold = Color(0xFFFFCC00)
}

@Composable
fun TelviqueTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    forceDarkSystemBars: Boolean? = null,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) AppleDarkPalette else AppleLightPalette
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val useLightIcons = !(forceDarkSystemBars ?: darkTheme)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = useLightIcons
                isAppearanceLightNavigationBars = useLightIcons
            }
        }
    }

    CompositionLocalProvider(LocalApplePalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
