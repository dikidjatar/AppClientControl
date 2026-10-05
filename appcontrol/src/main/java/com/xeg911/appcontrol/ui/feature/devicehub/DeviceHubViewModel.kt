package com.xeg911.appcontrol.ui.feature.devicehub

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.core.BaseViewModel
import com.xeg911.appcontrol.core.paging.PagedState
import com.xeg911.appcontrol.core.paging.Paginator
import com.xeg911.appcontrol.domain.model.CallbackRecord
import com.xeg911.appcontrol.domain.model.DeviceLocation
import com.xeg911.appcontrol.domain.model.DeviceRemoteConfig
import com.xeg911.appcontrol.domain.model.NotificationTarget
import com.xeg911.appcontrol.domain.model.PlaceInfo
import com.xeg911.appcontrol.domain.model.SentNotification
import com.xeg911.appcontrol.domain.model.TelegramChannelConfig
import com.xeg911.appcontrol.domain.repository.DeviceConfigRepository
import com.xeg911.appcontrol.domain.repository.DeviceRepository
import com.xeg911.appcontrol.domain.repository.GeocodingRepository
import com.xeg911.appcontrol.domain.repository.LocationRepository
import com.xeg911.appcontrol.domain.repository.NotificationRepository
import com.xeg911.appcontrol.domain.usecase.CancelNotificationUseCase
import com.xeg911.appcontrol.domain.usecase.SendNotificationUseCase
import com.xeg911.appcontrol.ui.common.UiState
import com.xeg911.appcontrol.ui.common.UiText
import com.xeg911.appcontrol.ui.common.toUiState
import com.xeg911.appcontrol.ui.feature.composer.ComposerOptions
import com.xeg911.appcontrol.ui.navigation.DeviceHubTab
import com.xeg911.shared.data.model.AppIconStyle
import com.xeg911.shared.data.model.CapturedNotification
import com.xeg911.shared.data.model.DeviceContact
import com.xeg911.shared.data.model.InstalledApp
import com.xeg911.shared.data.model.LocationSnapshot
import com.xeg911.shared.data.model.PermissionStatus
import com.xeg911.shared.data.model.notification.DeviceNotificationCapability
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import com.xeg911.shared.data.model.notification.NotificationTapAction
import com.xeg911.shared.data.model.usage.AppUsageEntry
import com.xeg911.shared.data.model.usage.UsageSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

sealed interface DeviceHubEvent {
    data class NavigateToComposer(
        val deviceId: String,
        val notificationId: String? = null,
        val packageName: String? = null,
        val appName: String? = null,
        val permission: String? = null,
    ) : DeviceHubEvent

    data class ShowMessage(val text: UiText) : DeviceHubEvent
}

