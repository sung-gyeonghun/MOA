package kr.moa.app

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Purple: Color @Composable get() = MaterialTheme.colorScheme.primary
val Ink: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val Muted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val Lavender: Color @Composable get() = MaterialTheme.colorScheme.secondaryContainer
val Canvas: Color @Composable get() = MaterialTheme.colorScheme.background
val Pink: Color @Composable get() = MaterialTheme.colorScheme.tertiaryContainer
val MoaSurface: Color @Composable get() = MaterialTheme.colorScheme.surface

@Composable fun MoaTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) darkColorScheme(
        primary = Color(0xFFB8A4FF), onPrimary = Color(0xFF251944),
        background = Color(0xFF15121D), surface = Color(0xFF211C2C),
        onBackground = Color(0xFFF1EBFA), onSurface = Color(0xFFF1EBFA),
        onSurfaceVariant = Color(0xFFB9B0CA), secondaryContainer = Color(0xFF352A4A),
        tertiaryContainer = Color(0xFF5A3048), outline = Color(0xFF463C56)
    ) else lightColorScheme(
        primary = Color(0xFF7353E6), onPrimary = Color.White,
        background = Color(0xFFFCFAFF), surface = Color.White,
        onBackground = Color(0xFF29233B), onSurface = Color(0xFF29233B),
        onSurfaceVariant = Color(0xFF827B91), secondaryContainer = Color(0xFFF0EAFF),
        tertiaryContainer = Color(0xFFFFD8E5), outline = Color(0xFFE8E2F0)
    )
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(typography = moaTypography(), colorScheme = colors, content = content)
}
