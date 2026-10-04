package com.xeg911.appclient

import android.app.Application
import com.xeg911.appclient.monitoring.watchdog.ServiceWatchdogScheduler
import com.xeg911.appclient.session.DeviceSyncCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var syncCoordinator: DeviceSyncCoordinator

    @Inject
    lateinit var watchdogScheduler: ServiceWatchdogScheduler

    override fun onCreate() {
        super.onCreate()
        syncCoordinator.start()
        watchdogScheduler.schedule()
    }
}
