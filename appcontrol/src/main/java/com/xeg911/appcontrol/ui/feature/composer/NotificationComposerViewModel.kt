package com.xeg911.appcontrol.ui.feature.composer

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.core.BaseViewModel
import com.xeg911.appcontrol.di.PrettyJson
import com.xeg911.appcontrol.domain.model.DeviceTargetOption
import com.xeg911.appcontrol.domain.model.NotificationTarget
import com.xeg911.appcontrol.domain.model.NotificationTemplate
import com.xeg911.appcontrol.domain.model.SendReport
import com.xeg911.appcontrol.domain.repository.DeviceRepository
import com.xeg911.appcontrol.domain.repository.NotificationRepository
import com.xeg911.appcontrol.domain.repository.TemplateRepository
import com.xeg911.appcontrol.domain.usecase.SendNotificationUseCase
import com.xeg911.appcontrol.ui.common.UiText
import com.xeg911.appcontrol.ui.navigation.Screen
import com.xeg911.shared.data.model.InstalledApp
import com.xeg911.shared.data.model.PermissionStatus
import com.xeg911.shared.data.model.PermissionType
import com.xeg911.shared.data.model.notification.DeviceNotificationCapability
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationStyleDef
import com.xeg911.shared.data.model.notification.SupportedActionDef
import com.xeg911.shared.data.model.notification.SupportedStyleDef
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

sealed interface ComposerEvent {
    data class ShowMessage(val text: UiText) : ComposerEvent
    data object Finished : ComposerEvent
}

data class ComposerUiState(
    val form: ComposerForm = ComposerForm(notificationId = ComposerOptions.generateNotificationId()),
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val capability: DeviceNotificationCapability? = null,
    val devices: List<DeviceTargetOption> = emptyList(),
    val selectedDeviceIds: Set<String> = emptySet(),
    val manualTokens: List<String> = emptyList(),
    val templates: List<NotificationTemplate> = emptyList(),
    val apps: List<InstalledApp> = emptyList(),
    val missingPermissions: List<PermissionPickOption> = emptyList(),
    val errors: List<UiText> = emptyList(),
    val payloadPreview: String = "",
    val pendingPick: PendingPick? = null,
) {
    val styles: List<SupportedStyleDef>
        get() = capability?.supportedStyles?.takeIf { it.isNotEmpty() }
            ?: NotificationStyleDef.entries.map {
                SupportedStyleDef(
                    it.id,
                    it.description,
                    it.usedFields
                )
            }

    val actionDefs: List<SupportedActionDef>
        get() = capability?.supportedActions?.takeIf { it.isNotEmpty() }
            ?: NotificationActionDef.entries.map {
                SupportedActionDef(it.id, it.description, it.requiredParams, it.optionalParams)
            }

    val selectedStyleFields: Set<String>
        get() = (styles.firstOrNull { it.id.equals(form.style, ignoreCase = true) }?.usedFields
            ?: NotificationStyleDef.fromId(form.style).usedFields).toSet()

    val targets: List<NotificationTarget>
        get() = devices.filter { it.deviceId in selectedDeviceIds && it.hasToken }
            .map { it.toTarget() } +
                manualTokens.map { NotificationTarget(token = it) }

    fun actionDef(actionId: String): SupportedActionDef? =
        actionDefs.firstOrNull { it.id.equals(actionId, ignoreCase = true) }
}

