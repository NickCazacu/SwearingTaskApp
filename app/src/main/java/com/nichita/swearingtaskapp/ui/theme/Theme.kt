package com.nichita.swearingtaskapp.ui.theme

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

private val Flame = Color(0xFFD84315)
private val FlameDark = Color(0xFFFFB59D)
private val Ember = Color(0xFF8D4E2A)
private val EmberDark = Color(0xFFFFB68E)

private val LightColors = lightColorScheme(
    primary = Flame,
    onPrimary = Color.White,
    secondary = Ember,
    onSecondary = Color.White,
    background = Color(0xFFFFF8F5),
    surface = Color(0xFFFFF8F5),
)

private val DarkColors = darkColorScheme(
    primary = FlameDark,
    onPrimary = Color(0xFF5F1500),
    secondary = EmberDark,
    onSecondary = Color(0xFF512300),
    background = Color(0xFF1A1110),
    surface = Color(0xFF1A1110),
)

@Composable
fun RoastTodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
