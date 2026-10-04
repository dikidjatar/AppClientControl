package com.xeg911.appcontrol.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xeg911.appcontrol.domain.model.AppSettings
import com.xeg911.appcontrol.domain.model.ControlIconStyle
import com.xeg911.appcontrol.domain.model.ThemeMode
import com.xeg911.appcontrol.domain.repository.AppSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore("app_settings")

@Singleton
class AppSettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AppSettingsRepository {

    override val settings: Flow<AppSettings> = context.settingsStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                themeMode = prefs[KEY_THEME_MODE].toEnumOrNull<ThemeMode>() ?: ThemeMode.SYSTEM,
                iconStyle = prefs[KEY_ICON_STYLE].toEnumOrNull<ControlIconStyle>()
                    ?: ControlIconStyle.DEFAULT,
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    override suspend fun setIconStyle(style: ControlIconStyle) {
        context.settingsStore.edit { it[KEY_ICON_STYLE] = style.name }
    }

    private inline fun <reified E : Enum<E>> String?.toEnumOrNull(): E? =
        this?.let { raw -> enumValues<E>().firstOrNull { it.name == raw } }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_ICON_STYLE = stringPreferencesKey("icon_style")
    }
}