@HiltViewModel
class NotificationComposerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
    private val deviceRepository: DeviceRepository,
    private val notificationRepository: NotificationRepository,
    private val templateRepository: TemplateRepository,
    private val sendNotification: SendNotificationUseCase,
    @param:PrettyJson private val prettyJson: Json,
) : BaseViewModel<ComposerEvent>() {

    private val deviceId: String = savedStateHandle[Screen.ARG_DEVICE_ID] ?: ""
    private val editNotificationId: String? = savedStateHandle[Screen.ARG_NOTIFICATION_ID]
    private val presetPackageName: String? = savedStateHandle[Screen.ARG_PACKAGE_NAME]
    private val presetAppName: String? = savedStateHandle[Screen.ARG_APP_NAME]
    private val presetPermission: String? = savedStateHandle[Screen.ARG_PERMISSION]

    private var pendingTemplateId: String? = savedStateHandle[Screen.ARG_TEMPLATE_ID]

    private val _uiState = MutableStateFlow(
        ComposerUiState(
            isEditing = !editNotificationId.isNullOrBlank(),
            selectedDeviceIds = setOfNotNull(deviceId.takeIf { it.isNotBlank() }),
        )
    )
    val uiState: StateFlow<ComposerUiState> = _uiState.asStateFlow()

    /**
     * Devices the pickers should describe: the selected ones,
     * or every known device while nothing is selected.
     */
    private val targetDeviceIds: Flow<Set<String>> = _uiState
        .map { state ->
            state.selectedDeviceIds.ifEmpty {
                state.devices.map { it.deviceId }.toSet()
            }
        }
        .distinctUntilChanged()

    init {
        applyPresetApp()
        applyPresetPermission()
        refreshPreview()
        observeCapability()
        observeDevices()
        observeTemplates()
        observeApps()
        observePermissions()
        loadDraft()
    }

    /**
     * If Composer is opened from the Apps tab, autofill the form:
     */
    private fun applyPresetApp() {
        val packageName = presetPackageName?.takeIf { it.isNotBlank() } ?: return
        // If appName is empty, take the end of packageName
        val label = presetAppName?.takeIf { it.isNotBlank() }
            ?: packageName.substringAfterLast('.')

        val actionId = NotificationActionDef.OPEN_OTHER_APP.id

        _uiState.update { state ->
            state.copy(
                form = state.form.copy(
                    title = label,
                    actions = listOf(
                        ActionForm(
                            id = actionId.lowercase(),
                            label = label,
                            action = actionId,
                            params = mapOf("packageName" to packageName)
                                .toEntries(state.actionDef(actionId)),
                        )
                    ),
                )
            )
        }
    }

    /**
     * If Composer is opened from the Permissions tab, pre-fill a high-priority
     * notification whose single button asks AppClient to request [presetPermission].
     */
    private fun applyPresetPermission() {
        val permission = presetPermission?.takeIf { it.isNotBlank() } ?: return
        val shortName = permission.substringAfterLast('.')
        val actionId = NotificationActionDef.REQUEST_PERMISSION.id
        val title = context.getString(R.string.composer_preset_permission_title)
        val rationale = context.getString(R.string.composer_preset_permission_rationale, shortName)

        _uiState.update { state ->
            state.copy(
                form = state.form.copy(
                    title = title,
                    body = rationale,
                    priority = "HIGH",
                    actions = listOf(
                        ActionForm(
                            id = "request_${shortName.lowercase()}",
                            label = context.getString(R.string.composer_preset_permission_button),
                            action = actionId,
                            params = mapOf(
                                "permission" to permission,
                                "title" to title,
                                "rationale" to rationale,
                            ).toEntries(state.actionDef(actionId)),
                        )
                    ),
                )
            )
        }
    }

    private fun observeTemplates() {
        templateRepository.observeTemplates()
            .onEach { result ->
                val templates = result.getOrDefault(emptyList())
                _uiState.update { it.copy(templates = templates) }
                pendingTemplateId?.let { id ->
                    templates.firstOrNull { it.id == id }?.let { template ->
                        pendingTemplateId = null
                        loadTemplate(template)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeCapability() {
        if (deviceId.isBlank()) return
        notificationRepository.observeCapability(deviceId)
            .onEach { result -> _uiState.update { it.copy(capability = result.getOrNull()) } }
            .launchIn(viewModelScope)
    }

    private fun observeDevices() {
        notificationRepository.observeDeviceTargets()
            .onEach { result -> _uiState.update { it.copy(devices = result.getOrDefault(emptyList())) } }
            .launchIn(viewModelScope)
    }

    private fun observeApps() {
        loadPerDevice { id ->
            deviceRepository.appsPage(id).load(after = null, limit = APP_PICKER_LIMIT).items
        }
            .map { perDevice ->
                perDevice.flatten()
                    .distinctBy { it.packageName }
                    .sortedBy { it.appName.ifBlank { it.packageName }.lowercase() }
            }
            .onEach { apps -> _uiState.update { it.copy(apps = apps) } }
            .launchIn(viewModelScope)
    }

    /**
     * Requestable permissions missing on at least one target device, most widespread first.
     */
    private fun observePermissions() {
        loadPerDevice { id -> deviceRepository.observePermissions(id).first().getOrThrow() }
            .map { perDevice -> perDevice.toMissingPermissions() }
            .onEach { missing -> _uiState.update { it.copy(missingPermissions = missing) } }
            .launchIn(viewModelScope)
    }

    private fun <T> loadPerDevice(load: suspend (deviceId: String) -> List<T>): Flow<List<List<T>>> =
        targetDeviceIds.map { ids -> ids.map { id -> runCatching { load(id) }.getOrDefault(emptyList()) } }

    private fun List<List<PermissionStatus>>.toMissingPermissions(): List<PermissionPickOption> =
        asSequence().flatten()
            .filter { it.type != PermissionType.INSTALL_TIME && it.name.isNotBlank() }
            .groupBy { it.name }
            .mapNotNull { (name, statuses) ->
                val missingOn = statuses.count { !it.granted }
                if (missingOn == 0) null
                else PermissionPickOption(
                    manifestName = name,
                    label = statuses.first().simpleName.ifBlank { name.substringAfterLast('.') },
                    missingOn = missingOn,
                    deviceCount = size,
                )
            }
            .sortedWith(compareByDescending<PermissionPickOption> { it.missingOn }.thenBy { it.label })
            .toList()

    private fun loadDraft() {
        val id = editNotificationId
        if (id.isNullOrBlank() || deviceId.isBlank()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            val sent = notificationRepository.getSent(deviceId, id)
            _uiState.update { state ->
                val form = sent?.payload?.toForm(state.actionDefs)
                    ?: state.form.copy(notificationId = id)
                state.copy(form = form, isLoading = false)
            }
            refreshPreview()
            if (sent == null) sendEvent(ComposerEvent.ShowMessage(UiText.Res(R.string.composer_draft_not_found)))
        }
    }

    fun update(transform: ComposerForm.() -> ComposerForm) {
        _uiState.update { it.copy(form = it.form.transform(), errors = emptyList()) }
        refreshPreview()
    }

    fun selectTapAction(actionId: String) {
        update {
            copy(
                tapAction = actionId,
                tapParams = tapParams.reconcileWith(_uiState.value.actionDef(actionId))
            )
        }
        openAutoPicker(ParamTarget.Tap, actionId)
    }

    fun addAction() = update {
        if (actions.size >= ComposerOptions.MAX_ACTIONS) this
        else copy(actions = actions + ActionForm())
    }

    fun updateAction(uid: String, transform: ActionForm.() -> ActionForm) = update {
        copy(actions = actions.map { if (it.uid == uid) it.transform() else it })
    }

    fun selectActionType(uid: String, actionId: String) {
        updateAction(uid) {
            copy(
                action = actionId,
                id = id.ifBlank { actionId.lowercase() },
                params = params.reconcileWith(_uiState.value.actionDef(actionId)),
            )
        }
        openAutoPicker(ParamTarget.Action(uid), actionId)
    }

    private fun openAutoPicker(target: ParamTarget, actionId: String) {
        val (key, kind) = ActionUiSpec.autoPickerFor(actionId) ?: return
        _uiState.update { it.copy(pendingPick = PendingPick(target, key, kind)) }
    }

    fun dismissPick() = _uiState.update { it.copy(pendingPick = null) }

    /** Writes the picked value into the pending target, adding the param row if it is missing. */
    fun applyPick(value: String) {
        val pick = _uiState.value.pendingPick ?: return
        dismissPick()
        fun List<KeyValueEntry>.withValue(): List<KeyValueEntry> =
            if (any { it.key == pick.key }) map { if (it.key == pick.key) it.copy(value = value) else it }
            else this + KeyValueEntry(key = pick.key, value = value)
        when (val target = pick.target) {
            ParamTarget.Tap -> update { copy(tapParams = tapParams.withValue()) }
            is ParamTarget.Action -> updateAction(target.uid) { copy(params = params.withValue()) }
        }
    }

    fun removeAction(uid: String) = update { copy(actions = actions.filterNot { it.uid == uid }) }

    fun toggleDevice(deviceId: String) = _uiState.update { state ->
        val selected = if (deviceId in state.selectedDeviceIds) state.selectedDeviceIds - deviceId
        else state.selectedDeviceIds + deviceId
        state.copy(selectedDeviceIds = selected, errors = emptyList())
    }

    fun addManualToken(token: String) {
        val clean = token.trim()
        if (clean.isBlank()) return
        _uiState.update {
            it.copy(
                manualTokens = (it.manualTokens + clean).distinct(),
                errors = emptyList()
            )
        }
    }

    fun removeManualToken(token: String) =
        _uiState.update { it.copy(manualTokens = it.manualTokens - token) }

    fun regenerateId() = update { copy(notificationId = ComposerOptions.generateNotificationId()) }

    fun saveTemplate(name: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val state = _uiState.value
        val existing = state.templates.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
        val template = NotificationTemplate(
            id = existing?.id ?: "tpl_${System.currentTimeMillis().toString(36)}",
            name = cleanName,
            payload = state.form.toPayload().copy(notificationId = ""),
            updatedAt = System.currentTimeMillis(),
        )
        viewModelScope.launch {
            templateRepository.saveTemplate(template).fold(
                onSuccess = {
                    sendEvent(
                        ComposerEvent.ShowMessage(
                            UiText.Res(
                                R.string.template_saved,
                                cleanName
                            )
                        )
                    )
                },
                onFailure = {
                    sendEvent(
                        ComposerEvent.ShowMessage(
                            UiText.Dynamic(
                                it.message ?: it.javaClass.simpleName
                            )
                        )
                    )
                },
            )
        }
    }

    fun loadTemplate(template: NotificationTemplate) {
        val state = _uiState.value
        val notificationId = if (state.isEditing) state.form.notificationId
        else ComposerOptions.generateNotificationId()
        update { template.payload.toForm(state.actionDefs).copy(notificationId = notificationId) }
        viewModelScope.launch {
            sendEvent(
                ComposerEvent.ShowMessage(
                    UiText.Res(
                        R.string.template_loaded,
                        template.name
                    )
                )
            )
        }
    }

    fun deleteTemplate(template: NotificationTemplate) {
        viewModelScope.launch {
            templateRepository.deleteTemplate(template.id).onFailure {
                sendEvent(
                    ComposerEvent.ShowMessage(
                        UiText.Dynamic(
                            it.message ?: it.javaClass.simpleName
                        )
                    )
                )
            }
        }
    }

    fun send() {
        val state = _uiState.value
        val errors = ComposerValidator.validate(state.form, state.targets.isNotEmpty())
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        _uiState.update { it.copy(isSending = true) }
        viewModelScope.launch {
            val report = sendNotification(state.form.toPayload(), state.targets)
            _uiState.update { it.copy(isSending = false) }
            sendEvent(ComposerEvent.ShowMessage(report.toMessage()))
            if (report.isFullSuccess && state.isEditing) sendEvent(ComposerEvent.Finished)
        }
    }

    private fun SendReport.toMessage(): UiText = when {
        isFullSuccess -> UiText.Res(R.string.composer_send_success, delivered.size)
        delivered.isEmpty() -> UiText.Res(R.string.composer_send_failed, failures.values.first())
        else -> UiText.Res(
            R.string.composer_send_partial,
            delivered.size,
            total,
            failures.values.first()
        )
    }

    private fun refreshPreview() {
        _uiState.update { it.copy(payloadPreview = prettyJson.encodeToString(it.form.toPayload())) }
    }

    private companion object {
        const val APP_PICKER_LIMIT = 300
    }
}
