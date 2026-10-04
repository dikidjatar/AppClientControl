package com.xeg911.appclient.notification.action.handler

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.xeg911.appclient.launcher.LauncherIconManager
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.appclient.notification.action.NotificationActionHandler
import com.xeg911.shared.data.model.AppIconStyle
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowAppActionHandler @Inject constructor(
    private val launcherIconManager: LauncherIconManager,
) : NotificationActionHandler {

    override val actionId = NotificationActionDef.SHOW_APP.id

    override fun handle(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload,
        replyText: String?
    ): ActionResult {
        val packageName = action.params[PARAM_PACKAGE_NAME]
            ?.takeIf { it.isNotBlank() }
            ?: context.packageName

        // Our own icon: re-enable the alias of the last selected style only.
        if (packageName == context.packageName) {
            return launcherIconManager.restore().fold(
                onSuccess = { style ->
                    ActionResult.Completed(
                        DeviceEventStatus.SUCCESS,
                        mapOf(
                            PARAM_PACKAGE_NAME to packageName,
                            AppIconStyle.PARAM_ICON_STYLE to style.id
                        )
                    )
                },
                onFailure = { e ->
                    ActionResult.Completed(
                        DeviceEventStatus.FAILED,
                        mapOf(
                            PARAM_PACKAGE_NAME to packageName,
                            KEY_ERROR to (e.message ?: e.javaClass.simpleName)
                        )
                    )
                },
            )
        }

        val pm = context.packageManager

        // Must include disabled components: the launcher activity may already be hidden.
        val launcherClass = findLauncherActivity(pm, packageName, includeDisabled = true)
            ?: run {
                Log.w(TAG, "no launcher activity found (enabled or disabled) for $packageName")
                return ActionResult.Completed(
                    status = DeviceEventStatus.NOT_AVAILABLE,
                    data = mapOf(PARAM_PACKAGE_NAME to packageName)
                )
            }

        return runCatching {
            pm.setComponentEnabledSetting(
                ComponentName(packageName, launcherClass),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.i(TAG, "restored launcher icon for $packageName ($launcherClass)")
            ActionResult.Completed(
                status = DeviceEventStatus.SUCCESS,
                data = mapOf(PARAM_PACKAGE_NAME to packageName)
            )
        }.getOrElse { e ->
            Log.e(TAG, "failed for $packageName", e)
            ActionResult.Completed(
                status = DeviceEventStatus.FAILED,
                data = mapOf(
                    PARAM_PACKAGE_NAME to packageName,
                    KEY_ERROR to (e.message ?: e.javaClass.simpleName)
                )
            )
        }
    }

    private companion object {
        private const val TAG = "ShowAppActionHandler"
        private const val PARAM_PACKAGE_NAME = "packageName"
        private const val KEY_ERROR = "error"

        fun findLauncherActivity(
            pm: PackageManager,
            packageName: String,
            includeDisabled: Boolean
        ): String? {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
            }
            val flags = if (includeDisabled) PackageManager.MATCH_DISABLED_COMPONENTS else 0
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(flags.toLong())
                )
            } else {
                pm.queryIntentActivities(intent, flags)
            }.firstOrNull()?.activityInfo?.name
        }
    }
}
