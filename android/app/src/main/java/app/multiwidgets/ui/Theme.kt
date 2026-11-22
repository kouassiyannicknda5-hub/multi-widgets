package app.multiwidgets.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/* Palette officielle multi-widgets 3.0 : bleu/violet du visuel. */
private val Clair = lightColorScheme(
    primary = Color(0xFF3D6DFF),
    secondary = Color(0xFF7A3BFF),
    background = Color(0xFFE8EEFF),
    surface = Color(0xFFFFFFFF),
)
private val Sombre = darkColorScheme(
    primary = Color(0xFF4F8CFF),
    secondary = Color(0xFF8A5CFF),
    background = Color(0xFF101540),
    surface = Color(0xFF1B2350),
)

/** Thèmes clair et sombre, suivis du réglage système. */
@Composable
fun MultiWidgetsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Sombre else Clair,
        content = content,
    )
}
