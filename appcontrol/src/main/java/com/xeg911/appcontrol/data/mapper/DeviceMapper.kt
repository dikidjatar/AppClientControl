package com.xeg911.appcontrol.data.mapper

import com.google.firebase.database.DataSnapshot
import com.xeg911.appcontrol.core.util.DeviceNode
import com.xeg911.appcontrol.core.util.IndexField
import com.xeg911.appcontrol.core.util.getValueOrNull
import com.xeg911.appcontrol.domain.model.CallbackRecord
import com.xeg911.appcontrol.domain.model.DeviceSnapshot
import com.xeg911.shared.data.model.BatteryInfo
import com.xeg911.shared.data.model.ConnectivityInfo
import com.xeg911.shared.data.model.DeviceInfo
import com.xeg911.shared.data.model.DeviceStatus
import com.xeg911.shared.data.model.event.DeviceEvent

fun DataSnapshot.toDeviceSnapshot(): DeviceSnapshot? {
    val deviceId = key ?: return null
    val info: DeviceInfo = child(DeviceNode.NODE_INFO).getValueOrNull() ?: return null
    val status = child(DeviceNode.NODE_STATUS).getValueOrNull<DeviceStatus>() ?: DeviceStatus()
    return DeviceSnapshot(
        deviceId = deviceId,
        info = info,
        isOnline = status.online,
        lastSeen = status.lastSeen,
        battery = child(DeviceNode.NODE_BATTERY).getValueOrNull<BatteryInfo>(),
        connectivity = child(DeviceNode.NODE_CONNECTIVITY).getValueOrNull<ConnectivityInfo>(),
        status = status,
        fcmToken = child(IndexField.FIELD_FCM_TOKEN).getValueOrNull<String>().orEmpty(),
        missingRequiredPermissions = child(DeviceNode.NODE_PERMISSIONS)
            .child(IndexField.FIELD_MISSING_REQUIRED)
            .children.mapNotNull { it.getValueOrNull<String>() },
    )
}

fun DataSnapshot.toCallbackRecord(): CallbackRecord? {
    val key = key ?: return null
    return getValueOrNull<DeviceEvent>()?.let { CallbackRecord(key, it) }
}
