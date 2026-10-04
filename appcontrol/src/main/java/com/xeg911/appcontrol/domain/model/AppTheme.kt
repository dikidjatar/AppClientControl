package com.xeg911.appcontrol.domain.model

enum class ThemePreset {
    DEFAULT, HACKER, MIDNIGHT, OCEAN, SUNSET, FOREST, DRACULA, NORD, MONOCHROME, ROSE, MATERIAL_YOU
}

enum class AppFont {
    DEFAULT, ROBOTO, MONTSERRAT, LATO, POPPINS, SOURCE_CODE_PRO
}

data class AppTheme(
    val preset: ThemePreset = ThemePreset.DEFAULT,
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val font: AppFont = AppFont.DEFAULT,
    val primaryColorOverride: Long? = null,
    val secondaryColorOverride: Long? = null,
    val backgroundColorOverride: Long? = null,
    val surfaceColorOverride: Long? = null,
    val accentColorOverride: Long? = null,
)
