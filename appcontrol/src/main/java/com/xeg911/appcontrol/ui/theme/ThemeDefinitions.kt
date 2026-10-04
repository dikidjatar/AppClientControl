package com.xeg911.appcontrol.ui.theme

import androidx.compose.ui.graphics.Color
import com.xeg911.appcontrol.domain.model.ThemePreset

data class ThemeDefinition(
    val preset: ThemePreset,
    val name: String,
    val supportsLight: Boolean,
    val supportsDark: Boolean,
    val lightPalette: AppPalette? = null,
    val darkPalette: AppPalette? = null
)

object ThemeDefinitions {

    val Default = ThemeDefinition(
        preset = ThemePreset.DEFAULT,
        name = "Default",
        supportsLight = true,
        supportsDark = true,
        lightPalette = LightPalette,
        darkPalette = DarkPalette
    )

    val Hacker = ThemeDefinition(
        preset = ThemePreset.HACKER,
        name = "Hacker",
        supportsLight = false,
        supportsDark = true,
        darkPalette = DarkPalette.copy(
            primary = Color(0xFF00FF00),
            onPrimary = Color(0xFF000000),
            primaryContainer = Color(0xFF003300),
            onPrimaryContainer = Color(0xFF00FF00),
            secondary = Color(0xFF00CC00),
            background = Color(0xFF000000),
            surface = Color(0xFF050505),
            surfaceContainer = Color(0xFF0A0A0A)
        )
    )

    val Midnight = ThemeDefinition(
        preset = ThemePreset.MIDNIGHT,
        name = "Midnight",
        supportsLight = false,
        supportsDark = true,
        darkPalette = DarkPalette.copy(
            primary = Color(0xFF5C9CFF),
            background = Color(0xFF080D1A),
            surface = Color(0xFF101726),
            surfaceContainer = Color(0xFF182236)
        )
    )

    val Ocean = ThemeDefinition(
        preset = ThemePreset.OCEAN,
        name = "Ocean",
        supportsLight = true,
        supportsDark = true,
        lightPalette = LightPalette.copy(
            primary = Color(0xFF006C75),
            background = Color(0xFFEAF5F6),
            surface = Color(0xFFF2F9FA)
        ),
        darkPalette = DarkPalette.copy(
            primary = Color(0xFF4DD8E5),
            background = Color(0xFF051618),
            surface = Color(0xFF0A2225)
        )
    )

    val Sunset = ThemeDefinition(
        preset = ThemePreset.SUNSET,
        name = "Sunset",
        supportsLight = true,
        supportsDark = true,
        lightPalette = LightPalette.copy(
            primary = Color(0xFFB34A00),
            background = Color(0xFFFFF6F0),
            surface = Color(0xFFFFF9F5)
        ),
        darkPalette = DarkPalette.copy(
            primary = Color(0xFFFFB591),
            background = Color(0xFF1F0D00),
            surface = Color(0xFF2E1300)
        )
    )

    val Forest = ThemeDefinition(
        preset = ThemePreset.FOREST,
        name = "Forest",
        supportsLight = true,
        supportsDark = true,
        lightPalette = LightPalette.copy(
            primary = Color(0xFF2E7D32),
            background = Color(0xFFF1F8E9),
            surface = Color(0xFFF9FBE7)
        ),
        darkPalette = DarkPalette.copy(
            primary = Color(0xFFA5D6A7),
            background = Color(0xFF0B190D),
            surface = Color(0xFF122815)
        )
    )

    val Dracula = ThemeDefinition(
        preset = ThemePreset.DRACULA,
        name = "Dracula",
        supportsLight = false,
        supportsDark = true,
        darkPalette = DarkPalette.copy(
            primary = Color(0xFFFF79C6),
            secondary = Color(0xFFBD93F9),
            background = Color(0xFF282A36),
            surface = Color(0xFF383A59),
            surfaceContainer = Color(0xFF44475A)
        )
    )

    val Nord = ThemeDefinition(
        preset = ThemePreset.NORD,
        name = "Nord",
        supportsLight = true,
        supportsDark = true,
        lightPalette = LightPalette.copy(
            primary = Color(0xFF5E81AC),
            secondary = Color(0xFF81A1C1),
            background = Color(0xFFECEFF4),
            surface = Color(0xFFE5E9F0)
        ),
        darkPalette = DarkPalette.copy(
            primary = Color(0xFF88C0D0),
            secondary = Color(0xFF8FBCBB),
            background = Color(0xFF2E3440),
            surface = Color(0xFF3B4252),
            surfaceContainer = Color(0xFF434C5E)
        )
    )

    val Monochrome = ThemeDefinition(
        preset = ThemePreset.MONOCHROME,
        name = "Monochrome",
        supportsLight = true,
        supportsDark = true,
        lightPalette = LightPalette.copy(
            primary = Color(0xFF000000),
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF5F5F5)
        ),
        darkPalette = DarkPalette.copy(
            primary = Color(0xFFFFFFFF),
            background = Color(0xFF000000),
            surface = Color(0xFF111111)
        )
    )

    val Rose = ThemeDefinition(
        preset = ThemePreset.ROSE,
        name = "Rose",
        supportsLight = true,
        supportsDark = true,
        lightPalette = LightPalette.copy(
            primary = Color(0xFFE91E63),
            background = Color(0xFFFCE4EC),
            surface = Color(0xFFF8BBD0)
        ),
        darkPalette = DarkPalette.copy(
            primary = Color(0xFFF48FB1),
            background = Color(0xFF240410),
            surface = Color(0xFF360618)
        )
    )

    val MaterialYou = ThemeDefinition(
        preset = ThemePreset.MATERIAL_YOU,
        name = "Material You",
        supportsLight = true,
        supportsDark = true,
        lightPalette = LightPalette, // Will be overridden dynamically if supported
        darkPalette = DarkPalette
    )

    val all = listOf(
        Default,
        Hacker,
        Midnight,
        Ocean,
        Sunset,
        Forest,
        Dracula,
        Nord,
        Monochrome,
        Rose,
        MaterialYou
    )

    fun get(preset: ThemePreset): ThemeDefinition =
        all.find { it.preset == preset } ?: Default
}
