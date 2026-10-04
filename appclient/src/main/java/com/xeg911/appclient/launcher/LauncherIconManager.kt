package com.xeg911.appclient.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.xeg911.appclient.core.preference.AppPreferences
import com.xeg911.shared.data.model.AppIconStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LauncherIconManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: AppPreferences,
) {
    private val pm: PackageManager get() = context.packageManager

    /**
     * Enabled alias, or null when the icon is hidden.
     */
    fun current(): AppIconStyle? = AppIconStyle.entries.firstOrNull { isEnabled(it) }

    fun apply(style: AppIconStyle): Result<Unit> = runCatching {
        // Enable the new alias first so the launcher never sees zero entries.
        setEnabled(style, true)
        AppIconStyle.entries.filter { it != style }.forEach { setEnabled(it, false) }
    }

    fun hide(): Result<Unit> = runCatching {
        AppIconStyle.entries.forEach { setEnabled(it, false) }
    }

    /**
     * Re-enables the last persisted style (or [AppIconStyle.DEFAULT]).
     */
    fun restore(): Result<AppIconStyle> = runCatching {
        val style = runBlocking { preferences.launcherIconStyle() }
            .let(AppIconStyle::fromId) ?: AppIconStyle.DEFAULT
        apply(style).getOrThrow()
        style
    }

    private fun component(style: AppIconStyle) =
        ComponentName(context, "${context.packageName}.launcher.${aliasName(style)}")

    private fun aliasName(style: AppIconStyle) = when (style) {
        AppIconStyle.DEFAULT -> "DefaultIcon"
        AppIconStyle.MONO -> "MonoIcon"
        AppIconStyle.SHIELD -> "ShieldIcon"
        AppIconStyle.ORBIT -> "OrbitIcon"
    }

    private fun isEnabled(style: AppIconStyle): Boolean =
        when (pm.getComponentEnabledSetting(component(style))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> style == AppIconStyle.DEFAULT
            else -> false
        }

    private fun setEnabled(style: AppIconStyle, enabled: Boolean) {
        pm.setComponentEnabledSetting(
            component(style),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }
}
