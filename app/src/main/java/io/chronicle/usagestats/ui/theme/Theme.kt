package io.chronicle.usagestats.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.chronicle.usagestats.domain.model.AccentColorPreset
import io.chronicle.usagestats.domain.model.ThemeMode

private fun createDarkColorScheme(accent: AccentColorPreset): ColorScheme {
    val primary = Color(accent.primaryColorLong)
    val secondary = Color(accent.secondaryColorLong)
    val tertiary = Color(accent.tertiaryColorLong)

    return darkColorScheme(
        primary = primary,
        onPrimary = Color.Black,
        primaryContainer = primary.copy(alpha = 0.2f),
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = secondary.copy(alpha = 0.2f),
        onSecondaryContainer = secondary,
        tertiary = tertiary,
        background = BrandDarkBg,
        onBackground = DarkTextPrimary,
        surface = DarkSurface,
        onSurface = DarkTextPrimary,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkTextSecondary,
        surfaceContainer = DarkSurfaceContainer,
        outline = DarkOutline,
        outlineVariant = DarkOutline.copy(alpha = 0.5f)
    )
}

private fun createAmoledColorScheme(accent: AccentColorPreset): ColorScheme {
    val primary = Color(accent.primaryColorLong)
    val secondary = Color(accent.secondaryColorLong)
    val tertiary = Color(accent.tertiaryColorLong)

    return darkColorScheme(
        primary = primary,
        onPrimary = Color.Black,
        primaryContainer = primary.copy(alpha = 0.15f),
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = secondary.copy(alpha = 0.15f),
        onSecondaryContainer = secondary,
        tertiary = tertiary,
        background = BrandAmoledBg,
        onBackground = Color.White,
        surface = AmoledSurface,
        onSurface = Color.White,
        surfaceVariant = AmoledSurfaceVariant,
        onSurfaceVariant = DarkTextSecondary,
        surfaceContainer = AmoledSurface,
        outline = AmoledOutline,
        outlineVariant = AmoledOutline.copy(alpha = 0.4f)
    )
}

private fun createLightColorScheme(accent: AccentColorPreset): ColorScheme {
    val primary = Color(accent.secondaryColorLong)
    val secondary = Color(accent.primaryColorLong)
    val tertiary = Color(accent.tertiaryColorLong)

    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary.copy(alpha = 0.12f),
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = secondary.copy(alpha = 0.12f),
        onSecondaryContainer = secondary,
        tertiary = tertiary,
        background = BrandLightBg,
        onBackground = LightTextPrimary,
        surface = LightSurface,
        onSurface = LightTextPrimary,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightTextSecondary,
        surfaceContainer = LightSurfaceContainer,
        outline = LightOutline,
        outlineVariant = LightOutline.copy(alpha = 0.5f)
    )
}

@Composable
fun ChronicleTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentColor: AccentColorPreset = AccentColorPreset.SAPPHIRE,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> createLightColorScheme(accentColor)
        ThemeMode.DARK -> createDarkColorScheme(accentColor)
        ThemeMode.AMOLED -> createAmoledColorScheme(accentColor)
        ThemeMode.DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (systemDark) createDarkColorScheme(accentColor) else createLightColorScheme(accentColor)
            }
        }
    }

    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.DYNAMIC -> systemDark
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
