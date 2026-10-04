package com.xeg911.appclient.core.device

import android.app.ActivityManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.Display
import android.view.WindowManager
import com.xeg911.shared.data.model.DisplayInfo
import com.xeg911.shared.data.model.HardwareInfo
import com.xeg911.shared.data.model.RamInfo
import com.xeg911.shared.data.model.StorageInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HardwareInfoProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun collect(): HardwareInfo = HardwareInfo(
        ram = collectRam(),
        internalStorage = collectInternalStorage(),
        externalStorage = collectExternalStorage(),
        display = collectDisplay(),
        processorAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
        cpuCoreCount = Runtime.getRuntime().availableProcessors(),
        locale = Locale.getDefault().toLanguageTag(),
        timezone = TimeZone.getDefault().id,
        rooted = detectRoot()
    )

    private fun collectRam(): RamInfo {
        val am = context.getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        return RamInfo(
            totalBytes = mi.totalMem,
            availableBytes = mi.availMem,
            lowMemory = mi.lowMemory,
            lowMemoryThresholdBytes = mi.threshold
        )
    }

    private fun collectInternalStorage(): StorageInfo {
        val stat = StatFs(Environment.getDataDirectory().path)
        return StorageInfo(totalBytes = stat.totalBytes, freeBytes = stat.availableBytes)
    }

    private fun collectExternalStorage(): StorageInfo? {
        val dir = context.getExternalFilesDir(null) ?: return null
        return try {
            val stat = StatFs(dir.absolutePath)
            StorageInfo(totalBytes = stat.totalBytes, freeBytes = stat.availableBytes)
        } catch (_: Exception) {
            null
        }
    }

    private fun collectDisplay(): DisplayInfo {
        val dm = context.resources.displayMetrics
        val fontScale = context.resources.configuration.fontScale

        val refreshRate = context.getSystemService(DisplayManager::class.java)
            ?.getDisplay(Display.DEFAULT_DISPLAY)
            ?.refreshRate
            ?: 60f

        val (width, height) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = context.getSystemService(WindowManager::class.java)
                .currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            dm.widthPixels to dm.heightPixels
        }

        return DisplayInfo(
            widthPx = width,
            heightPx = height,
            densityDpi = dm.densityDpi,
            refreshRate = refreshRate,
            fontScaleFactor = fontScale
        )
    }

    private fun detectRoot(): Boolean {
        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/app/Superuser.apk",
            "/system/app/SuperSU/SuperSU.apk",
            "/data/local/bin/su",
            "/data/local/xbin/su"
        )
        return suPaths.any { File(it).exists() }
    }
}