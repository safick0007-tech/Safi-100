package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.domain.model.StudioThemePreset

@Immutable
data class StudioPalette(
    val isDark: Boolean,
    val canvas: Color,
    val surface: Color,
    val card: Color,
    val elevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val emerald: Color,
    val emeraldSoft: Color,
    val liquid: Color,
    val liquidSoft: Color,
    val classic: Color,
    val classicSoft: Color,
    val ultra: Color,
    val ultraSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val codeBg: Color,
    val codeHeader: Color
)

private val ObsidianEmeraldPalette = StudioPalette(
    isDark = true,
    canvas = ObsidianCanvas,
    surface = ObsidianSurface,
    card = ObsidianCard,
    elevated = ObsidianElevated,
    border = ObsidianBorder,
    textPrimary = ObsidianInkPrimary,
    textSecondary = ObsidianInkSecondary,
    textMuted = ObsidianInkMuted,
    emerald = EmeraldPrimaryDark,
    emeraldSoft = EmeraldContainerDark,
    liquid = CyanSecondaryDark,
    liquidSoft = CyanContainerDark,
    classic = VioletTertiaryDark,
    classicSoft = VioletContainerDark,
    ultra = AmberUltraDark,
    ultraSoft = AmberContainerDark,
    danger = CoralDangerDark,
    dangerSoft = CoralContainerDark,
    codeBg = CodeSurfaceObsidian,
    codeHeader = CodeHeaderObsidian
)

private val MidnightSlatePalette = StudioPalette(
    isDark = true,
    canvas = MidnightCanvas,
    surface = MidnightSurface,
    card = MidnightCard,
    elevated = MidnightElevated,
    border = MidnightBorder,
    textPrimary = MidnightInkPrimary,
    textSecondary = MidnightInkSecondary,
    textMuted = MidnightInkMuted,
    emerald = EmeraldPrimaryDark,
    emeraldSoft = EmeraldContainerDark,
    liquid = CyanSecondaryDark,
    liquidSoft = CyanContainerDark,
    classic = VioletTertiaryDark,
    classicSoft = VioletContainerDark,
    ultra = AmberUltraDark,
    ultraSoft = AmberContainerDark,
    danger = CoralDangerDark,
    dangerSoft = CoralContainerDark,
    codeBg = CodeSurfaceMidnight,
    codeHeader = CodeHeaderMidnight
)

private val CrispPaperPalette = StudioPalette(
    isDark = false,
    canvas = PaperCanvas,
    surface = PaperSurface,
    card = PaperCard,
    elevated = PaperElevated,
    border = PaperBorder,
    textPrimary = PaperInkPrimary,
    textSecondary = PaperInkSecondary,
    textMuted = PaperInkMuted,
    emerald = EmeraldPrimaryLight,
    emeraldSoft = EmeraldContainerLight,
    liquid = CyanSecondaryLight,
    liquidSoft = CyanContainerLight,
    classic = VioletTertiaryLight,
    classicSoft = VioletContainerLight,
    ultra = AmberUltraLight,
    ultraSoft = AmberContainerLight,
    danger = CoralDangerLight,
    dangerSoft = CoralContainerLight,
    codeBg = CodeSurfaceLight,
    codeHeader = CodeHeaderLight
)

val LocalStudioPalette = staticCompositionLocalOf { ObsidianEmeraldPalette }

private val ObsidianDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldOnContainerDark,
    inversePrimary = EmeraldPrimaryLight,
    secondary = CyanSecondaryDark,
    onSecondary = CyanOnSecondaryDark,
    secondaryContainer = CyanContainerDark,
    onSecondaryContainer = CyanOnContainerDark,
    tertiary = VioletTertiaryDark,
    onTertiary = VioletOnTertiaryDark,
    tertiaryContainer = VioletContainerDark,
    onTertiaryContainer = VioletOnContainerDark,
    background = ObsidianCanvas,
    onBackground = ObsidianInkPrimary,
    surface = ObsidianSurface,
    onSurface = ObsidianInkPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = ObsidianInkSecondary,
    surfaceTint = EmeraldPrimaryDark,
    inverseSurface = PaperSurface,
    inverseOnSurface = PaperInkPrimary,
    surfaceDim = ObsidianSurfaceDim,
    surfaceBright = ObsidianElevated,
    surfaceContainerLowest = ObsidianCanvas,
    surfaceContainerLow = ObsidianSurfaceLow,
    surfaceContainer = ObsidianSurface,
    surfaceContainerHigh = ObsidianCard,
    surfaceContainerHighest = ObsidianSurfaceHighest,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorderSubtle,
    error = CoralDangerDark,
    onError = CoralOnErrorDark,
    errorContainer = CoralContainerDark,
    onErrorContainer = CoralOnContainerDark,
    scrim = Color(0xB3000000)
)

