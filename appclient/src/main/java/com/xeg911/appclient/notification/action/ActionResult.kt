package com.xeg911.appclient.notification.action

import com.xeg911.shared.data.model.event.DeviceEventStatus

/**
 * Outcome of a notification action handler.
 */
sealed interface ActionResult {
    /**
     * Nothing to report (no-op or invalid params).
     */
    data object Silent : ActionResult

    /**
     * Another component (Activity, dialog) will report the final event.
     */
    data object Delegated : ActionResult

    data class Completed(
        val status: DeviceEventStatus,
        val data: Map<String, String> = emptyMap()
    ) : ActionResult
}
