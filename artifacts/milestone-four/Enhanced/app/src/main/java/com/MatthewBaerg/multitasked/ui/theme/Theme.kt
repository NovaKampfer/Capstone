package com.MatthewBaerg.multitasked.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightPrimary,
    onPrimaryContainer = Color.White,
    secondary = LightSecondary,
    tertiary = LightTertiary
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.Black,
    primaryContainer = DarkPrimary,
    onPrimaryContainer = Color.Black,
    secondary = DarkSecondary,
    tertiary = DarkTertiary
)

private val ForestColorScheme = darkColorScheme(
    primary = ForestPrimary,
    onPrimary = ForestBackground,
    primaryContainer = ForestPrimary,
    onPrimaryContainer = ForestBackground,
    secondary = ForestSecondary,
    tertiary = ForestTertiary,
    background = ForestBackground,
    onBackground = ForestOnBackground,
    surface = ForestCard,
    onSurface = ForestOnBackground,
    surfaceContainer = ForestCard,
    outline = ForestPrimary
)

private val OceanColorScheme = darkColorScheme(
    primary = OceanPrimary,
    onPrimary = OceanBackground,
    primaryContainer = OceanPrimary,
    onPrimaryContainer = OceanBackground,
    secondary = OceanSecondary,
    tertiary = OceanTertiary,
    background = OceanBackground,
    onBackground = OceanOnBackground,
    surface = OceanCard,
    onSurface = OceanOnBackground,
    surfaceContainer = OceanCard,
    outline = OceanPrimary
)

private val SunsetColorScheme = darkColorScheme(
    primary = SunsetPrimary,
    onPrimary = SunsetBackground,
    primaryContainer = SunsetPrimary,
    onPrimaryContainer = SunsetBackground,
    secondary = SunsetSecondary,
    tertiary = SunsetTertiary,
    background = SunsetBackground,
    onBackground = SunsetOnBackground,
    surface = SunsetCard,
    onSurface = SunsetOnBackground,
    surfaceContainer = SunsetCard,
    outline = SunsetPrimary
)


// AppTheme enum to define available themes
enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK,
    OCEAN,
    FOREST,
    SUNSET
}

@Composable
fun MultiTaskedTheme(
    theme: AppTheme,
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        AppTheme.SYSTEM -> {
            val context = LocalContext.current
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isSystemInDarkTheme()) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            } else {
                if (isSystemInDarkTheme()) DarkColorScheme
                else LightColorScheme
            }
        }
        AppTheme.LIGHT -> LightColorScheme
        AppTheme.DARK -> DarkColorScheme
        AppTheme.OCEAN -> OceanColorScheme
        AppTheme.FOREST -> ForestColorScheme
        AppTheme.SUNSET -> SunsetColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
