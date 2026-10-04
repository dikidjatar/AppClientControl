package com.xeg911.appclient.notification.action.handler

import android.content.Context
import com.xeg911.appclient.core.di.ApplicationScope
import com.xeg911.appclient.core.preference.AppPreferences
import com.xeg911.appclient.launcher.LauncherIconManager
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.appclient.notification.action.NotificationActionHandler
import com.xeg911.shared.data.model.AppIconStyle
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetAppIconActionHandler @Inject constructor(
    private val launcherIconManager: LauncherIconManager,
    private val preferences: AppPreferences,
    @param:ApplicationScope private val appScope: CoroutineScope,
) : NotificationActionHandler {

    override val actionId = NotificationActionDef.SET_APP_ICON.id

    override fun handle(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload,
        replyText: String?
    ): ActionResult {
        val requested = action.params[AppIconStyle.PARAM_ICON_STYLE].orEmpty()
        val style = AppIconStyle.fromId(requested) ?: return ActionResult.Completed(
            status = DeviceEventStatus.NOT_AVAILABLE,
            data = mapOf(
                AppIconStyle.PARAM_ICON_STYLE to requested,
                KEY_SUPPORTED to AppIconStyle.entries.joinToString(",") { it.id },
            ),
        )
        val previous = launcherIconManager.current()?.id.orEmpty()

        return launcherIconManager.apply(style).fold(
            onSuccess = {
                appScope.launch { preferences.setLauncherIconStyle(style.id) }
                ActionResult.Completed(
                    status = DeviceEventStatus.SUCCESS,
                    data = mapOf(
                        AppIconStyle.PARAM_ICON_STYLE to style.id,
                        KEY_PREVIOUS to previous
                    ),
                )
            },
            onFailure = { e ->
                ActionResult.Completed(
                    status = DeviceEventStatus.FAILED,
                    data = mapOf(
                        AppIconStyle.PARAM_ICON_STYLE to style.id,
                        KEY_ERROR to (e.message ?: e.javaClass.simpleName),
                    ),
                )
            },
        )
    }

    private companion object {
        const val KEY_PREVIOUS = "previous"
        const val KEY_SUPPORTED = "supported"
        const val KEY_ERROR = "error"
    }
}
