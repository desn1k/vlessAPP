package com.desn1k.vlessapp.ui.theme

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
import com.desn1k.vlessapp.prefs.ThemeMode

private val Blue = Color(0xFF3D7DF6)
private val BlueLight = Color(0xFFE8F0FE)

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = BlueLight,
    onPrimaryContainer = Blue,
    secondary = Color(0xFF5B6B82),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF1B1F24),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1F24),
    surfaceVariant = Color(0xFFEEF2F8),
    onSurfaceVariant = Color(0xFF555F6E),
    outline = Color(0xFFD7DEE8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4FF),
    background = Color(0xFF15171B),
    surface = Color(0xFF1E2126),
)

@Composable
fun VlessAppTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // Material You: derive the palette from the device wallpaper on Android 12+, falling back
    // to the fixed brand palette on older devices where dynamic color isn't available.
    val context = LocalContext.current
    val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (useDark) DarkColors else LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
