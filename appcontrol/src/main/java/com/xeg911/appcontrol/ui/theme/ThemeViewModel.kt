package com.xeg911.appcontrol.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xeg911.appcontrol.domain.model.AppFont
import com.xeg911.appcontrol.domain.model.AppTheme
import com.xeg911.appcontrol.domain.model.ThemeMode
import com.xeg911.appcontrol.domain.model.ThemePreset
import com.xeg911.appcontrol.domain.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    val themeState: StateFlow<AppTheme> = themeRepository.theme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppTheme()
    )

    fun updateMode(mode: ThemeMode) = update { it.copy(mode = mode) }
    fun updatePreset(preset: ThemePreset) = update { it.copy(preset = preset) }
    fun updateFont(font: AppFont) = update { it.copy(font = font) }
    fun overridePrimaryColor(color: Long?) = update { it.copy(primaryColorOverride = color) }
    fun overrideSecondaryColor(color: Long?) = update { it.copy(secondaryColorOverride = color) }
    fun overrideBackgroundColor(color: Long?) = update { it.copy(backgroundColorOverride = color) }
    fun overrideSurfaceColor(color: Long?) = update { it.copy(surfaceColorOverride = color) }
    fun overrideAccentColor(color: Long?) = update { it.copy(accentColorOverride = color) }

    private fun update(updater: (AppTheme) -> AppTheme) {
        viewModelScope.launch {
            themeRepository.updateTheme(updater)
        }
    }
}
