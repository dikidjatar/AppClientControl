package com.xeg911.appcontrol.ui.feature.devicehub

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.common.UiState
import com.xeg911.appcontrol.ui.common.UiText
import com.xeg911.appcontrol.ui.components.AppTopBar
import com.xeg911.appcontrol.ui.components.ConfirmDialog
import com.xeg911.appcontrol.ui.components.UiStateContent
import com.xeg911.appcontrol.ui.feature.devicehub.tabs.AppsTab
import com.xeg911.appcontrol.ui.feature.devicehub.tabs.CallbacksTab
import com.xeg911.appcontrol.ui.feature.devicehub.tabs.CapabilitiesTab
import com.xeg911.appcontrol.ui.feature.devicehub.tabs.ConfigTab
import com.xeg911.appcontrol.ui.feature.devicehub.tabs.ContactsTab
import com.xeg911.appcontrol.ui.feature.devicehub.tabs.HardwareTab
import com.xeg911.appcontrol.ui.feature.devicehub.tabs.LocationTab
import com.xeg911.appcontrol.ui.feature.devicehub.tabs.NotificationsTab
import com.xeg911.appcontrol.ui.feature.devicehub.tabs.OutboxTab
import com.xeg911.appcontrol.ui.feature.devicehub.tabs.OverviewTab
import com.xeg911.appcontrol.ui.feature.devicehub.tabs.PermissionsTab
import com.xeg911.appcontrol.ui.feature.devicehub.tabs.UsageTab
import com.xeg911.appcontrol.ui.navigation.DeviceHubTab
import com.xeg911.appcontrol.ui.util.tabSlideTransition

