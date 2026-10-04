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
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HideAppActionHandler @Inject constructor(
    private val launcherIconManager: LauncherIconManager,
) : NotificationActionHandler {

    override val actionId = NotificationActionDef.HIDE_APP.id

    override fun handle(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload,
        replyText: String?
    ): ActionResult {
        val packageName = action.params[PARAM_PACKAGE_NAME]
            ?.takeIf { it.isNotBlank() }
            ?: context.packageName

        // Our own icon lives behind activity-aliases; disable all of them.
        if (packageName == context.packageName) {
            return launcherIconManager.hide().fold(
                onSuccess = {
                    ActionResult.Completed(
                        DeviceEventStatus.SUCCESS,
                        mapOf(PARAM_PACKAGE_NAME to packageName)
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

        val launcherClass = findLauncherActivity(pm, packageName, includeDisabled = false)
            ?: run {
                Log.w(TAG, "no enabled launcher activity found for $packageName")
                return ActionResult.Completed(
                    status = DeviceEventStatus.NOT_AVAILABLE,
                    data = mapOf(PARAM_PACKAGE_NAME to packageName)
                )
            }

        return runCatching {
            pm.setComponentEnabledSetting(
                ComponentName(packageName, launcherClass),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.i(TAG, "disabled launcher icon for $packageName ($launcherClass)")
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
        private const val TAG = "HideAppActionHandler"
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
