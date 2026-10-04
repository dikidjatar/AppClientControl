package com.xeg911.appclient.notification.action.command

import android.content.Context
import android.content.Intent
import com.xeg911.appclient.notification.action.ActionResult

interface AppCommandExecutor {
    val commandId: String

    /**
     * Activity to launch when the command needs user interaction, null for silent commands.
     */
    fun activityIntent(context: Context, notificationId: String): Intent? = null

    fun execute(context: Context): ActionResult
}
