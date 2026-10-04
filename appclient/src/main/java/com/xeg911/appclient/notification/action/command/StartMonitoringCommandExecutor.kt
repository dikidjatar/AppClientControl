package com.xeg911.appclient.notification.action.command

import android.content.Context
import com.xeg911.appclient.monitoring.MonitoringController
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.shared.data.model.event.DeviceEventStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartMonitoringCommandExecutor @Inject constructor(
    private val monitoringController: MonitoringController
) : AppCommandExecutor {

    override val commandId = COMMAND_ID

    override fun execute(context: Context): ActionResult {
        val wasRunning = monitoringController.isRunning.value
        if (!wasRunning) monitoringController.start(MonitoringController.StartReason.REMOTE_COMMAND)
        return ActionResult.Completed(
            status = if (monitoringController.canStart()) DeviceEventStatus.SUCCESS else DeviceEventStatus.DENIED,
            data = mapOf("wasRunning" to wasRunning.toString()),
        )
    }

    companion object {
        const val COMMAND_ID = "start_monitoring"
    }
}