private val MidnightDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldOnContainerDark,
    inversePrimary = EmeraldPrimaryLight,
    secondary = CyanSecondaryDark,
    onSecondary = CyanOnSecondaryDark,
    secondaryContainer = CyanContainerDark,
    onSecondaryContainer = CyanOnContainerDark,
    tertiary = VioletTertiaryDark,
    onTertiary = VioletOnTertiaryDark,
    tertiaryContainer = VioletContainerDark,
    onTertiaryContainer = VioletOnContainerDark,
    background = MidnightCanvas,
    onBackground = MidnightInkPrimary,
    surface = MidnightSurface,
    onSurface = MidnightInkPrimary,
    surfaceVariant = MidnightCard,
    onSurfaceVariant = MidnightInkSecondary,
    surfaceTint = EmeraldPrimaryDark,
    inverseSurface = PaperSurface,
    inverseOnSurface = PaperInkPrimary,
    surfaceDim = MidnightSurfaceDim,
    surfaceBright = MidnightElevated,
    surfaceContainerLowest = MidnightCanvas,
    surfaceContainerLow = MidnightSurfaceLow,
    surfaceContainer = MidnightSurface,
    surfaceContainerHigh = MidnightCard,
    surfaceContainerHighest = MidnightSurfaceHighest,
    outline = MidnightBorder,
    outlineVariant = MidnightBorderSubtle,
    error = CoralDangerDark,
    onError = CoralOnErrorDark,
    errorContainer = CoralContainerDark,
    onErrorContainer = CoralOnContainerDark,
    scrim = Color(0xB3000000)
)

private val CrispLightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = EmeraldOnPrimaryLight,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = EmeraldOnContainerLight,
    inversePrimary = EmeraldPrimaryDark,
    secondary = CyanSecondaryLight,
    onSecondary = CyanOnSecondaryLight,
    secondaryContainer = CyanContainerLight,
    onSecondaryContainer = CyanOnContainerLight,
    tertiary = VioletTertiaryLight,
    onTertiary = VioletOnTertiaryLight,
    tertiaryContainer = VioletContainerLight,
    onTertiaryContainer = VioletOnContainerLight,
    background = PaperCanvas,
    onBackground = PaperInkPrimary,
    surface = PaperSurface,
    onSurface = PaperInkPrimary,
    surfaceVariant = PaperCard,
    onSurfaceVariant = PaperInkSecondary,
    surfaceTint = EmeraldPrimaryLight,
    inverseSurface = ObsidianSurface,
    inverseOnSurface = ObsidianInkPrimary,
    surfaceDim = PaperSurfaceDim,
    surfaceBright = PaperSurface,
    surfaceContainerLowest = PaperSurface,
    surfaceContainerLow = PaperSurfaceLow,
    surfaceContainer = PaperCanvas,
    surfaceContainerHigh = PaperCard,
    surfaceContainerHighest = PaperSurfaceHighest,
    outline = PaperBorder,
    outlineVariant = PaperBorderSubtle,
    error = CoralDangerLight,
    onError = CoralOnErrorLight,
    errorContainer = CoralContainerLight,
    onErrorContainer = CoralOnContainerLight,
    scrim = Color(0x66000000)
)

val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

/**
 * Convenience accessor for GodMode Studio's Material 3 theme tokens and semantic brand palette.
 */
object GodModeTheme {
    val palette: StudioPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalStudioPalette.current

    val colorScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes
}

/**
 * Returns the 4 brand swatch preview colors (canvas, card, primary, secondary) for a theme preset.
 */
fun previewSwatchesFor(preset: StudioThemePreset): List<Color> = when (preset) {
    StudioThemePreset.OBSIDIAN_EMERALD -> listOf(ObsidianCanvas, ObsidianCard, EmeraldPrimaryDark, CyanSecondaryDark)
    StudioThemePreset.MIDNIGHT_NORD -> listOf(MidnightCanvas, MidnightCard, EmeraldPrimaryDark, VioletTertiaryDark)
    StudioThemePreset.CRISP_PAPER -> listOf(PaperCanvas, PaperSurface, EmeraldPrimaryLight, CyanSecondaryLight)
}

@Composable
fun GodModeStudioTheme(
    themePreset: StudioThemePreset = StudioThemePreset.OBSIDIAN_EMERALD,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isLight = themePreset.isLight

    val basePalette = when (themePreset) {
        StudioThemePreset.OBSIDIAN_EMERALD -> ObsidianEmeraldPalette
        StudioThemePreset.MIDNIGHT_NORD -> MidnightSlatePalette
        StudioThemePreset.CRISP_PAPER -> CrispPaperPalette
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isLight) dynamicLightColorScheme(context) else dynamicDarkColorScheme(context)
        }
        themePreset == StudioThemePreset.OBSIDIAN_EMERALD -> ObsidianDarkColorScheme
        themePreset == StudioThemePreset.MIDNIGHT_NORD -> MidnightDarkColorScheme
        else -> CrispLightColorScheme
    }

    // Harmonize semantic palette accents when Material You Dynamic Color is explicitly opted in
    val effectivePalette = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        basePalette.copy(
            canvas = colorScheme.background,
            surface = colorScheme.surface,
            card = colorScheme.surfaceContainerHigh,
            elevated = colorScheme.surfaceContainerHighest,
            border = colorScheme.outline,
            textPrimary = colorScheme.onSurface,
            textSecondary = colorScheme.onSurfaceVariant,
            emerald = colorScheme.primary,
            emeraldSoft = colorScheme.primaryContainer,
            liquid = colorScheme.secondary,
            liquidSoft = colorScheme.secondaryContainer,
            classic = colorScheme.tertiary,
            classicSoft = colorScheme.tertiaryContainer
        )
    } else {
        basePalette
    }

    // Keep system status bar & navigation bar icon contrast crisp in both dark and light presets
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = isLight
                controller.isAppearanceLightNavigationBars = isLight
            }
        }
    }

    CompositionLocalProvider(LocalStudioPalette provides effectivePalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = StudioShapes,
            content = content
        )
    }
}
