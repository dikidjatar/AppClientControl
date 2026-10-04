package com.xeg911.appclient.core.device

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.core.content.pm.PackageInfoCompat
import com.xeg911.shared.data.model.DeviceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceInfoProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun collect(deviceId: String): DeviceInfo {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

        return DeviceInfo(
            deviceId = deviceId,
            deviceName = resolveDeviceName(),
            model = Build.MODEL,
            brand = Build.BRAND,
            manufacturer = Build.MANUFACTURER,
            product = Build.PRODUCT,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            appVersionName = packageInfo.versionName.orEmpty(),
            appVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
            lastSeen = System.currentTimeMillis()
        )
    }

    private fun resolveDeviceName(): String {
        val deviceName =
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        if (!deviceName.isNullOrBlank()) {
            return deviceName
        }
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }
}