package com.telvique.phone.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF165DFC),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE5FF),
    onPrimaryContainer = Color(0xFF001946),
    secondary = Color(0xFF4F5E7B),
    onSecondary = Color.White,
    background = Color(0xFFF8F9FC),
    onBackground = Color(0xFF12151C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF12151C),
    surfaceVariant = Color(0xFFEEF1F7),
    onSurfaceVariant = Color(0xFF485163),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6B9BFF),
    onPrimary = Color(0xFF002D73),
    primaryContainer = Color(0xFF0E41A8),
    onPrimaryContainer = Color(0xFFDCE5FF),
    secondary = Color(0xFFB5C3DE),
    onSecondary = Color(0xFF212E47),
    background = Color(0xFF0B0E14),
    onBackground = Color(0xFFE9EDF5),
    surface = Color(0xFF131822),
    onSurface = Color(0xFFE9EDF5),
    surfaceVariant = Color(0xFF1F2634),
    onSurfaceVariant = Color(0xFFA8B3C7),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

object TelviqueColors {
    val CallGreen = Color(0xFF16A34A)
    val CallRed = Color(0xFFDC2626)
    val MissedRed = Color(0xFFEF4444)
    val IncomingBlue = Color(0xFF2563EB)
    val OutgoingGreen = Color(0xFF059669)
}

@Composable
fun TelviqueTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
