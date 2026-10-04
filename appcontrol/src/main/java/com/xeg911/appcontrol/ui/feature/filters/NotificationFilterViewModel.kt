package com.xeg911.appcontrol.ui.feature.filters

import androidx.lifecycle.viewModelScope
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.core.BaseViewModel
import com.xeg911.appcontrol.domain.repository.NotificationFilterRepository
import com.xeg911.appcontrol.ui.common.UiState
import com.xeg911.appcontrol.ui.common.UiText
import com.xeg911.appcontrol.ui.common.toUiState
import com.xeg911.shared.data.model.notification.NotificationSourceDef
import com.xeg911.shared.util.toSafeFirebaseKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FilterEvent {
    data class ShowMessage(val text: UiText) : FilterEvent
}

data class SourceDraft(
    val id: String = "",
    val isNew: Boolean = true,
    val label: String = "",
    val enabled: Boolean = true,
    val headerEmoji: String = NotificationSourceDef().headerEmoji,
    val fromEmoji: String = NotificationSourceDef().fromEmoji,
    val packages: List<String> = emptyList(),
    val usesDefaultSms: Boolean = false,
    val noisePatterns: List<String> = emptyList(),
) {
    val isValid: Boolean
        get() = id.isNotBlank() && label.isNotBlank() &&
                (usesDefaultSms || packages.any { it.isNotBlank() })

    fun toSourceDef() = NotificationSourceDef(
        id = id,
        label = label.trim(),
        enabled = enabled,
        headerEmoji = headerEmoji.ifBlank { NotificationSourceDef().headerEmoji },
        fromEmoji = fromEmoji.ifBlank { NotificationSourceDef().fromEmoji },
        packages = packages,
        usesDefaultSms = usesDefaultSms,
        noisePatterns = noisePatterns,
    )

    companion object {
        fun from(source: NotificationSourceDef) = SourceDraft(
            id = source.id,
            isNew = false,
            label = source.label,
            enabled = source.enabled,
            headerEmoji = source.headerEmoji,
            fromEmoji = source.fromEmoji,
            packages = source.packages,
            usesDefaultSms = source.usesDefaultSms,
            noisePatterns = source.noisePatterns,
        )

        fun idFromLabel(label: String): String =
            label.trim().lowercase().replace(Regex("\\s+"), "_").toSafeFirebaseKey()
    }
}

@HiltViewModel
class NotificationFilterViewModel @Inject constructor(
    private val repository: NotificationFilterRepository,
) : BaseViewModel<FilterEvent>() {

    private val _sources = MutableStateFlow<UiState<List<NotificationSourceDef>>>(UiState.Loading)
    val sources: StateFlow<UiState<List<NotificationSourceDef>>> = _sources.asStateFlow()

    private val _busyIds = MutableStateFlow<Set<String>>(emptySet())
    val busyIds: StateFlow<Set<String>> = _busyIds.asStateFlow()

    init {
        repository.observeSources()
            .onEach { result -> _sources.update { result.toUiState() } }
            .launchIn(viewModelScope)
    }

    fun save(draft: SourceDraft) {
        if (!draft.isValid) {
            trySendEvent(FilterEvent.ShowMessage(UiText.Res(R.string.filter_error_invalid)))
            return
        }
        runBusy(draft.id, R.string.filter_saved) { repository.upsertSource(draft.toSourceDef()) }
    }

    fun setEnabled(source: NotificationSourceDef, enabled: Boolean) =
        runBusy(
            id = source.id,
            successRes = if (enabled) R.string.filter_enabled_msg else R.string.filter_disabled_msg,
        ) {
            repository.setEnabled(source.id, enabled)
        }

    fun delete(source: NotificationSourceDef) = runBusy(source.id, R.string.filter_deleted) {
        repository.deleteSource(source.id)
    }

    private fun runBusy(
        id: String,
        successRes: Int, action: suspend () -> Result<Unit>
    ) {
        _busyIds.update { it + id }
        viewModelScope.launch {
            action().fold(
                onSuccess = { sendEvent(FilterEvent.ShowMessage(UiText.Res(successRes))) },
                onFailure = {
                    sendEvent(
                        FilterEvent.ShowMessage(
                            UiText.Dynamic(
                                it.message ?: it.javaClass.simpleName
                            )
                        )
                    )
                },
            )
            _busyIds.update { it - id }
        }
    }
}
