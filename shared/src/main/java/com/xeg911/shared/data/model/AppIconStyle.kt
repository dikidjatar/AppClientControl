package com.xeg911.shared.data.model

/**
 * Launcher icon styles AppClient can switch to. [DEFAULT] is the stock icon.
 * Sent by AppControl as the `iconStyle` param of a SET_APP_ICON action.
 */
enum class AppIconStyle(val id: String, val label: String) {
    DEFAULT("default", "Default"),
    MONO("mono", "Mono"),
    SHIELD("shield", "Shield"),
    ORBIT("orbit", "Orbit");

    companion object {
        const val PARAM_ICON_STYLE = "iconStyle"
        const val PARAM_AUTO_APPLY = "autoApply"

        fun fromId(id: String?): AppIconStyle? =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) }
    }
}
