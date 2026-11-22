package app.multiwidgets.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Clair = lightColorScheme(
    primary = Color(0xFFB35400),
    secondary = Color(0xFF1C2B3A),
    background = Color(0xFFE9EDF1),
    surface = Color(0xFFFFFFFF),
)
private val Sombre = darkColorScheme(
    primary = Color(0xFFFF7A1A),
    secondary = Color(0xFF3B2A4D),
    background = Color(0xFF10151B),
    surface = Color(0xFF1B2128),
)

/** Thèmes clair et sombre, suivis du réglage système. */
@Composable
fun MultiWidgetsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Sombre else Clair,
        content = content,
    )
}
