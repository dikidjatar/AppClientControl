package com.xeg911.appclient.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.appclient.location.LocationSharingController
import com.xeg911.shared.data.model.event.DeviceEventType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var monitoringController: MonitoringController

    @Inject
    lateinit var locationSharingController: LocationSharingController

    @Inject
    lateinit var eventReporter: DeviceEventReporter

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        eventReporter.report(DeviceEventType.BOOT_COMPLETED)
        monitoringController.startIfAllowed(MonitoringController.StartReason.BOOT)
        locationSharingController.startIfEnabled(LocationSharingController.StartReason.BOOT)
    }
}
