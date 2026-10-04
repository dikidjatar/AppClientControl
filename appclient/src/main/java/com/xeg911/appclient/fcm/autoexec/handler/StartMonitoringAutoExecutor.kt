package com.xeg911.appclient.fcm.autoexec.handler

import com.xeg911.appclient.fcm.autoexec.AutoExecuteContext
import com.xeg911.appclient.fcm.autoexec.AutoExecuteHandler
import com.xeg911.appclient.monitoring.MonitoringController
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartMonitoringAutoExecutor @Inject constructor(
    private val monitoringController: MonitoringController,
) : AutoExecuteHandler {

    override val id = "startMonitoring"
    override val order = AutoExecuteHandler.FIRST_ORDER

    override fun shouldExecute(context: AutoExecuteContext): Boolean =
        context.flag(KEY_START_MONITORING) || context.payload.startMonitoring

    override suspend fun execute(context: AutoExecuteContext) {
        monitoringController.start(MonitoringController.StartReason.REMOTE_COMMAND)
    }

    companion object {
        const val KEY_START_MONITORING = "startMonitoring"
    }
}