private enum class ClearTarget(val titleRes: Int, val messageRes: Int) {
    NOTIFICATIONS(
        R.string.confirm_clear_notifications_title,
        R.string.confirm_clear_notifications_message
    ),
    CALLBACKS(R.string.confirm_clear_callbacks_title, R.string.confirm_clear_callbacks_message),
    OUTBOX(R.string.confirm_clear_outbox_title, R.string.confirm_clear_outbox_message),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceHubScreen(
    deviceId: String,
    deviceHubViewModel: DeviceHubViewModel = hiltViewModel(),
    onOpenComposer: (DeviceHubEvent.NavigateToComposer) -> Unit,
    onOpenFiles: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by deviceHubViewModel.uiState.collectAsStateWithLifecycle()
    val permissionsState by deviceHubViewModel.permissionsState.collectAsStateWithLifecycle()
    val notificationsState by deviceHubViewModel.notificationsState.collectAsStateWithLifecycle()
    val outboxState by deviceHubViewModel.outboxState.collectAsStateWithLifecycle()
    val capabilityState by deviceHubViewModel.capabilityState.collectAsStateWithLifecycle()
    val appsState by deviceHubViewModel.appsState.collectAsStateWithLifecycle()
    val appsQuery by deviceHubViewModel.appsQuery.collectAsStateWithLifecycle()
    val sendingPackages by deviceHubViewModel.sendingPackages.collectAsStateWithLifecycle()
    val contactsState by deviceHubViewModel.contactsState.collectAsStateWithLifecycle()
    val contactsQuery by deviceHubViewModel.contactsQuery.collectAsStateWithLifecycle()
    val callbacksPagedState by deviceHubViewModel.callbacksPagedState.collectAsStateWithLifecycle()
    val usageState by deviceHubViewModel.usageState.collectAsStateWithLifecycle()
    val usageSummaryState by deviceHubViewModel.usageSummaryState.collectAsStateWithLifecycle()
    val configState by deviceHubViewModel.configState.collectAsStateWithLifecycle()
    val isApplyingIcon by deviceHubViewModel.isApplyingIcon.collectAsStateWithLifecycle()
    val busyIds by deviceHubViewModel.busyNotificationIds.collectAsStateWithLifecycle()
    val isPinging by deviceHubViewModel.isPinging.collectAsStateWithLifecycle()
    val isStartingMonitoring by deviceHubViewModel.isStartingMonitoring.collectAsStateWithLifecycle()
    val locationState by deviceHubViewModel.locationState.collectAsStateWithLifecycle()
    val placeState by deviceHubViewModel.placeState.collectAsStateWithLifecycle()
    val isRequestingLocation by deviceHubViewModel.isRequestingLocation.collectAsStateWithLifecycle()
    val isAskingLocationSharing by deviceHubViewModel.isAskingLocationSharing.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var pendingClear by remember { mutableStateOf<ClearTarget?>(null) }

    LaunchedEffect(deviceId) {
        deviceHubViewModel.startObserveDevice(deviceId)
    }

    // Listeners and pagers live only while their tab is on screen.
    LaunchedEffect(deviceId, selectedTabIndex) {
        deviceHubViewModel.onTabSelected(DeviceHubTab.entries[selectedTabIndex])
    }

    LaunchedEffect(Unit) {
        deviceHubViewModel.events.collect { event ->
            when (event) {
                is DeviceHubEvent.NavigateToComposer -> onOpenComposer(event)

                is DeviceHubEvent.ShowMessage ->
                    snackbarHostState.showSnackbar(event.text.asString(context))
            }
        }
    }

    pendingClear?.let { target ->
        ConfirmDialog(
            title = stringResource(target.titleRes),
            message = stringResource(target.messageRes),
            confirmLabel = stringResource(R.string.action_clear),
            onConfirm = {
                when (target) {
                    ClearTarget.NOTIFICATIONS -> deviceHubViewModel.clearNotifications()
                    ClearTarget.CALLBACKS -> deviceHubViewModel.clearCallbacks()
                    ClearTarget.OUTBOX -> deviceHubViewModel.clearOutbox()
                }
            },
            onDismiss = { pendingClear = null },
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = when (val state = uiState) {
                    is UiState.Content ->
                        state.data.info.deviceName.ifBlank { state.data.info.model }

                    else -> stringResource(R.string.label_device)
                },
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = onOpenFiles) {
                        Icon(
                            painter = painterResource(R.drawable.swap_vert_24px),
                            contentDescription = stringResource(R.string.tool_files)
                        )
                    }
                    IconButton(onClick = { deviceHubViewModel.openComposer() }) {
                        Icon(
                            painter = painterResource(R.drawable.notification_add_24px),
                            contentDescription = stringResource(R.string.notif_compose_fab_cd)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            SecondaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                DeviceHubTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = stringResource(tab.labelRes),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTabIndex == index)
                                    FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }

            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = { tabSlideTransition(initialState, targetState) },
                modifier = Modifier.fillMaxSize(),
                label = "device_hub_tab",
            ) { tabIndex ->
                UiStateContent(
                    uiState = uiState,
                    modifier = Modifier.fillMaxSize()
                ) { device ->
                    when (DeviceHubTab.entries[tabIndex]) {
                        DeviceHubTab.OVERVIEW -> OverviewTab(
                            device = device,
                            isPinging = isPinging,
                            isStartingMonitoring = isStartingMonitoring,
                            onPing = deviceHubViewModel::pingDevice,
                            onStartMonitoring = deviceHubViewModel::startMonitoring,
                        )

                        DeviceHubTab.HARDWARE -> HardwareTab(device = device)

                        DeviceHubTab.LOCATION -> LocationTab(
                            uiState = locationState,
                            placeState = placeState,
                            onRetryPlace = { latest ->
                                deviceHubViewModel.resolvePlace(
                                    latest,
                                    force = true
                                )
                            },
                            deviceOnline = device.online,
                            isRequesting = isRequestingLocation,
                            isAsking = isAskingLocationSharing,
                            onRequestLocation = deviceHubViewModel::requestLocation,
                            onAskToEnable = deviceHubViewModel::askToEnableLocationSharing,
                            onCopied = {
                                deviceHubViewModel.showMessage(UiText.Res(R.string.location_coordinates_copied))
                            },
                        )

                        DeviceHubTab.PERMISSIONS -> PermissionsTab(
                            uiState = permissionsState,
                            onRequest = deviceHubViewModel::requestPermission,
                        )

                        DeviceHubTab.NOTIFICATIONS -> NotificationsTab(
                            state = notificationsState,
                            onLoadMore = deviceHubViewModel::loadMoreNotifications,
                            onRefresh = deviceHubViewModel::refreshNotifications,
                            onClearAll = { pendingClear = ClearTarget.NOTIFICATIONS },
                            onDelete = deviceHubViewModel::deleteNotification,
                            onComposeForPackage = deviceHubViewModel::openComposerForPackage,
                            onMessage = deviceHubViewModel::showMessage,
                        )

                        DeviceHubTab.OUTBOX -> OutboxTab(
                            uiState = outboxState,
                            busyIds = busyIds,
                            callbacksFor = deviceHubViewModel::callbacksFor,
                            onCompose = { deviceHubViewModel.openComposer() },
                            onEdit = { deviceHubViewModel.openComposer(it.id) },
                            onResend = deviceHubViewModel::resend,
                            onCancel = deviceHubViewModel::cancel,
                            onDelete = deviceHubViewModel::deleteSent,
                            onClearAll = { pendingClear = ClearTarget.OUTBOX },
                        )

                        DeviceHubTab.CALLBACKS -> CallbacksTab(
                            state = callbacksPagedState,
                            onLoadMore = deviceHubViewModel::loadMoreCallbacks,
                            onRefresh = deviceHubViewModel::refreshCallbacks,
                            onClearAll = { pendingClear = ClearTarget.CALLBACKS },
                            onDelete = deviceHubViewModel::deleteCallback,
                        )

                        DeviceHubTab.APPLICATIONS -> AppsTab(
                            state = appsState,
                            query = appsQuery,
                            sendingPackages = sendingPackages,
                            onSearch = deviceHubViewModel::searchApps,
                            onLoadMore = deviceHubViewModel::loadMoreApps,
                            onRefresh = deviceHubViewModel::refreshApps,
                            onPickForAction = deviceHubViewModel::openComposerForApp,
                            onSend = deviceHubViewModel::sendOpenApp,
                            onCopied = {
                                deviceHubViewModel.showMessage(UiText.Res(R.string.apps_package_copied))
                            },
                        )

                        DeviceHubTab.CONTACTS -> ContactsTab(
                            state = contactsState,
                            query = contactsQuery,
                            onSearch = deviceHubViewModel::searchContacts,
                            onLoadMore = deviceHubViewModel::loadMoreContacts,
                            onRefresh = deviceHubViewModel::refreshContacts,
                            onCopied = { number ->
                                deviceHubViewModel.showMessage(
                                    UiText.Res(R.string.contacts_number_copied, number)
                                )
                            },
                        )

                        DeviceHubTab.USAGE -> UsageTab(
                            summaryState = usageSummaryState,
                            state = usageState,
                            onLoadMore = deviceHubViewModel::loadMoreUsage,
                            onRefresh = deviceHubViewModel::refreshUsage,
                        )

                        DeviceHubTab.CAPABILITIES -> CapabilitiesTab(
                            uiState = capabilityState,
                            onTokenCopied = {
                                deviceHubViewModel.showMessage(UiText.Res(R.string.capability_token_copied))
                            },
                        )

                        DeviceHubTab.CONFIG -> ConfigTab(
                            uiState = configState,
                            onSaveWebviewUrl = deviceHubViewModel::saveWebviewUrl,
                            onSaveTelegram = deviceHubViewModel::saveTelegram,
                            onSaveLocationInterval = deviceHubViewModel::saveLocationInterval,
                            onSaveUsageInterval = deviceHubViewModel::saveUsageInterval,
                            onSaveRequiredPermissions = deviceHubViewModel::saveRequiredPermissions,
                            isApplyingIcon = isApplyingIcon,
                            onApplyClientIcon = deviceHubViewModel::applyClientIcon,
                            onSetAppClientVisibility = deviceHubViewModel::setAppClientVisibility,
                        )
                    }
                }
            }
        }
    }
}
