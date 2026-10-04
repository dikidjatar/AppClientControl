package com.xeg911.appcontrol.core.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.xeg911.appcontrol.domain.model.ControlIconStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LauncherIconManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun apply(style: ControlIconStyle): Result<Unit> = runCatching {
        val pm = context.packageManager
        // Enable the new alias first so the launcher never sees zero entries.
        pm.setState(style, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
        ControlIconStyle.entries
            .filter { it != style }
            .forEach { pm.setState(it, PackageManager.COMPONENT_ENABLED_STATE_DISABLED) }
    }

    private fun PackageManager.setState(style: ControlIconStyle, state: Int) =
        setComponentEnabledSetting(component(style), state, PackageManager.DONT_KILL_APP)

    private fun component(style: ControlIconStyle) =
        ComponentName(context, "${context.packageName}.launcher.${aliasName(style)}")

    private fun aliasName(style: ControlIconStyle) = when (style) {
        ControlIconStyle.DEFAULT -> "DefaultIcon"
        ControlIconStyle.TERMINAL -> "TerminalIcon"
        ControlIconStyle.RADAR -> "RadarIcon"
        ControlIconStyle.HEX -> "HexIcon"
    }
}
