package com.xeg911.appclient.notification.action.command

import android.content.Context
import com.xeg911.appclient.location.LocationSharingController
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.shared.data.model.event.DeviceEventStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RequestLocationCommandExecutor @Inject constructor(
    private val controller: LocationSharingController,
) : AppCommandExecutor {

    override val commandId = COMMAND_ID

    override fun execute(context: Context): ActionResult {
        val requestId = System.currentTimeMillis()
        val queued = controller.requestCurrentLocation(requestId)
        return ActionResult.Completed(
            status = if (queued) DeviceEventStatus.SUCCESS else DeviceEventStatus.NOT_AVAILABLE,
            data = mapOf(
                "requestId" to requestId.toString(),
                "sharingState" to controller.status.value.state.name,
            ),
        )
    }

    companion object {
        const val COMMAND_ID = "request_location"
    }
}
