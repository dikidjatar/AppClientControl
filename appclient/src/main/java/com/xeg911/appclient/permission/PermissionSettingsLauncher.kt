package com.xeg911.appclient.permission

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.xeg911.appclient.service.notification.AppNotificationListenerService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionSettingsLauncher @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun open(permission: AppPermission) = launchFirstResolvable(intentsFor(permission))

    fun intentFor(permission: AppPermission): Intent = intentsFor(permission).first()

    private fun intentsFor(permission: AppPermission): List<Intent> = when (permission) {
        AppPermission.MANAGE_EXTERNAL_STORAGE -> manageStorageIntents()
        AppPermission.NOTIFICATION_LISTENER -> listOf(notificationListenerIntent())
        AppPermission.IGNORE_BATTERY_OPTIMIZATIONS -> batteryOptimizationIntents()
        AppPermission.PACKAGE_USAGE_STATS -> listOf(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        else -> listOf(appDetailsIntent())
    }

    fun openAppDetails() = launchFirstResolvable(listOf(appDetailsIntent()))

    private fun launchFirstResolvable(intents: List<Intent>) {
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(intent) }.isSuccess) return
        }
    }

    private fun appDetailsIntent() = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )

    private fun manageStorageIntents(): List<Intent> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return listOf(appDetailsIntent())
        return listOf(
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, packageUri()),
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
        )
    }

    private fun notificationListenerIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(
                    context,
                    AppNotificationListenerService::class.java
                ).flattenToString(),
            )
        } else {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }

    @SuppressLint("BatteryLife")
    private fun batteryOptimizationIntents() = listOf(
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri()),
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
    )

    private fun packageUri(): Uri = "package:${context.packageName}".toUri()
}
