package com.xeg911.appcontrol.ui.feature.templates

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.domain.model.NotificationTemplate
import com.xeg911.appcontrol.domain.usecase.ExportTemplatesUseCase
import com.xeg911.appcontrol.ui.components.AppCard
import com.xeg911.appcontrol.ui.components.AppTopBar
import com.xeg911.appcontrol.ui.components.ConfirmDialog
import com.xeg911.appcontrol.ui.components.EmptyState
import com.xeg911.appcontrol.ui.components.ErrorState
import com.xeg911.appcontrol.ui.components.FullScreenLoading
import com.xeg911.appcontrol.ui.components.IconBadge
import com.xeg911.appcontrol.ui.components.LabelChip
import com.xeg911.appcontrol.ui.components.NotificationPreview
import com.xeg911.appcontrol.ui.components.SearchField
import com.xeg911.appcontrol.ui.feature.composer.components.ComposerTextField
import com.xeg911.appcontrol.ui.feature.files.components.PrimaryActionButton
import com.xeg911.appcontrol.ui.feature.files.components.SecondaryActionButton
import com.xeg911.appcontrol.ui.feature.templates.components.ImportJsonDialog
import com.xeg911.appcontrol.ui.feature.templates.components.TemplateTransferMenu
import com.xeg911.appcontrol.ui.feature.templates.components.TransferChoice
import com.xeg911.appcontrol.ui.theme.AppTheme
import com.xeg911.appcontrol.ui.util.formatRelativeTime
import com.xeg911.appcontrol.ui.util.shareText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    onEditTemplate: (templateId: String) -> Unit,
    onCreateTemplate: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: TemplatesViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var renaming by remember { mutableStateOf<NotificationTemplate?>(null) }
    var deleting by remember { mutableStateOf<NotificationTemplate?>(null) }
    var showImportText by remember { mutableStateOf(false) }

    val openJsonFile =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(viewModel::importJsonFile)
        }
    val createJsonFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportTemplatesUseCase.MIME_JSON)
    ) { uri -> viewModel.exportToFile(uri) }

    fun onTransfer(choice: TransferChoice, templates: List<NotificationTemplate>) {
        when (choice) {
            TransferChoice.IMPORT_TEXT -> showImportText = true
            TransferChoice.IMPORT_FILE -> openJsonFile.launch(
                arrayOf(
                    ExportTemplatesUseCase.MIME_JSON,
                    "text/*"
                )
            )

            TransferChoice.EXPORT_FILE -> createJsonFile.launch(viewModel.prepareExport(templates))
            TransferChoice.EXPORT_SHARE -> viewModel.shareJson(templates)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TemplatesEvent.ShowMessage -> snackbarHostState.showSnackbar(
                    event.text.asString(
                        context
                    )
                )

                is TemplatesEvent.ShareJson -> context.shareText(
                    text = event.json,
                    subject = event.fileName,
                    mimeType = ExportTemplatesUseCase.MIME_JSON,
                )
            }
        }
    }

    if (showImportText) {
        ImportJsonDialog(
            onImport = viewModel::importJsonText,
            onDismiss = { showImportText = false },
        )
    }
    renaming?.let { template ->
        RenameDialog(
            current = template.name,
            onSave = { viewModel.rename(template, it) },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { template ->
        ConfirmDialog(
            title = stringResource(R.string.templates_delete_title),
            message = stringResource(R.string.templates_delete_message, template.name),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { deleting = null; viewModel.delete(template) },
            onDismiss = { deleting = null },
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.tool_templates),
                onNavigateBack = onNavigateBack,
                actions = {
                    TemplateTransferMenu(
                        enabled = !state.isLoading && !state.isBusy,
                        exportEnabled = state.templates.isNotEmpty(),
                        onChoice = { onTransfer(it, state.templates) },
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateTemplate,
                icon = { Icon(painterResource(R.drawable.add_24px), contentDescription = null) },
                text = { Text(stringResource(R.string.templates_new)) },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> FullScreenLoading(modifier = Modifier.padding(padding))
            state.errorMessage != null -> ErrorState(
                message = state.errorMessage.orEmpty(),
                modifier = Modifier.padding(padding),
            )

            state.templates.isEmpty() -> EmptyState(
                title = stringResource(R.string.templates_empty_title),
                subtitle = stringResource(R.string.template_empty),
                icon = R.drawable.bookmark_24px,
                modifier = Modifier.padding(padding),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                SearchField(
                    query = state.query,
                    onQueryChange = viewModel::setQuery,
                    placeholder = stringResource(R.string.templates_search_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm),
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = AppTheme.spacing.lg,
                        end = AppTheme.spacing.lg,
                        bottom = AppTheme.spacing.xxl * 2,
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                ) {
                    if (state.visible.isEmpty()) {
                        item(key = "no_match") {
                            EmptyState(
                                title = stringResource(R.string.templates_no_match),
                                subtitle = "",
                                icon = R.drawable.bookmark_24px,
                                compact = true,
                                modifier = Modifier.padding(vertical = AppTheme.spacing.xxl),
                            )
                        }
                    }
                    items(state.visible, key = { it.id }) { template ->
                        TemplateCard(
                            template = template,
                            onEdit = { onEditTemplate(template.id) },
                            onRename = { renaming = template },
                            onDuplicate = { viewModel.duplicate(template) },
                            onDelete = { deleting = template },
                            onExport = { onTransfer(it, listOf(template)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: NotificationTemplate,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onExport: (TransferChoice) -> Unit,
) {
    val payload = template.payload
    AppCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            IconBadge(iconRes = R.drawable.bookmark_24px, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                ) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    LabelChip(label = if (payload.cancelOnly) stringResource(R.string.composer_field_cancel_only) else payload.style)
                }
                Text(
                    text = stringResource(
                        R.string.template_meta,
                        payload.actions.size,
                        formatRelativeTime(template.updatedAt),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TemplateTransferMenu(
                exportOnly = true,
                onChoice = onExport,
            )
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.delete_24px),
                    contentDescription = stringResource(R.string.template_cd_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        NotificationPreview(payload = payload, compact = true)
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PrimaryActionButton(
                label = stringResource(R.string.templates_edit),
                iconRes = R.drawable.edit_24px,
                onClick = onEdit,
                modifier = Modifier,
            )
            SecondaryActionButton(
                label = stringResource(R.string.templates_rename),
                iconRes = R.drawable.bookmark_24px,
                onClick = onRename,
            )
            SecondaryActionButton(
                label = stringResource(R.string.templates_duplicate),
                iconRes = R.drawable.content_copy_24px,
                onClick = onDuplicate,
            )
        }
    }
}

@Composable
private fun RenameDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember(current) { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.templates_rename)) },
        text = {
            ComposerTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.template_name),
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && name.trim() != current,
                onClick = { onSave(name); onDismiss() },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
