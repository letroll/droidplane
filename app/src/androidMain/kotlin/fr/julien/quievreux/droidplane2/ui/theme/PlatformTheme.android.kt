package fr.julien.quievreux.droidplane2.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

fun isContrastAvailable(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
}

@Composable
fun selectSchemeForContrast(isDark: Boolean): ColorScheme {
    val context = LocalContext.current
    var colorScheme = if (isDark) darkScheme else lightScheme
    val isPreview = LocalInspectionMode.current
    if (!isPreview && isContrastAvailable()) {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val contrastLevel = uiModeManager.contrast

        colorScheme = when (contrastLevel) {
            in 0.0f..0.33f -> if (isDark) darkScheme else lightScheme
            in 0.34f..0.66f -> if (isDark) mediumContrastDarkColorScheme else mediumContrastLightColorScheme
            in 0.67f..1.0f -> if (isDark) highContrastDarkColorScheme else highContrastLightColorScheme
            else -> if (isDark) darkScheme else lightScheme
        }
        return colorScheme
    } else {
        return colorScheme
    }
}

@Composable
actual fun getPlatformColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
): ColorScheme {
    return when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> selectSchemeForContrast(darkTheme)
    }
}