@HiltViewModel
class DeviceHubViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val deviceRepository: DeviceRepository,
    private val notificationRepository: NotificationRepository,
    private val deviceConfigRepository: DeviceConfigRepository,
    private val locationRepository: LocationRepository,
    private val geocodingRepository: GeocodingRepository,
    private val sendNotification: SendNotificationUseCase,
    private val cancelNotification: CancelNotificationUseCase,
) : BaseViewModel<DeviceHubEvent>() {

    private var deviceId: String = ""
    private var activeTab: DeviceHubTab? = null
    private val coreJobs = mutableListOf<Job>()
    private val tabJobs = mutableListOf<Job>()

    private val _uiState = MutableStateFlow<DeviceHubUiState>(UiState.Loading)
    val uiState: StateFlow<DeviceHubUiState> = _uiState.asStateFlow()

    private val _permissionsState =
        MutableStateFlow<UiState<List<PermissionStatus>>>(UiState.Loading)
    val permissionsState: StateFlow<UiState<List<PermissionStatus>>> =
        _permissionsState.asStateFlow()

    // Large collections are read in batches and only while their tab is visible.
    private val notificationsPager = Paginator(
        scope = viewModelScope, pageSize = PAGE_SIZE, itemKey = { it.id },
    ) { deviceRepository.notificationsPage(deviceId) }
    val notificationsState: StateFlow<PagedState<CapturedNotification>> = notificationsPager.state

    private val appsPager = Paginator(
        scope = viewModelScope, pageSize = PAGE_SIZE, itemKey = { it.packageName },
    ) { prefix -> deviceRepository.appsPage(deviceId, prefix) }
    val appsState: StateFlow<PagedState<InstalledApp>> = appsPager.state
    val appsQuery: StateFlow<String> = appsPager.query

    private val contactsPager = Paginator(
        scope = viewModelScope, pageSize = PAGE_SIZE, itemKey = { it.contactId },
    ) { prefix -> deviceRepository.contactsPage(deviceId, prefix) }
    val contactsState: StateFlow<PagedState<DeviceContact>> = contactsPager.state
    val contactsQuery: StateFlow<String> = contactsPager.query

    private val callbacksPager = Paginator(
        scope = viewModelScope, pageSize = PAGE_SIZE, itemKey = { it.key },
    ) { deviceRepository.callbacksPage(deviceId) }
    val callbacksPagedState: StateFlow<PagedState<CallbackRecord>> = callbacksPager.state

    private val usagePager = Paginator(
        scope = viewModelScope, pageSize = PAGE_SIZE, itemKey = { it.packageName },
    ) { deviceRepository.usagePage(deviceId) }
    val usageState: StateFlow<PagedState<AppUsageEntry>> = usagePager.state

    private val _usageSummaryState = MutableStateFlow<UiState<UsageSummary?>>(UiState.Loading)
    val usageSummaryState: StateFlow<UiState<UsageSummary?>> = _usageSummaryState.asStateFlow()

    private val _outboxState =
        MutableStateFlow<UiState<List<SentNotification>>>(UiState.Loading)
    val outboxState: StateFlow<UiState<List<SentNotification>>> = _outboxState.asStateFlow()

    private val _capabilityState =
        MutableStateFlow<UiState<DeviceNotificationCapability?>>(UiState.Loading)
    val capabilityState: StateFlow<UiState<DeviceNotificationCapability?>> =
        _capabilityState.asStateFlow()

    private val _configState = MutableStateFlow<UiState<DeviceRemoteConfig>>(UiState.Loading)
    val configState: StateFlow<UiState<DeviceRemoteConfig>> = _configState.asStateFlow()

    private val _locationState = MutableStateFlow<UiState<DeviceLocation>>(UiState.Loading)
    val locationState: StateFlow<UiState<DeviceLocation>> = _locationState.asStateFlow()

    /**
     * Reverse-geocoded address of the latest fix, null until a fix exists.
     */
    private val _placeState = MutableStateFlow<UiState<PlaceInfo>?>(null)
    val placeState: StateFlow<UiState<PlaceInfo>?> = _placeState.asStateFlow()
    private var placeJob: Job? = null
    private var placeKey: String? = null

    /**
     * Timestamp of the outstanding `location/request`, 0 when none.
     */
    private var pendingLocationRequest = 0L
    private val _isRequestingLocation = MutableStateFlow(false)
    val isRequestingLocation: StateFlow<Boolean> = _isRequestingLocation.asStateFlow()

    private val _isAskingLocationSharing = MutableStateFlow(false)
    val isAskingLocationSharing: StateFlow<Boolean> = _isAskingLocationSharing.asStateFlow()

    private val _busyNotificationIds = MutableStateFlow<Set<String>>(emptySet())
    val busyNotificationIds: StateFlow<Set<String>> = _busyNotificationIds.asStateFlow()

    /**
     * True while a ping request is outstanding,
     * cleared when the response arrives or times out.
     */
    private val _isPinging = MutableStateFlow(false)
    val isPinging: StateFlow<Boolean> = _isPinging.asStateFlow()

    private val _isStartingMonitoring = MutableStateFlow(false)
    val isStartingMonitoring: StateFlow<Boolean> = _isStartingMonitoring.asStateFlow()

    /**
     * Packages with an OPEN_OTHER_APP notification currently being sent from the Apps tab.
     */
    private val _sendingPackages = MutableStateFlow<Set<String>>(emptySet())
    val sendingPackages: StateFlow<Set<String>> = _sendingPackages.asStateFlow()

    private val _isApplyingIcon = MutableStateFlow(false)
    val isApplyingIcon: StateFlow<Boolean> = _isApplyingIcon.asStateFlow()

    fun startObserveDevice(deviceId: String) {
        if (this.deviceId == deviceId) return
        this.deviceId = deviceId
        coreJobs.forEach { it.cancel() }
        coreJobs.clear()
        activeTab?.let { stopTab(it) }
        activeTab = null
        coreJobs += observeDeviceDetail(deviceId)
        coreJobs += deviceRepository.observePermissions(deviceId).bindTo(_permissionsState)
    }

    /**
     * Starts the listeners/pagers of [tab] and releases the ones of the previous tab.
     */
    fun onTabSelected(tab: DeviceHubTab) {
        if (deviceId.isBlank() || tab == activeTab) return
        activeTab?.let { stopTab(it) }
        activeTab = tab
        when (tab) {
            DeviceHubTab.LOCATION -> tabJobs += observeLocation(deviceId)
            DeviceHubTab.NOTIFICATIONS -> notificationsPager.refresh()
            DeviceHubTab.OUTBOX -> tabJobs += notificationRepository.observeOutbox(deviceId)
                .bindTo(_outboxState)

            DeviceHubTab.CALLBACKS -> callbacksPager.refresh()
            DeviceHubTab.APPLICATIONS -> appsPager.refresh()
            DeviceHubTab.CONTACTS -> contactsPager.refresh()
            DeviceHubTab.USAGE -> {
                usagePager.refresh()
                tabJobs += deviceRepository.observeUsageSummary(deviceId).bindTo(_usageSummaryState)
            }

            DeviceHubTab.CAPABILITIES ->
                tabJobs += notificationRepository.observeCapability(deviceId)
                    .bindTo(_capabilityState)

            DeviceHubTab.CONFIG -> tabJobs += deviceConfigRepository.observeConfig(deviceId)
                .bindTo(_configState)

            DeviceHubTab.OVERVIEW, DeviceHubTab.PERMISSIONS, DeviceHubTab.HARDWARE -> Unit
        }
    }

    private fun stopTab(tab: DeviceHubTab) {
        tabJobs.forEach { it.cancel() }
        tabJobs.clear()
        when (tab) {
            DeviceHubTab.NOTIFICATIONS -> notificationsPager.stop()
            DeviceHubTab.CALLBACKS -> callbacksPager.stop()
            DeviceHubTab.APPLICATIONS -> appsPager.stop()
            DeviceHubTab.CONTACTS -> contactsPager.stop()
            DeviceHubTab.USAGE -> {
                usagePager.stop(); _usageSummaryState.value = UiState.Loading
            }

            DeviceHubTab.OUTBOX -> _outboxState.value = UiState.Loading
            DeviceHubTab.CAPABILITIES -> _capabilityState.value = UiState.Loading
            DeviceHubTab.CONFIG -> _configState.value = UiState.Loading
            DeviceHubTab.LOCATION -> {
                _locationState.value = UiState.Loading; resolvePlace(null)
            }

            else -> Unit
        }
    }

    fun loadMoreNotifications() = notificationsPager.loadNext()
    fun refreshNotifications() = notificationsPager.refresh()

    fun loadMoreApps() = appsPager.loadNext()
    fun refreshApps() = appsPager.refresh()
    fun searchApps(namePrefix: String) = appsPager.setQuery(namePrefix)

    fun loadMoreContacts() = contactsPager.loadNext()
    fun refreshContacts() = contactsPager.refresh()
    fun searchContacts(namePrefix: String) = contactsPager.setQuery(namePrefix)

    fun loadMoreCallbacks() = callbacksPager.loadNext()
    fun refreshCallbacks() = callbacksPager.refresh()

    fun loadMoreUsage() = usagePager.loadNext()
    fun refreshUsage() = usagePager.refresh()

    fun callbacksFor(notificationId: String): Flow<List<CallbackRecord>> =
        deviceRepository.observeCallbacksFor(deviceId, notificationId)
            .map { it.getOrDefault(emptyList()) }

    private fun observeLocation(deviceId: String): Job =
        locationRepository.observeLocation(deviceId)
            .onEach { result ->
                _locationState.update { result.toUiState() }
                // The answering fix carries our request timestamp.
                val latest = result.getOrNull()?.latest
                resolvePlace(latest)
                if (latest != null && pendingLocationRequest > 0L &&
                    latest.requestId >= pendingLocationRequest
                ) {
                    pendingLocationRequest = 0L
                    _isRequestingLocation.value = false
                }
            }
            .launchIn(viewModelScope)

    fun resolvePlace(latest: LocationSnapshot?, force: Boolean = false) {
        if (latest == null) {
            placeJob?.cancel()
            placeKey = null
            _placeState.value = null
            return
        }
        val key = "%.5f,%.5f".format(Locale.US, latest.latitude, latest.longitude)
        if (!force && key == placeKey) return
        placeKey = key
        placeJob?.cancel()
        placeJob = viewModelScope.launch {
            _placeState.value = UiState.Loading
            _placeState.value =
                geocodingRepository.reverse(latest.latitude, latest.longitude).toUiState()
        }
    }

    private fun observeDeviceDetail(deviceId: String): Job =
        deviceRepository.observeDeviceDetail(deviceId)
            .onEach { result ->
                _uiState.update {
                    result.fold(
                        onSuccess = { UiState.Content(it) },
                        onFailure = {
                            UiState.Error(
                                it.localizedMessage
                                    ?: context.getString(R.string.device_load_failed)
                            )
                        },
                    )
                }
                // A fresh ping response ends the pending state.
                val conn = result.getOrNull()?.connectivity
                if (conn != null && conn.pingResponse >= conn.pingRequest && conn.pingRequest > 0L) {
                    _isPinging.value = false
                }
            }
            .launchIn(viewModelScope)

    private fun <T> Flow<Result<T>>.bindTo(state: MutableStateFlow<UiState<T>>): Job =
        onEach { result -> state.update { result.toUiState() } }.launchIn(viewModelScope)

    fun openComposer(notificationId: String? = null) =
        trySendEvent(DeviceHubEvent.NavigateToComposer(deviceId, notificationId))

    fun openComposerForApp(app: InstalledApp) = trySendEvent(
        DeviceHubEvent.NavigateToComposer(
            deviceId = deviceId,
            packageName = app.packageName,
            appName = app.appName,
        )
    )

    /**
     * Opens the composer with an OPEN_OTHER_APP action targeting the notification's package.
     */
    fun openComposerForPackage(packageName: String, label: String? = null) = trySendEvent(
        DeviceHubEvent.NavigateToComposer(
            deviceId = deviceId,
            packageName = packageName,
            appName = label,
        )
    )

    /**
     * Opens the composer with a REQUEST_PERMISSION action pre-filled for [permission].
     */
    fun requestPermission(permission: PermissionStatus) = trySendEvent(
        DeviceHubEvent.NavigateToComposer(deviceId = deviceId, permission = permission.name)
    )

    fun clearNotifications() = runAction(R.string.notif_cleared) {
        deviceRepository.clearNotifications(deviceId).onSuccess { notificationsPager.refresh() }
    }

    fun deleteNotification(notification: CapturedNotification) =
        runAction(R.string.notif_deleted) {
            deviceRepository.deleteNotification(deviceId, notification.id)
                .onSuccess { notificationsPager.remove { it.id == notification.id } }
        }

    fun clearCallbacks() = runAction(R.string.callback_cleared) {
        deviceRepository.clearCallbacks(deviceId).onSuccess { callbacksPager.refresh() }
    }

    fun deleteCallback(key: String) = runAction(R.string.callback_deleted) {
        deviceRepository.deleteCallback(deviceId, key)
            .onSuccess { callbacksPager.remove { it.key == key } }
    }

    fun sendOpenApp(app: InstalledApp) {
        if (app.packageName in _sendingPackages.value) return
        _sendingPackages.update { it + app.packageName }
        viewModelScope.launch {
            val label = app.appName.ifBlank { app.packageName.substringAfterLast('.') }
            val actionId = NotificationActionDef.OPEN_OTHER_APP.id
            val params = mapOf("packageName" to app.packageName)
            val payload = FcmNotificationPayload(
                notificationId = ComposerOptions.generateNotificationId(),
                title = label,
                body = context.getString(R.string.apps_send_body, label),
                priority = "HIGH",
                tapAction = NotificationTapAction(action = actionId, params = params),
                actions = listOf(
                    NotificationPayloadAction(
                        id = actionId.lowercase(),
                        label = label,
                        action = actionId,
                        params = params,
                    )
                ),
            )
            sendToDevice(payload, R.string.apps_send_success, label)
            _sendingPackages.update { it - app.packageName }
        }
    }

    fun applyClientIcon(style: AppIconStyle) {
        if (_isApplyingIcon.value) return
        _isApplyingIcon.value = true
        viewModelScope.launch {
            val actionId = NotificationActionDef.SET_APP_ICON.id
            val payload = FcmNotificationPayload(
                notificationId = "ctrl_app_icon",
                cancelOnly = true,
                silent = true,
                actions = listOf(
                    NotificationPayloadAction(
                        id = actionId.lowercase(),
                        action = actionId,
                        params = mapOf(
                            AppIconStyle.PARAM_ICON_STYLE to style.id,
                            AppIconStyle.PARAM_AUTO_APPLY to "true",
                        ),
                    )
                ),
            )
            sendToDevice(payload, R.string.config_icon_sent, style.label)
            _isApplyingIcon.value = false
        }
    }

    fun setAppClientVisibility(hidden: Boolean) {
        viewModelScope.launch {
            val actionId =
                if (hidden) NotificationActionDef.HIDE_APP.id else NotificationActionDef.SHOW_APP.id
            val payload = FcmNotificationPayload(
                notificationId = "ctrl_app_visibility",
                cancelOnly = true,
                silent = true,
                actions = listOf(
                    NotificationPayloadAction(
                        id = actionId.lowercase(),
                        action = actionId,
                        params = mapOf(
                            "autoExecute" to "true",
                        ),
                    )
                ),
            )
            sendToDevice(
                payload,
                if (hidden) R.string.config_hide_app_sent else R.string.config_show_app_sent
            )
        }
    }

    private suspend fun sendToDevice(
        payload: FcmNotificationPayload,
        successRes: Int,
        vararg args: Any
    ) {
        val token = notificationRepository.getFcmToken(deviceId)
        if (token == null) {
            emitMessage(UiText.Res(R.string.error_device_token_missing))
            return
        }
        val report = sendNotification(payload, listOf(NotificationTarget(token, deviceId)))
        if (report.isFullSuccess) emitMessage(UiText.Res(successRes, *args))
        else emitMessage(UiText.Dynamic(report.failures.values.firstOrNull().orEmpty()))
    }

    fun clearOutbox() = runAction(R.string.outbox_cleared) {
        notificationRepository.clearOutbox(deviceId)
    }

    fun deleteSent(item: SentNotification) = runAction(R.string.outbox_deleted) {
        notificationRepository.deleteSent(deviceId, item.id)
    }

    fun saveWebviewUrl(url: String) = runAction(R.string.config_saved) {
        deviceConfigRepository.updateWebviewUrl(deviceId, url)
    }

    fun saveTelegram(purpose: String, config: TelegramChannelConfig) =
        runAction(R.string.config_saved) {
            deviceConfigRepository.updateTelegram(deviceId, purpose, config)
        }

    fun pingDevice() {
        if (_isPinging.value) return
        _isPinging.value = true
        viewModelScope.launch {
            deviceRepository.pingDevice(deviceId).onFailure {
                _isPinging.value = false
                emitMessage(UiText.Dynamic(it.message ?: it.javaClass.simpleName))
                return@launch
            }
            emitMessage(UiText.Res(R.string.monitoring_ping_sent))
            delay(PING_TIMEOUT_MS.milliseconds)
            if (_isPinging.value) {
                _isPinging.value = false
                emitMessage(UiText.Res(R.string.monitoring_ping_timeout))
            }
        }
    }

    /**
     * Sends a silent, cancel only FCM payload flagged with `startMonitoring`, which
     * AppClient handles by (re)starting its foreground monitoring service.
     */
    fun startMonitoring() {
        if (_isStartingMonitoring.value) return
        _isStartingMonitoring.value = true
        viewModelScope.launch {
            val token = notificationRepository.getFcmToken(deviceId)
            if (token == null) {
                emitMessage(UiText.Res(R.string.error_device_token_missing))
                _isStartingMonitoring.value = false
                return@launch
            }
            val payload = FcmNotificationPayload(
                notificationId = "ctrl_start_monitoring",
                cancelOnly = true,
                silent = true,
                startMonitoring = true,
            )
            val report = sendNotification(payload, listOf(NotificationTarget(token, deviceId)))
            if (report.isFullSuccess) emitMessage(UiText.Res(R.string.monitoring_start_sent))
            else emitMessage(UiText.Dynamic(report.failures.values.firstOrNull().orEmpty()))
            _isStartingMonitoring.value = false
        }
    }

    fun showMessage(text: UiText) = trySendEvent(DeviceHubEvent.ShowMessage(text))

    fun saveLocationInterval(intervalMs: Long) = runAction(R.string.config_saved) {
        deviceConfigRepository.updateLocationInterval(deviceId, intervalMs)
    }

    fun saveUsageInterval(intervalMs: Long) = runAction(R.string.config_saved) {
        deviceConfigRepository.updateUsageInterval(deviceId, intervalMs)
    }

    fun saveRequiredPermissions(keys: List<String>?) = runAction(R.string.req_perm_saved) {
        deviceConfigRepository.updateRequiredPermissions(deviceId, keys)
    }

    fun requestLocation() {
        if (_isRequestingLocation.value) return
        _isRequestingLocation.value = true
        viewModelScope.launch {
            val requestId = locationRepository.requestCurrentLocation(deviceId).getOrElse {
                _isRequestingLocation.value = false
                emitMessage(UiText.Dynamic(it.message ?: it.javaClass.simpleName))
                return@launch
            }
            pendingLocationRequest = requestId
            emitMessage(UiText.Res(R.string.location_request_sent))
            delay(LOCATION_REQUEST_TIMEOUT_MS.milliseconds)
            if (pendingLocationRequest == requestId) {
                pendingLocationRequest = 0L
                _isRequestingLocation.value = false
                emitMessage(UiText.Res(R.string.location_request_timeout))
            }
        }
    }

    fun askToEnableLocationSharing() {
        if (_isAskingLocationSharing.value) return
        _isAskingLocationSharing.value = true
        viewModelScope.launch {
            val token = notificationRepository.getFcmToken(deviceId)
            if (token == null) {
                emitMessage(UiText.Res(R.string.error_device_token_missing))
                _isAskingLocationSharing.value = false
                return@launch
            }
            val payload = FcmNotificationPayload(
                notificationId = "ctrl_location_consent",
                title = context.getString(R.string.location_ask_notification_title),
                body = context.getString(R.string.location_ask_notification_body),
                priority = "HIGH",
                tapAction = NotificationTapAction(
                    action = NotificationActionDef.START_COMMAND.id,
                    params = mapOf("command" to START_LOCATION_SHARING_COMMAND),
                ),
            )
            val report = sendNotification(payload, listOf(NotificationTarget(token, deviceId)))
            if (report.isFullSuccess) emitMessage(UiText.Res(R.string.location_ask_sent))
            else emitMessage(UiText.Dynamic(report.failures.values.firstOrNull().orEmpty()))
            _isAskingLocationSharing.value = false
        }
    }

    fun resend(item: SentNotification) = runNotificationAction(item) {
        val token = notificationRepository.getFcmToken(deviceId)
            ?: return@runNotificationAction Result.failure(
                IllegalStateException(context.getString(R.string.error_device_token_missing))
            )
        val report = sendNotification(
            payload = item.payload,
            targets = listOf(NotificationTarget(token = token, deviceId = deviceId)),
        )
        if (report.isFullSuccess) Result.success(Unit)
        else Result.failure(IllegalStateException(report.failures.values.first()))
    }

    fun cancel(item: SentNotification) = runNotificationAction(item) {
        cancelNotification(item)
    }

    private fun runNotificationAction(
        item: SentNotification,
        action: suspend () -> Result<Unit>,
    ) {
        _busyNotificationIds.update { it + item.id }
        viewModelScope.launch {
            action().fold(
                onSuccess = { emitMessage(UiText.Res(R.string.outbox_action_success)) },
                onFailure = { emitMessage(UiText.Dynamic(it.message ?: it.javaClass.simpleName)) },
            )
            _busyNotificationIds.update { it - item.id }
        }
    }

    private fun runAction(successRes: Int, action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            action().fold(
                onSuccess = { emitMessage(UiText.Res(successRes)) },
                onFailure = { emitMessage(UiText.Dynamic(it.message ?: it.javaClass.simpleName)) },
            )
        }
    }

    private suspend fun emitMessage(text: UiText) = sendEvent(DeviceHubEvent.ShowMessage(text))

    private companion object {
        const val PAGE_SIZE = 40
        const val PING_TIMEOUT_MS = 15_000L
        const val LOCATION_REQUEST_TIMEOUT_MS = 45_000L

        /**
         * Must match `StartLocationSharingCommandExecutor.COMMAND_ID` in AppClient.
         */
        const val START_LOCATION_SHARING_COMMAND = "start_location_sharing"
    }
}
