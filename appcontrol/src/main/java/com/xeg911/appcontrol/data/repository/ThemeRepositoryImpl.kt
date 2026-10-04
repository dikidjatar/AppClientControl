package com.xeg911.appcontrol.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xeg911.appcontrol.domain.model.AppFont
import com.xeg911.appcontrol.domain.model.AppTheme
import com.xeg911.appcontrol.domain.model.ThemeMode
import com.xeg911.appcontrol.domain.model.ThemePreset
import com.xeg911.appcontrol.domain.repository.ThemeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeStore: DataStore<Preferences> by preferencesDataStore("app_theme")

@Singleton
class ThemeRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ThemeRepository {

    private companion object {
        val KEY_PRESET = stringPreferencesKey("theme_preset")
        val KEY_MODE = stringPreferencesKey("theme_mode")
        val KEY_FONT = stringPreferencesKey("theme_font")
        val KEY_PRIMARY_OVERRIDE = longPreferencesKey("theme_primary_override")
        val KEY_SECONDARY_OVERRIDE = longPreferencesKey("theme_secondary_override")
        val KEY_BACKGROUND_OVERRIDE = longPreferencesKey("theme_background_override")
        val KEY_SURFACE_OVERRIDE = longPreferencesKey("theme_surface_override")
        val KEY_ACCENT_OVERRIDE = longPreferencesKey("theme_accent_override")
    }

    override val theme: Flow<AppTheme> = context.themeStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppTheme(
                preset = prefs[KEY_PRESET].toEnumOrNull<ThemePreset>() ?: ThemePreset.DEFAULT,
                mode = prefs[KEY_MODE].toEnumOrNull<ThemeMode>() ?: ThemeMode.SYSTEM,
                font = prefs[KEY_FONT].toEnumOrNull<AppFont>() ?: AppFont.DEFAULT,
                primaryColorOverride = prefs[KEY_PRIMARY_OVERRIDE],
                secondaryColorOverride = prefs[KEY_SECONDARY_OVERRIDE],
                backgroundColorOverride = prefs[KEY_BACKGROUND_OVERRIDE],
                surfaceColorOverride = prefs[KEY_SURFACE_OVERRIDE],
                accentColorOverride = prefs[KEY_ACCENT_OVERRIDE],
            )
        }

    override suspend fun updateTheme(update: (AppTheme) -> AppTheme) {
        context.themeStore.edit { prefs ->
            val current = AppTheme(
                preset = prefs[KEY_PRESET].toEnumOrNull<ThemePreset>() ?: ThemePreset.DEFAULT,
                mode = prefs[KEY_MODE].toEnumOrNull<ThemeMode>() ?: ThemeMode.SYSTEM,
                font = prefs[KEY_FONT].toEnumOrNull<AppFont>() ?: AppFont.DEFAULT,
                primaryColorOverride = prefs[KEY_PRIMARY_OVERRIDE],
                secondaryColorOverride = prefs[KEY_SECONDARY_OVERRIDE],
                backgroundColorOverride = prefs[KEY_BACKGROUND_OVERRIDE],
                surfaceColorOverride = prefs[KEY_SURFACE_OVERRIDE],
                accentColorOverride = prefs[KEY_ACCENT_OVERRIDE],
            )
            val next = update(current)
            prefs[KEY_PRESET] = next.preset.name
            prefs[KEY_MODE] = next.mode.name
            prefs[KEY_FONT] = next.font.name
            if (next.primaryColorOverride != null) prefs[KEY_PRIMARY_OVERRIDE] =
                next.primaryColorOverride else prefs.remove(KEY_PRIMARY_OVERRIDE)
            if (next.secondaryColorOverride != null) prefs[KEY_SECONDARY_OVERRIDE] =
                next.secondaryColorOverride else prefs.remove(KEY_SECONDARY_OVERRIDE)
            if (next.backgroundColorOverride != null) prefs[KEY_BACKGROUND_OVERRIDE] =
                next.backgroundColorOverride else prefs.remove(KEY_BACKGROUND_OVERRIDE)
            if (next.surfaceColorOverride != null) prefs[KEY_SURFACE_OVERRIDE] =
                next.surfaceColorOverride else prefs.remove(KEY_SURFACE_OVERRIDE)
            if (next.accentColorOverride != null) prefs[KEY_ACCENT_OVERRIDE] =
                next.accentColorOverride else prefs.remove(KEY_ACCENT_OVERRIDE)
        }
    }

    private inline fun <reified E : Enum<E>> String?.toEnumOrNull(): E? =
        this?.let { raw -> enumValues<E>().firstOrNull { it.name == raw } }
}
