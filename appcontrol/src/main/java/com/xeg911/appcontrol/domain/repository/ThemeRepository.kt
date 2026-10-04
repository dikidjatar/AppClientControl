package com.xeg911.appcontrol.domain.repository

import com.xeg911.appcontrol.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    val theme: Flow<AppTheme>
    suspend fun updateTheme(update: (AppTheme) -> AppTheme)
}
