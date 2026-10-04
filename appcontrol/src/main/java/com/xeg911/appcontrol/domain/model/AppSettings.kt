package com.xeg911.appcontrol.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class ControlIconStyle { DEFAULT, TERMINAL, RADAR, HEX }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val iconStyle: ControlIconStyle = ControlIconStyle.DEFAULT,
)
