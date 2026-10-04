package com.xeg911.appcontrol.ui.feature.filters

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.common.UiState
import com.xeg911.appcontrol.ui.components.AppTopBar
import com.xeg911.appcontrol.ui.components.ConfirmDialog
import com.xeg911.appcontrol.ui.components.EmptyState
import com.xeg911.appcontrol.ui.components.SectionHeader
import com.xeg911.appcontrol.ui.components.UiStateContent
import com.xeg911.appcontrol.ui.feature.filters.components.SourceCard
import com.xeg911.appcontrol.ui.feature.filters.components.SourceEditorSheet
import com.xeg911.appcontrol.ui.util.standardFadeTransition
import com.xeg911.shared.data.model.notification.NotificationSourceDef

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationFilterScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationFilterViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val sourcesState by viewModel.sources.collectAsStateWithLifecycle()
    val busyIds by viewModel.busyIds.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var editing by remember { mutableStateOf<SourceDraft?>(null) }
    var pendingDelete by remember { mutableStateOf<NotificationSourceDef?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FilterEvent.ShowMessage ->
                    snackbarHostState.showSnackbar(event.text.asString(context))
            }
        }
    }

    editing?.let { draft ->
        val existingIds =
            (sourcesState as? UiState.Content<List<NotificationSourceDef>>)?.data?.map { it.id }
                .orEmpty()
        SourceEditorSheet(
            draft = draft,
            existingIds = existingIds,
            onSave = {
                viewModel.save(it)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    pendingDelete?.let { source ->
        ConfirmDialog(
            title = stringResource(R.string.filter_confirm_delete_title, source.label),
            message = stringResource(R.string.filter_confirm_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { viewModel.delete(source) },
            onDismiss = { pendingDelete = null },
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.filter_screen_title),
                onNavigateBack = onNavigateBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = SourceDraft() },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.add_24px),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                text = { Text(stringResource(R.string.filter_add_source)) },
            )
        },
    ) { paddingValues ->
        UiStateContent(
            uiState = sourcesState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) { sources ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                SectionHeader(
                    title = stringResource(
                        R.string.filter_section_title,
                        sources.count { it.enabled },
                        sources.size,
                    ),
                )
                Text(
                    text = stringResource(R.string.filter_section_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )

                AnimatedContent(
                    targetState = sources,
                    transitionSpec = { standardFadeTransition() },
                    contentKey = { it.isEmpty() },
                    modifier = Modifier.fillMaxSize(),
                    label = "filter_sources",
                ) { list ->
                    if (list.isEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.filter_empty_title),
                            subtitle = stringResource(R.string.filter_empty_subtitle),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(list, key = { it.id }) { source ->
                                SourceCard(
                                    source = source,
                                    busy = source.id in busyIds,
                                    onToggle = { viewModel.setEnabled(source, it) },
                                    onEdit = { editing = SourceDraft.from(source) },
                                    onDelete = { pendingDelete = source },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
