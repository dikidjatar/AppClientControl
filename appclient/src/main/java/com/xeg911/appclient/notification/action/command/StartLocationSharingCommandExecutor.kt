package com.xeg911.appclient.notification.action.command

import android.content.Context
import android.content.Intent
import com.xeg911.appclient.location.LocationSharingController
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.appclient.ui.location.LocationConsentActivity
import com.xeg911.shared.data.model.event.DeviceEventStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartLocationSharingCommandExecutor @Inject constructor(
    private val controller: LocationSharingController,
) : AppCommandExecutor {

    override val commandId = COMMAND_ID

    override fun activityIntent(context: Context, notificationId: String): Intent =
        LocationConsentActivity.intent(context, notificationId)

    override fun execute(context: Context): ActionResult {
        if (controller.isEnabled() && controller.isRunning()) {
            return ActionResult.Completed(
                DeviceEventStatus.SUCCESS,
                mapOf("outcome" to "already_active")
            )
        }
        runCatching { context.startActivity(activityIntent(context, "")) }
        return ActionResult.Delegated
    }

    companion object {
        const val COMMAND_ID = "start_location_sharing"
    }
}
