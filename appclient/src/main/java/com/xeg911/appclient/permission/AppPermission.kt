package com.xeg911.appclient.permission

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.xeg911.appclient.R
import com.xeg911.shared.data.model.PermissionType

@SuppressLint("InlinedApi")
enum class AppPermission(
    val manifestName: String,
    val type: PermissionType,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    @param:DrawableRes val iconRes: Int,
    val minSdk: Int = Build.VERSION_CODES.BASE,
    val maxSdk: Int = Int.MAX_VALUE,
) {
    POST_NOTIFICATIONS(
        manifestName = Manifest.permission.POST_NOTIFICATIONS,
        type = PermissionType.RUNTIME,
        titleRes = R.string.permission_notifications_title,
        descriptionRes = R.string.permission_notifications_desc,
        iconRes = R.drawable.round_notifications_24,
        minSdk = Build.VERSION_CODES.TIRAMISU,
    ),
    READ_CONTACTS(
        manifestName = Manifest.permission.READ_CONTACTS,
        type = PermissionType.RUNTIME,
        titleRes = R.string.permission_contacts_title,
        descriptionRes = R.string.permission_contacts_desc,
        iconRes = R.drawable.person_24px,
    ),
    READ_EXTERNAL_STORAGE(
        manifestName = Manifest.permission.READ_EXTERNAL_STORAGE,
        type = PermissionType.RUNTIME,
        titleRes = R.string.permission_storage_read_title,
        descriptionRes = R.string.permission_storage_read_desc,
        iconRes = R.drawable.storage_24px,
        maxSdk = Build.VERSION_CODES.S_V2,
    ),
    MANAGE_EXTERNAL_STORAGE(
        manifestName = Manifest.permission.MANAGE_EXTERNAL_STORAGE,
        type = PermissionType.SPECIAL,
        titleRes = R.string.permission_storage_manage_title,
        descriptionRes = R.string.permission_storage_manage_desc,
        iconRes = R.drawable.storage_24px,
        minSdk = Build.VERSION_CODES.R,
    ),
    NOTIFICATION_LISTENER(
        manifestName = Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE,
        type = PermissionType.SPECIAL,
        titleRes = R.string.permission_notification_listener_title,
        descriptionRes = R.string.permission_notification_listener_desc,
        iconRes = R.drawable.verified_user_24px,
    ),
    IGNORE_BATTERY_OPTIMIZATIONS(
        manifestName = Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        type = PermissionType.SPECIAL,
        titleRes = R.string.permission_battery_title,
        descriptionRes = R.string.permission_battery_desc,
        iconRes = R.drawable.battery_alert_24px,
    ),

    ACCESS_FINE_LOCATION(
        manifestName = Manifest.permission.ACCESS_FINE_LOCATION,
        type = PermissionType.RUNTIME,
        titleRes = R.string.permission_location_title,
        descriptionRes = R.string.permission_location_desc,
        iconRes = R.drawable.location_on_24px,
    ),
    ACCESS_BACKGROUND_LOCATION(
        manifestName = Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        type = PermissionType.RUNTIME,
        titleRes = R.string.permission_background_location_title,
        descriptionRes = R.string.permission_background_location_desc,
        iconRes = R.drawable.location_on_24px,
        minSdk = Build.VERSION_CODES.Q,
    ),
    PACKAGE_USAGE_STATS(
        manifestName = Manifest.permission.PACKAGE_USAGE_STATS,
        type = PermissionType.SPECIAL,
        titleRes = R.string.permission_usage_stats_title,
        descriptionRes = R.string.permission_usage_stats_desc,
        iconRes = R.drawable.storage_24px,
    );

    val simpleName: String get() = name

    val isSupported: Boolean
        @SuppressLint("ObsoleteSdkInt")
        get() = Build.VERSION.SDK_INT in minSdk..maxSdk

    val isRuntime: Boolean get() = type == PermissionType.RUNTIME

    companion object {
        val supported: List<AppPermission> by lazy { entries.filter { it.isSupported } }

        fun fromManifestName(name: String): AppPermission? =
            supported.firstOrNull { it.manifestName == name }

        fun fromKeys(keys: Collection<String>): List<AppPermission> =
            keys.mapNotNull { key -> supported.firstOrNull { it.name.equals(key, true) } }
    }
}
