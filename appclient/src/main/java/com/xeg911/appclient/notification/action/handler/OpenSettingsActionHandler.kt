package com.xeg911.appclient.notification.action.handler

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.appclient.notification.action.NotificationActionHandler
import com.xeg911.appclient.permission.AppPermission
import com.xeg911.appclient.permission.PermissionSettingsLauncher
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenSettingsActionHandler @Inject constructor(
    private val settingsLauncher: PermissionSettingsLauncher,
) : NotificationActionHandler {
    override val actionId: String = NotificationActionDef.OPEN_SETTINGS.id

    override fun buildActivityIntent(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload
    ): Intent? {
        val screen = action.params["screen"]?.trim() ?: ""
        val targetPackage = action.params["packageName"]
            ?.takeIf { it.isNotBlank() }
            ?: context.packageName
        return toSettingsIntent(context, screen, targetPackage)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
    }

    override fun handle(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload,
        replyText: String?
    ): ActionResult {
        runCatching {
            buildActivityIntent(
                context,
                action,
                payload
            )?.let { context.startActivity(it) }
        }.onFailure { Log.e(TAG, "Failed to open settings screen", it) }
        return ActionResult.Delegated
    }

    @SuppressLint("BatteryLife")
    private fun toSettingsIntent(
        context: Context,
        screen: String,
        targetPackage: String
    ): Intent? {
        if (screen.isEmpty()) return null

        if (screen.equals("notification_listener", ignoreCase = true)) {
            return settingsLauncher.intentFor(AppPermission.NOTIFICATION_LISTENER)
        }

        val resolvedAction = ACTION_MAP[screen.lowercase()] ?: screen

        return Intent(resolvedAction).apply {
            when (resolvedAction) {
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> {
                    data = Uri.fromParts("package", targetPackage, null)
                }

                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        data = Uri.fromParts("package", targetPackage, null)
                    } else {
                        return null
                    }
                }
            }
        }

    }

    private companion object {
        const val TAG = "OpenSettingsActionHandler"

        @SuppressLint("InlinedApi")
        val ACTION_MAP = mapOf(
            "wifi" to Settings.ACTION_WIFI_SETTINGS,
            "bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
            "location" to Settings.ACTION_LOCATION_SOURCE_SETTINGS,
            "sound" to Settings.ACTION_SOUND_SETTINGS,
            "display" to Settings.ACTION_DISPLAY_SETTINGS,
            "battery" to Settings.ACTION_BATTERY_SAVER_SETTINGS,
            "accessibility" to Settings.ACTION_ACCESSIBILITY_SETTINGS,
            "airplane" to Settings.ACTION_AIRPLANE_MODE_SETTINGS,
            "nfc" to Settings.ACTION_NFC_SETTINGS,
            "storage" to Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
            "apps" to Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS,
            "developer" to Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
            "app_info" to Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "all_file_access" to Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
        )
    }
}