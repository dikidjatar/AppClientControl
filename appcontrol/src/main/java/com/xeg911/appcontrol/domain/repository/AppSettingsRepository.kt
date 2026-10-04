package com.xeg911.appcontrol.domain.repository

import com.xeg911.appcontrol.domain.model.AppSettings
import com.xeg911.appcontrol.domain.model.ControlIconStyle
import com.xeg911.appcontrol.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setIconStyle(style: ControlIconStyle)
}
