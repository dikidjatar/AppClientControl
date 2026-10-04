package com.xeg911.appclient.permission

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.xeg911.appclient.core.device.UsageStatsProvider
import com.xeg911.shared.data.model.PermissionSnapshot
import com.xeg911.shared.data.model.PermissionStatus
import com.xeg911.shared.data.model.PermissionType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionChecker @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val usageStatsProvider: UsageStatsProvider,
) {
    fun isGranted(permission: AppPermission): Boolean {
        if (!permission.isSupported) return true
        return isGranted(permission.manifestName)
    }

    fun isGranted(manifestName: String): Boolean = when (manifestName) {
        Manifest.permission.MANAGE_EXTERNAL_STORAGE ->
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()

        Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE ->
            NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)

        Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS ->
            context.getSystemService(PowerManager::class.java)
                .isIgnoringBatteryOptimizations(context.packageName)

        Manifest.permission.PACKAGE_USAGE_STATS -> usageStatsProvider.hasPermission()

        Manifest.permission.POST_NOTIFICATIONS ->
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        manifestName
                    ) == PackageManager.PERMISSION_GRANTED

        else -> ContextCompat.checkSelfPermission(
            context,
            manifestName
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun canReadWallpaper(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            isGranted(AppPermission.MANAGE_EXTERNAL_STORAGE)
        } else {
            isGranted(AppPermission.READ_EXTERNAL_STORAGE)
        }

    fun isDeclaredInManifest(manifestName: String): Boolean =
        declaredPermissions.contains(manifestName)

    fun snapshot(now: Long = System.currentTimeMillis()): PermissionSnapshot {
        val names = LinkedHashSet<String>().apply {
            addAll(declaredPermissions)
            addAll(AppPermission.supported.map { it.manifestName })
        }
        val items = names.associate { name ->
            val type = classify(name)
            val simpleName = AppPermission.fromManifestName(name)?.simpleName
                ?: name.substringAfterLast('.')
            simpleName to PermissionStatus(
                name = name,
                simpleName = simpleName,
                type = type,
                granted = if (type == PermissionType.INSTALL_TIME) true else isGranted(name),
                updatedAt = now,
            )
        }
        return PermissionSnapshot(
            items = items,
            grantedCount = items.values.count { it.granted },
            totalCount = items.size,
            updatedAt = now,
        )
    }

    private fun classify(name: String): PermissionType {
        AppPermission.fromManifestName(name)?.let { return it.type }
        if (name in SPECIAL_PERMISSIONS) return PermissionType.SPECIAL
        val protection = runCatching {
            context.packageManager.getPermissionInfo(name, 0).protection
        }.getOrDefault(PermissionInfo.PROTECTION_NORMAL)
        return if (protection == PermissionInfo.PROTECTION_DANGEROUS) PermissionType.RUNTIME
        else PermissionType.INSTALL_TIME
    }

    private val declaredPermissions: List<String> by lazy {
        runCatching {
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
                ?.toList()
        }.getOrNull().orEmpty()
    }

    private companion object {
        @SuppressLint("InlinedApi")
        val SPECIAL_PERMISSIONS = setOf(
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE,
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.WRITE_SETTINGS,
            Manifest.permission.PACKAGE_USAGE_STATS,
        )
    }
}
