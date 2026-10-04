package com.xeg911.appcontrol.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.xeg911.appcontrol.domain.model.AppFont
import com.xeg911.appcontrol.domain.model.ThemeMode

/**
 * Material color roles plus the semantic status colors AppControl relies on.
 */
@Immutable
data class AppPalette(
    val primary: Color, val onPrimary: Color,
    val primaryContainer: Color, val onPrimaryContainer: Color,
    val secondary: Color, val onSecondary: Color,
    val secondaryContainer: Color, val onSecondaryContainer: Color,
    val tertiary: Color, val onTertiary: Color,
    val tertiaryContainer: Color, val onTertiaryContainer: Color,
    val error: Color, val onError: Color,
    val errorContainer: Color, val onErrorContainer: Color,
    val background: Color, val onBackground: Color,
    val surface: Color, val onSurface: Color,
    val surfaceVariant: Color, val onSurfaceVariant: Color,
    val outline: Color, val outlineVariant: Color,
    val inverseSurface: Color, val inverseOnSurface: Color, val inversePrimary: Color,
    val surfaceContainerLowest: Color, val surfaceContainerLow: Color,
    val surfaceContainer: Color, val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val success: Color, val onSuccess: Color, val successContainer: Color,
    val warning: Color, val onWarning: Color, val warningContainer: Color,
    val info: Color, val onInfo: Color, val infoContainer: Color,
    val neutral: Color,
) {
    // Domain aliases keep call sites readable.
    val online: Color get() = success
    val offline: Color get() = neutral
    val battery: Color get() = success
    val batteryLow: Color get() = error
    val batteryCharging: Color get() = warning
    val permissionRuntime: Color get() = info
    val permissionSpecial: Color get() = tertiary
    val permissionInstall: Color get() = neutral
    val callbackSuccess: Color get() = success
    val callbackFailed: Color get() = error
    val callbackDenied: Color get() = warning
    val callbackCancelled: Color get() = neutral
    val callbackNotAvailable: Color get() = tertiary
    val callbackInfo: Color get() = info
}

private fun AppPalette.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        secondary = secondary, onSecondary = onSecondary,
        secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary, onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
        error = error, onError = onError,
        errorContainer = errorContainer, onErrorContainer = onErrorContainer,
        background = background, onBackground = onBackground,
        surface = surface, onSurface = onSurface,
        surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
        outline = outline, outlineVariant = outlineVariant,
        inverseSurface = inverseSurface, inverseOnSurface = inverseOnSurface,
        inversePrimary = inversePrimary,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceDim = if (dark) surfaceContainerLowest else surfaceContainerHighest,
        surfaceBright = if (dark) surfaceContainerHighest else surface,
    )
}

private val LocalAppPalette = staticCompositionLocalOf { LightPalette }
private val LocalDarkTheme = staticCompositionLocalOf { false }

object AppTheme {
    val colors: AppPalette
        @Composable @ReadOnlyComposable get() = LocalAppPalette.current

    val isDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalDarkTheme.current

    val spacing: Spacing get() = Spacing
    val dimens: Dimens get() = Dimens
}

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun AppControlTheme(
    theme: com.xeg911.appcontrol.domain.model.AppTheme = com.xeg911.appcontrol.domain.model.AppTheme(),
    content: @Composable () -> Unit,
) {
    val dark = theme.mode.isDark()
    val definition = ThemeDefinitions.get(theme.preset)

    val basePalette = if (dark) {
        definition.darkPalette ?: definition.lightPalette ?: DarkPalette
    } else {
        definition.lightPalette ?: definition.darkPalette ?: LightPalette
    }

    val palette = basePalette.copy(
        primary = theme.primaryColorOverride?.let { Color(it) } ?: basePalette.primary,
        secondary = theme.secondaryColorOverride?.let { Color(it) } ?: basePalette.secondary,
        background = theme.backgroundColorOverride?.let { Color(it) } ?: basePalette.background,
        surface = theme.surfaceColorOverride?.let { Color(it) } ?: basePalette.surface,
        tertiary = theme.accentColorOverride?.let { Color(it) } ?: basePalette.tertiary
    ).animated()

    CompositionLocalProvider(
        LocalAppPalette provides palette,
        LocalDarkTheme provides dark,
    ) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(dark),
            typography = getTypographyForFont(theme.font),
            shapes = AppShapes,
            content = content,
        )
    }
}

