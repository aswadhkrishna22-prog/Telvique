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

// Velvet Ink — Light
private val AppleLightPalette = TelviqueApplePalette(
    isDark = false,
    background = Color(0xFFFFFFFF),
    groupedBackground = Color(0xFFF5F3FA),
    cardSurface = Color(0xFFFFFFFF),
    secondaryFill = Color(0xFFECE9F4),
    tertiaryFill = Color(0xFFF5F3FA),
    label = Color(0xFF0E0C14),
    secondaryLabel = Color(0xFF7A7589),
    tertiaryLabel = Color(0xFFA9A4B8),
    separator = Color(0xFFD9D5E3),
    accentBlue = Color(0xFF5B3FE0),
    callGreen = Color(0xFF2FBF71),
    callRed = Color(0xFFF2475A),
    keypadKeyFill = Color(0xFFECE9F4),
)

// Velvet Ink — Dark
private val AppleDarkPalette = TelviqueApplePalette(
    isDark = true,
    background = Color(0xFF000000),
    groupedBackground = Color(0xFF000000),
    cardSurface = Color(0xFF15141A),
    secondaryFill = Color(0xFF211F29),
    tertiaryFill = Color(0xFF2B2835),
    label = Color(0xFFF5F3FA),
    secondaryLabel = Color(0xFF9A95A8),
    tertiaryLabel = Color(0xFF6B667A),
    separator = Color(0xFF2E2B38),
    accentBlue = Color(0xFF8E7BFF),
    callGreen = Color(0xFF34D27F),
    callRed = Color(0xFFFF5A6B),
    keypadKeyFill = Color(0xFF211F29),
)

val LocalApplePalette = staticCompositionLocalOf { AppleLightPalette }

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B3FE0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FF),
    onPrimaryContainer = Color(0xFF1A0066),
    secondary = Color(0xFF7A7589),
    onSecondary = Color.White,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0E0C14),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E0C14),
    surfaceVariant = Color(0xFFF5F3FA),
    onSurfaceVariant = Color(0xFF7A7589),
    outlineVariant = Color(0xFFD9D5E3),
    error = Color(0xFFF2475A),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8E7BFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D2DB8),
    onPrimaryContainer = Color(0xFFE5E0FF),
    secondary = Color(0xFF9A95A8),
    onSecondary = Color.Black,
    background = Color(0xFF000000),
    onBackground = Color(0xFFF5F3FA),
    surface = Color(0xFF15141A),
    onSurface = Color(0xFFF5F3FA),
    surfaceVariant = Color(0xFF211F29),
    onSurfaceVariant = Color(0xFF9A95A8),
    outlineVariant = Color(0xFF2E2B38),
    error = Color(0xFFFF5A6B),
    onError = Color.White
)

object TelviqueColors {
    val AppleBlue = Color(0xFF5B3FE0)      // light accent (Velvet Ink violet)
    val AppleBlueDark = Color(0xFF8E7BFF)  // dark accent
    val CallGreen = Color(0xFF2FBF71)
    val CallRed = Color(0xFFF2475A)
    val MissedRed = Color(0xFFF2475A)
    val IncomingBlue = Color(0xFF5B3FE0)
    val OutgoingGreen = Color(0xFF2FBF71)
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
            typography = VelvetInkTypography,
            content = content
        )
    }
}
