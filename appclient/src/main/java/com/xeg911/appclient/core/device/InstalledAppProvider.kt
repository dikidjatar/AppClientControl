package com.xeg911.appclient.core.device

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.core.content.pm.PackageInfoCompat
import com.xeg911.shared.data.model.InstalledApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstalledAppProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun collect(): List<InstalledApp> {
        val packageManager = context.packageManager

        return packageManager
            .getInstalledPackages(0)
            .filter { packageInfo ->
                val flags = packageInfo.applicationInfo?.flags ?: 0
                (flags and ApplicationInfo.FLAG_SYSTEM) == 0
            }
            .map { packageInfo ->
                val appName = packageInfo.applicationInfo
                    ?.loadLabel(packageManager)
                    ?.toString()
                    .orEmpty()
                InstalledApp(
                    packageName = packageInfo.packageName,
                    appName = appName,
                    versionName = packageInfo.versionName.orEmpty(),
                    versionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
                    firstInstallTime = packageInfo.firstInstallTime,
                    lastUpdateTime = packageInfo.lastUpdateTime,
                    sortKey = appName.ifBlank { packageInfo.packageName }.trim().lowercase()
                )
            }
    }
}