@Composable
fun AppPalette.animated(): AppPalette {
    val duration = 400

    @Composable
    fun animateColor(color: Color) = animateColorAsState(
        targetValue = color,
        animationSpec = tween(duration),
        label = "color"
    ).value

    return AppPalette(
        primary = animateColor(primary),
        onPrimary = animateColor(onPrimary),
        primaryContainer = animateColor(primaryContainer),
        onPrimaryContainer = animateColor(onPrimaryContainer),
        secondary = animateColor(secondary),
        onSecondary = animateColor(onSecondary),
        secondaryContainer = animateColor(secondaryContainer),
        onSecondaryContainer = animateColor(onSecondaryContainer),
        tertiary = animateColor(tertiary),
        onTertiary = animateColor(onTertiary),
        tertiaryContainer = animateColor(tertiaryContainer),
        onTertiaryContainer = animateColor(onTertiaryContainer),
        error = animateColor(error),
        onError = animateColor(onError),
        errorContainer = animateColor(errorContainer),
        onErrorContainer = animateColor(onErrorContainer),
        background = animateColor(background),
        onBackground = animateColor(onBackground),
        surface = animateColor(surface),
        onSurface = animateColor(onSurface),
        surfaceVariant = animateColor(surfaceVariant),
        onSurfaceVariant = animateColor(onSurfaceVariant),
        outline = animateColor(outline),
        outlineVariant = animateColor(outlineVariant),
        inverseSurface = animateColor(inverseSurface),
        inverseOnSurface = animateColor(inverseOnSurface),
        inversePrimary = animateColor(inversePrimary),
        surfaceContainerLowest = animateColor(surfaceContainerLowest),
        surfaceContainerLow = animateColor(surfaceContainerLow),
        surfaceContainer = animateColor(surfaceContainer),
        surfaceContainerHigh = animateColor(surfaceContainerHigh),
        surfaceContainerHighest = animateColor(surfaceContainerHighest),
        success = animateColor(success),
        onSuccess = animateColor(onSuccess),
        successContainer = animateColor(successContainer),
        warning = animateColor(warning),
        onWarning = animateColor(onWarning),
        warningContainer = animateColor(warningContainer),
        info = animateColor(info),
        onInfo = animateColor(onInfo),
        infoContainer = animateColor(infoContainer),
        neutral = animateColor(neutral),
    )
}

fun getTypographyForFont(font: AppFont): Typography {
    val fontFamily = when (font) {
        AppFont.DEFAULT -> androidx.compose.ui.text.font.FontFamily.Default
        AppFont.ROBOTO -> androidx.compose.ui.text.font.FontFamily.SansSerif
        AppFont.MONTSERRAT -> androidx.compose.ui.text.font.FontFamily.Serif
        AppFont.LATO -> androidx.compose.ui.text.font.FontFamily.SansSerif
        AppFont.POPPINS -> androidx.compose.ui.text.font.FontFamily.Cursive
        AppFont.SOURCE_CODE_PRO -> androidx.compose.ui.text.font.FontFamily.Monospace
    }

    return Typography.copy(
        displayLarge = Typography.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = Typography.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = Typography.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = Typography.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = Typography.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = Typography.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = Typography.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = Typography.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = Typography.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = Typography.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = Typography.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = Typography.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = Typography.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = Typography.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = Typography.labelSmall.copy(fontFamily = fontFamily),
    )
}
