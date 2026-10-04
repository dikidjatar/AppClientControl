package com.xeg911.appcontrol.ui.feature.templates

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.core.BaseViewModel
import com.xeg911.appcontrol.domain.model.NotificationTemplate
import com.xeg911.appcontrol.domain.repository.TemplateRepository
import com.xeg911.appcontrol.domain.usecase.ExportTemplatesUseCase
import com.xeg911.appcontrol.domain.usecase.ImportTemplatesUseCase
import com.xeg911.appcontrol.domain.usecase.TemplateImportReport
import com.xeg911.appcontrol.ui.common.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TemplatesEvent {
    data class ShowMessage(val text: UiText) : TemplatesEvent

    /**
     * Hand the JSON to the system share sheet.
     */
    data class ShareJson(val json: String, val fileName: String) : TemplatesEvent
}

data class TemplatesUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val templates: List<NotificationTemplate> = emptyList(),
    val query: String = "",
    val isBusy: Boolean = false,
) {
    val visible: List<NotificationTemplate>
        get() = if (query.isBlank()) templates
        else templates.filter { t ->
            t.name.contains(query, true) || t.payload.title.contains(query, true) ||
                    t.payload.body.contains(query, true)
        }
}

@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val repository: TemplateRepository,
    private val importTemplates: ImportTemplatesUseCase,
    private val exportTemplates: ExportTemplatesUseCase,
) : BaseViewModel<TemplatesEvent>() {

    private val query = MutableStateFlow("")
    private val busy = MutableStateFlow(false)

    /**
     * Templates awaiting a destination chosen through the system document picker.
     */
    private var pendingExport: List<NotificationTemplate> = emptyList()

    val uiState: StateFlow<TemplatesUiState> = combine(
        repository.observeTemplates()
            .map<Result<List<NotificationTemplate>>, Result<List<NotificationTemplate>>?> { it }
            .onStart { emit(null) },
        query,
        busy,
    ) { result, query, busy ->
        TemplatesUiState(
            isLoading = result == null,
            errorMessage = result?.exceptionOrNull()?.localizedMessage,
            templates = result?.getOrNull().orEmpty(),
            query = query,
            isBusy = busy,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TemplatesUiState())

    init {
        viewModelScope.launch {
            repository.seedDefaultsIfNeeded().onSuccess { count ->
                if (count > 0) sendEvent(
                    TemplatesEvent.ShowMessage(
                        UiText.Res(
                            R.string.templates_defaults_seeded,
                            count
                        )
                    )
                )
            }
        }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun rename(template: NotificationTemplate, newName: String) {
        val clean = newName.trim()
        if (clean.isBlank() || clean == template.name) return
        val clash =
            uiState.value.templates.any { it.id != template.id && it.name.equals(clean, true) }
        if (clash) {
            trySendEvent(
                TemplatesEvent.ShowMessage(
                    UiText.Res(
                        R.string.templates_name_taken,
                        clean
                    )
                )
            )
            return
        }
        save(
            template.copy(name = clean, updatedAt = System.currentTimeMillis()),
            R.string.templates_renamed
        )
    }

    fun duplicate(template: NotificationTemplate) {
        val names = uiState.value.templates.map { it.name.lowercase() }.toSet()
        var candidate = "${template.name} copy"
        var index = 2
        while (candidate.lowercase() in names) candidate = "${template.name} copy ${index++}"
        save(
            template.copy(
                id = "tpl_${System.currentTimeMillis().toString(36)}",
                name = candidate,
                updatedAt = System.currentTimeMillis(),
            ),
            R.string.templates_duplicated,
        )
    }

    fun delete(template: NotificationTemplate) {
        viewModelScope.launch {
            repository.deleteTemplate(template.id).fold(
                onSuccess = {
                    sendEvent(
                        TemplatesEvent.ShowMessage(
                            UiText.Res(
                                R.string.templates_deleted,
                                template.name
                            )
                        )
                    )
                },
                onFailure = {
                    sendEvent(
                        TemplatesEvent.ShowMessage(
                            UiText.Dynamic(
                                it.message ?: it.javaClass.simpleName
                            )
                        )
                    )
                },
            )
        }
    }

    fun importJsonText(text: String) =
        runImport { importTemplates.fromText(text, uiState.value.templates) }

    fun importJsonFile(uri: Uri) =
        runImport { importTemplates.fromUri(uri, uiState.value.templates) }

    fun prepareExport(templates: List<NotificationTemplate> = uiState.value.templates): String {
        pendingExport = templates
        return ExportTemplatesUseCase.fileName(templates.singleOrNull()?.name)
    }

    fun exportToFile(uri: Uri?) {
        val templates = pendingExport
        pendingExport = emptyList()
        if (uri == null || templates.isEmpty()) return
        viewModelScope.launch {
            busy.value = true
            exportTemplates.toUri(uri, templates).fold(
                onSuccess = {
                    sendEvent(
                        TemplatesEvent.ShowMessage(
                            UiText.Res(
                                R.string.templates_exported,
                                templates.size
                            )
                        )
                    )
                },
                onFailure = {
                    sendEvent(
                        TemplatesEvent.ShowMessage(
                            UiText.Dynamic(
                                it.message ?: it.javaClass.simpleName
                            )
                        )
                    )
                },
            )
            busy.value = false
        }
    }

    fun shareJson(templates: List<NotificationTemplate> = uiState.value.templates) {
        if (templates.isEmpty()) return
        trySendEvent(
            TemplatesEvent.ShareJson(
                json = exportTemplates.toJson(templates),
                fileName = ExportTemplatesUseCase.fileName(templates.singleOrNull()?.name),
            )
        )
    }

    private fun runImport(block: suspend () -> Result<TemplateImportReport>) {
        viewModelScope.launch {
            busy.value = true
            block().fold(
                onSuccess = { report ->
                    sendEvent(
                        TemplatesEvent.ShowMessage(
                            UiText.Res(
                                R.string.templates_imported,
                                report.imported,
                                report.replaced
                            )
                        )
                    )
                },
                onFailure = {
                    sendEvent(
                        TemplatesEvent.ShowMessage(
                            UiText.Res(
                                R.string.templates_import_failed,
                                it.message ?: it.javaClass.simpleName
                            )
                        )
                    )
                },
            )
            busy.value = false
        }
    }

    private fun save(template: NotificationTemplate, successRes: Int) {
        viewModelScope.launch {
            repository.saveTemplate(template).fold(
                onSuccess = {
                    sendEvent(
                        TemplatesEvent.ShowMessage(
                            UiText.Res(
                                successRes,
                                template.name
                            )
                        )
                    )
                },
                onFailure = {
                    sendEvent(
                        TemplatesEvent.ShowMessage(
                            UiText.Dynamic(
                                it.message ?: it.javaClass.simpleName
                            )
                        )
                    )
                },
            )
        }
    }
}
