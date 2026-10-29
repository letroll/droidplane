package fr.julien.quievreux.droidplane2.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun getPlatformColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
): ColorScheme {
    return if (darkTheme) darkScheme else lightScheme
}
