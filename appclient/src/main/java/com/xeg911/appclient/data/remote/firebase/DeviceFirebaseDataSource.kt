package com.xeg911.appclient.data.remote.firebase

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.xeg911.appclient.core.firebase.getAwait
import com.xeg911.appclient.core.firebase.observe
import com.xeg911.appclient.core.firebase.setValueAwait
import com.xeg911.appclient.core.firebase.updateChildrenAwait
import com.xeg911.appclient.core.firebase.valueOrNull
import com.xeg911.shared.data.model.BatteryInfo
import com.xeg911.shared.data.model.CapturedNotification
import com.xeg911.shared.data.model.DeviceContact
import com.xeg911.shared.data.model.DeviceInfo
import com.xeg911.shared.data.model.HardwareInfo
import com.xeg911.shared.data.model.InstalledApp
import com.xeg911.shared.data.model.LocationSharingStatus
import com.xeg911.shared.data.model.LocationSnapshot
import com.xeg911.shared.data.model.PermissionSnapshot
import com.xeg911.shared.data.model.WallpaperSnapshot
import com.xeg911.shared.data.model.event.DeviceEvent
import com.xeg911.shared.data.model.notification.DeviceNotificationCapability
import com.xeg911.shared.data.model.usage.UsageStatsSnapshot
import com.xeg911.shared.firebase.FirebasePaths
import com.xeg911.shared.firebase.FirebasePaths.Device
import com.xeg911.shared.firebase.FirebasePaths.Index
import com.xeg911.shared.firebase.FirebasePaths.Location
import com.xeg911.shared.firebase.FirebasePaths.Usage
import com.xeg911.shared.rules.AutomationRule
import com.xeg911.shared.rules.RuleScope
import com.xeg911.shared.rules.RuleState
import com.xeg911.shared.util.toSafeFirebaseKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceFirebaseDataSource @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val root: DatabaseReference get() = database.reference

    fun deviceRef(deviceId: String): DatabaseReference =
        root.child(FirebasePaths.DEVICES).child(deviceId)

    private fun node(deviceId: String, node: String) = deviceRef(deviceId).child(node)

    private fun devicePath(deviceId: String, vararg segments: String) =
        (listOf(FirebasePaths.DEVICES, deviceId) + segments).joinToString("/")

    private fun indexPath(deviceId: String, vararg segments: String) =
        (listOf(FirebasePaths.DEVICE_INDEX, deviceId) + segments).joinToString("/")

    /**
     * Writes [value] under both trees, [indexValue] lets callers mirror a trimmed copy.
     */
    private suspend fun writeMirrored(
        deviceId: String,
        node: String,
        value: Any?,
        indexValue: Any? = value,
    ) = root.updateChildrenAwait(
        mapOf(
            devicePath(deviceId, node) to value,
            indexPath(deviceId, node) to indexValue
        )
    )

    private fun mirroredFields(
        deviceId: String,
        node: String,
        fields: Map<String, Any?>
    ) = fields.flatMap { (k, v) ->
        listOf(
            devicePath(deviceId, node, k) to v,
            indexPath(deviceId, node, k) to v
        )
    }.toMap()

    suspend fun writeInfo(deviceId: String, info: DeviceInfo) =
        writeMirrored(deviceId, Device.INFO, info)

    suspend fun writeApps(deviceId: String, apps: List<InstalledApp>) =
        node(deviceId, Device.APPS)
            .setValueAwait(apps.associateBy { it.packageName.toSafeFirebaseKey() })

    suspend fun writeContacts(deviceId: String, contacts: List<DeviceContact>) =
        node(deviceId, Device.CONTACTS)
            .setValueAwait(contacts.associateBy { it.contactId.toSafeFirebaseKey() })

    suspend fun writePermissions(
        deviceId: String,
        snapshot: PermissionSnapshot,
        missingRequired: List<String>
    ) {
        val summary = mapOf(
            FirebasePaths.Permissions.GRANTED_COUNT to snapshot.grantedCount,
            FirebasePaths.Permissions.TOTAL_COUNT to snapshot.totalCount,
            FirebasePaths.Permissions.UPDATED_AT to snapshot.updatedAt,
        )
        writeMirrored(
            deviceId, Device.PERMISSIONS,
            value = summary + (FirebasePaths.Permissions.ITEMS to snapshot.items.mapKeys { it.key.toSafeFirebaseKey() }),
            indexValue = summary + (Index.MISSING_REQUIRED to missingRequired),
        )
    }

    suspend fun writeBattery(deviceId: String, battery: BatteryInfo) = writeMirrored(
        deviceId = deviceId,
        node = Device.BATTERY,
        value = battery,
        indexValue = mapOf(
            "level" to battery.level,
            "charging" to battery.charging,
            "chargingSource" to battery.chargingSource,
            "capturedAt" to battery.capturedAt,
        ),
    )

    suspend fun writeHardware(deviceId: String, hardware: HardwareInfo) =
        node(deviceId, Device.HARDWARE).setValueAwait(hardware)

    suspend fun writeWallpaper(deviceId: String, wallpaper: WallpaperSnapshot) =
        node(deviceId, Device.WALLPAPER).setValueAwait(wallpaper)

    suspend fun writeCapability(deviceId: String, capability: DeviceNotificationCapability) =
        root.updateChildrenAwait(
            mapOf(
                devicePath(
                    deviceId,
                    Device.CAPABILITIES,
                    FirebasePaths.Capabilities.NOTIFICATION
                ) to capability,
                indexPath(deviceId, Index.FCM_TOKEN) to capability.fcmToken,
            )
        )

    suspend fun updateStatus(deviceId: String, fields: Map<String, Any?>) =
        root.updateChildrenAwait(
            mirroredFields(
                deviceId,
                Device.STATUS,
                fields + (FirebasePaths.Status.LAST_SEEN to ServerValue.TIMESTAMP)
            )
        )

    /**
     * Registers server side writes applied when this client disconnects.
     */
    fun armDisconnectStatus(deviceId: String) {
        root.onDisconnect().updateChildren(
            mirroredFields(
                deviceId,
                Device.STATUS,
                mapOf(
                    FirebasePaths.Status.ONLINE to false,
                    FirebasePaths.Status.APP_IN_FOREGROUND to false,
                    FirebasePaths.Status.MONITORING_RUNNING to false,
                    FirebasePaths.Status.LAST_SEEN to ServerValue.TIMESTAMP,
                )
            )
        )
    }

    suspend fun updateConnectivity(deviceId: String, fields: Map<String, Any?>) {
        val mirrored = fields.filterKeys { it in INDEXED_CONNECTIVITY_FIELDS }
        root.updateChildrenAwait(
            fields.mapKeys { devicePath(deviceId, Device.CONNECTIVITY, it.key) } +
                    mirrored.mapKeys { indexPath(deviceId, Device.CONNECTIVITY, it.key) }
        )
    }

    fun observePingRequest(deviceId: String): Flow<Long> =
        node(deviceId, Device.CONNECTIVITY)
            .child(FirebasePaths.Connectivity.PING_REQUEST)
            .observe()
            .map { it.valueOrNull<Long>() ?: 0L }

    fun observeConnected(): Flow<Boolean> =
        database.getReference(FirebasePaths.INFO_CONNECTED).observe()
            .map { it.valueOrNull<Boolean>() ?: false }

    suspend fun pushEvent(event: DeviceEvent) =
        node(event.deviceId, Device.EVENTS).push().setValueAwait(event)

    suspend fun writeNotification(notification: CapturedNotification) =
        node(notification.deviceId, Device.NOTIFICATIONS)
            .child(notification.id.toSafeFirebaseKey())
            .setValueAwait(notification)

    fun configRef(deviceId: String): DatabaseReference = node(deviceId, Device.CONFIG)

    suspend fun writeConfigField(deviceId: String, key: String, value: Any?) =
        configRef(deviceId).child(key).setValueAwait(value)

    fun defaultConfigRef(): DatabaseReference =
        root.child(FirebasePaths.DEFAULT_CONFIG)

    fun notificationSourcesRef(): DatabaseReference = root
        .child(FirebasePaths.NOTIFICATION_FILTER_CONFIG)
        .child(FirebasePaths.NotificationFilter.SOURCES)

    suspend fun readTelegramConfig(deviceId: String, purpose: String) =
        configRef(deviceId).child(FirebasePaths.Config.TELEGRAM).child(purpose).getAwait()

    private fun locationNode(deviceId: String) = node(deviceId, Device.LOCATION)

    suspend fun writeLocation(deviceId: String, snapshot: LocationSnapshot) {
        val location = locationNode(deviceId)
        location.child(Location.LATEST).setValueAwait(snapshot)
        val history = location.child(Location.HISTORY)
        history.push().setValueAwait(snapshot)
        trimHistory(history)
    }

    /**
     * Only the keys are needed to trim, so read the oldest overflow candidates, not the values.
     */
    private suspend fun trimHistory(history: DatabaseReference) {
        val keys = history.orderByKey().limitToFirst(Location.MAX_HISTORY + TRIM_BATCH)
            .get().await().children.mapNotNull { it.key }
        val overflow = keys.size - Location.MAX_HISTORY
        if (overflow <= 0) return
        history.updateChildrenAwait(keys.take(overflow).associateWith { null })
    }

    suspend fun writeLocationStatus(deviceId: String, status: LocationSharingStatus) =
        locationNode(deviceId).child(Location.STATUS).setValueAwait(status)

    fun observeLocationRequest(deviceId: String): Flow<Long> =
        locationNode(deviceId).child(Location.REQUEST).observe()
            .map { it.valueOrNull<Long>() ?: 0L }

    /**
     * Replaces the whole usage node so removed apps disappear, the list is already capped.
     */
    suspend fun writeUsage(deviceId: String, usage: UsageStatsSnapshot) =
        node(deviceId, Device.USAGE).setValueAwait(
            mapOf(
                Usage.SUMMARY to usage.summary,
                Usage.APPS to usage.apps.associateBy { it.packageName.toSafeFirebaseKey() },
            )
        )

    fun observeDeviceRules(): Flow<List<AutomationRule>> =
        root.child(FirebasePaths.AUTOMATION_RULES)
            .orderByChild(FirebasePaths.Rules.SCOPE)
            .equalTo(RuleScope.DEVICE.name)
            .observe()
            .map { snapshot -> snapshot.children.mapNotNull { it.valueOrNull<AutomationRule>() } }

    suspend fun writeRuleState(deviceId: String, state: RuleState) =
        node(deviceId, Device.RULE_STATE).child(state.ruleId).setValueAwait(state)

    suspend fun readRuleStates(deviceId: String): Map<String, RuleState> =
        node(deviceId, Device.RULE_STATE).getAwait().children
            .mapNotNull { it.valueOrNull<RuleState>() }
            .associateBy { it.ruleId }

    private companion object {
        const val TRIM_BATCH = 10
        val INDEXED_CONNECTIVITY_FIELDS = setOf(
            FirebasePaths.Connectivity.TRANSPORT,
            FirebasePaths.Connectivity.IP,
            FirebasePaths.Connectivity.LAST_UPDATED,
        )
    }
}
