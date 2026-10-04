package com.xeg911.appcontrol.ui.feature.composer

import com.xeg911.appcontrol.R
import com.xeg911.shared.data.model.AppIconStyle
import com.xeg911.shared.data.model.notification.NotificationActionDef

/**
 * Value pickers the composer can offer for an action param.
 */
enum class ParamPickerKind(val iconRes: Int) {
    APP(R.drawable.apps_24px),
    PERMISSION(R.drawable.verified_user_24px),
    ICON_STYLE(R.drawable.tune_24px),
    BOOLEAN(R.drawable.check_circle_24px),
}

/**
 * Where a picked value should be written.
 */
sealed interface ParamTarget {
    data object Tap : ParamTarget
    data class Action(val uid: String) : ParamTarget
}

data class PendingPick(val target: ParamTarget, val key: String, val kind: ParamPickerKind)

/**
 * A permission offered by the Permission Picker,
 * with how many target devices still lack it.
 */
data class PermissionPickOption(
    val manifestName: String,
    val label: String,
    val missingOn: Int = 0,
    val deviceCount: Int = 0,
) {
    val fromDevice: Boolean get() = deviceCount > 0
}

/**
 * Single source of truth for how the composer adapts to the selected action:
 * which params get a picker and which picker opens automatically on selection.
 */
object ActionUiSpec {
    const val PARAM_PACKAGE_NAME = "packageName"
    const val PARAM_PERMISSION = "permission"

    private val BOOLEAN_PARAMS = setOf(
        "autoStart", "autoApply", "allowMultiple", "showToolbar", "allowJs", "jsBridge",
        "allowExternalNav", "zoomEnabled", "downloadEnabled", "clearOnExit",
    )

    fun pickerFor(actionId: String, key: String): ParamPickerKind? = when {
        key == PARAM_PACKAGE_NAME -> ParamPickerKind.APP
        key == PARAM_PERMISSION && actionId.equals(
            NotificationActionDef.REQUEST_PERMISSION.id,
            true
        ) ->
            ParamPickerKind.PERMISSION

        key == AppIconStyle.PARAM_ICON_STYLE -> ParamPickerKind.ICON_STYLE
        key in BOOLEAN_PARAMS -> ParamPickerKind.BOOLEAN
        else -> null
    }

    /**
     * Picker that opens right after [actionId] is chosen, keyed by the param it fills.
     * */
    fun autoPickerFor(actionId: String): Pair<String, ParamPickerKind>? =
        when (NotificationActionDef.fromId(actionId)) {
            NotificationActionDef.OPEN_APP,
            NotificationActionDef.OPEN_OTHER_APP -> PARAM_PACKAGE_NAME to ParamPickerKind.APP

            NotificationActionDef.REQUEST_PERMISSION -> PARAM_PERMISSION to ParamPickerKind.PERMISSION
            NotificationActionDef.SET_APP_ICON -> AppIconStyle.PARAM_ICON_STYLE to ParamPickerKind.ICON_STYLE
            else -> null
        }

    fun supportsInlineReply(actionId: String): Boolean =
        actionId.equals(NotificationActionDef.REPLY.id, ignoreCase = true)
}
