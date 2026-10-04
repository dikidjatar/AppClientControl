package com.xeg911.appclient.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Handles the "Stop Location" action of the persistent sharing notification.
 */
@AndroidEntryPoint
class LocationSharingActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var controller: LocationSharingController

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_STOP) controller.stopByUser()
    }

    companion object {
        const val ACTION_STOP = "com.xeg911.appclient.location.STOP"

        fun stopIntent(context: Context): Intent =
            Intent(context, LocationSharingActionReceiver::class.java).setAction(ACTION_STOP)
    }
}
