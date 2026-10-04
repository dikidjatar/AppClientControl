package com.xeg911.appcontrol.domain.model

import com.xeg911.shared.data.model.event.DeviceEvent

data class CallbackRecord(
    val key: String,
    val event: DeviceEvent,
)
