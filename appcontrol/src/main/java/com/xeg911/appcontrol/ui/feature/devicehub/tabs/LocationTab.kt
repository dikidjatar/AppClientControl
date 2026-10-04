package com.xeg911.appcontrol.ui.feature.devicehub.tabs

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.core.util.LocationField
import com.xeg911.appcontrol.domain.model.DeviceLocation
import com.xeg911.appcontrol.domain.model.PlaceInfo
import com.xeg911.appcontrol.domain.model.coordinatesLabel
import com.xeg911.appcontrol.domain.model.googleMapsUrl
import com.xeg911.appcontrol.domain.model.isActive
import com.xeg911.appcontrol.ui.common.UiState
import com.xeg911.appcontrol.ui.components.AppCard
import com.xeg911.appcontrol.ui.components.EmptyState
import com.xeg911.appcontrol.ui.components.InfoRow
import com.xeg911.appcontrol.ui.components.InfoSection
import com.xeg911.appcontrol.ui.components.LabelChip
import com.xeg911.appcontrol.ui.components.SectionHeader
import com.xeg911.appcontrol.ui.components.StatusChip
import com.xeg911.appcontrol.ui.components.StatusIndicatorRow
import com.xeg911.appcontrol.ui.components.UiStateContent
import com.xeg911.appcontrol.ui.theme.AppTheme
import com.xeg911.appcontrol.ui.util.formatDateTime
import com.xeg911.appcontrol.ui.util.formatDuration
import com.xeg911.appcontrol.ui.util.formatRelativeTime
import com.xeg911.appcontrol.ui.util.formatRelativeWithAbsolute
import com.xeg911.shared.data.model.LocationSharingState
import com.xeg911.shared.data.model.LocationSharingStatus
import com.xeg911.shared.data.model.LocationSnapshot
import com.xeg911.shared.data.model.LocationSource

@Composable
fun LocationTab(
    uiState: UiState<DeviceLocation>,
    placeState: UiState<PlaceInfo>?,
    onRetryPlace: (LocationSnapshot) -> Unit,
    deviceOnline: Boolean,
    isRequesting: Boolean,
    isAsking: Boolean,
    onRequestLocation: () -> Unit,
    onAskToEnable: () -> Unit,
    onCopied: () -> Unit,
) {
    UiStateContent(uiState = uiState, modifier = Modifier.fillMaxSize()) { location ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "status") {
                SharingStatusSection(
                    status = location.status,
                    deviceOnline = deviceOnline,
                    isRequesting = isRequesting,
                    isAsking = isAsking,
                    onRequestLocation = onRequestLocation,
                    onAskToEnable = onAskToEnable,
                )
            }
            item(key = "latest") {
                val latest = location.latest
                if (latest == null) {
                    EmptyState(
                        title = stringResource(R.string.location_empty_title),
                        subtitle = stringResource(R.string.location_empty_subtitle),
                        icon = R.drawable.location_off_24px,
                        modifier = Modifier.padding(vertical = 32.dp),
                    )
                } else {
                    LatestLocationCard(
                        latest = latest,
                        isRequesting = isRequesting,
                        onCopied = onCopied,
                    )
                    PlaceCard(
                        placeState = placeState,
                        onRetry = { onRetryPlace(latest) },
                        onCopied = onCopied,
                    )
                }
            }
            if (location.history.isNotEmpty()) {
                item(key = "history_header") {
                    SectionHeader(
                        title = stringResource(
                            R.string.location_history_title,
                            location.history.size,
                            LocationField.MAX_HISTORY,
                        )
                    )
                }
                itemsIndexed(
                    location.history,
                    key = { index, item -> "${item.capturedAt}_$index" },
                ) { _, item ->
                    HistoryRow(item)
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SharingStatusSection(
    status: LocationSharingStatus?,
    deviceOnline: Boolean,
    isRequesting: Boolean,
    isAsking: Boolean,
    onRequestLocation: () -> Unit,
    onAskToEnable: () -> Unit,
) {
    val state = status?.state ?: LocationSharingState.DISABLED
    val (stateLabelRes, stateColor) = sharingStateAppearance(state)

    InfoSection(title = stringResource(R.string.location_section_status)) {
        StatusIndicatorRow(
            label = stringResource(R.string.location_label_sharing),
            statusLabel = stringResource(stateLabelRes),
            color = stateColor,
            detail = stringResource(sharingStateHint(state)),
        )
        if (status != null) {
            StatusIndicatorRow(
                label = stringResource(R.string.location_label_consent),
                statusLabel = stringResource(
                    if (status.consentGiven) R.string.location_value_given else R.string.location_value_not_given
                ),
                color = if (status.consentGiven) AppTheme.colors.online else AppTheme.colors.offline,
                detail = if (status.consentAt > 0L) stringResource(
                    R.string.location_detail_consent_at,
                    formatRelativeWithAbsolute(status.consentAt)
                ) else null,
            )
            StatusIndicatorRow(
                label = stringResource(R.string.location_label_permission),
                statusLabel = stringResource(
                    when {
                        status.backgroundPermission -> R.string.location_value_permission_always
                        status.foregroundPermission -> R.string.location_value_permission_in_use
                        else -> R.string.location_value_permission_none
                    }
                ),
                color = if (status.foregroundPermission) AppTheme.colors.online else AppTheme.colors.error,
            )
            StatusIndicatorRow(
                label = stringResource(R.string.location_label_gps),
                statusLabel = stringResource(
                    if (status.locationServicesEnabled) R.string.location_value_gps_on else R.string.location_value_gps_off
                ),
                color = if (status.locationServicesEnabled) AppTheme.colors.online else AppTheme.colors.callbackDenied,
            )
            InfoRow(
                label = stringResource(R.string.location_label_interval),
                value = formatDuration(status.intervalMs),
            )
            InfoRow(
                label = stringResource(R.string.location_label_last_update),
                value = formatRelativeWithAbsolute(status.lastUpdateAt),
            )
            if (status.lastError.isNotBlank()) {
                InfoRow(
                    label = stringResource(R.string.location_label_last_error),
                    value = status.lastError,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onRequestLocation,
                enabled = !isRequesting && deviceOnline && status.isActive,
                modifier = Modifier.weight(1f),
            ) {
                if (isRequesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        painter = painterResource(R.drawable.my_location_24px),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.location_action_request),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            FilledTonalButton(
                onClick = onAskToEnable,
                enabled = !isAsking && !status.isActive,
                modifier = Modifier.weight(1f),
            ) {
                if (isAsking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        painter = painterResource(R.drawable.location_on_24px),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.location_action_ask_enable),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.location_actions_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun LatestLocationCard(
    latest: LocationSnapshot,
    isRequesting: Boolean,
    onCopied: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.location_latest_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                SourceChip(latest.source)
            }
            Text(
                text = latest.coordinatesLabel(),
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    clipboard.setText(AnnotatedString(latest.coordinatesLabel()))
                    onCopied()
                },
            )
            Text(
                text = if (isRequesting) stringResource(R.string.location_waiting_for_fix)
                else stringResource(
                    R.string.location_detail_fixed_at,
                    formatRelativeTime(latest.fixedAt),
                    formatDateTime(latest.fixedAt),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MetricRow(
                stringResource(R.string.location_label_accuracy),
                stringResource(R.string.location_value_meters, latest.accuracyMeters.toInt()),
            )
            MetricRow(
                stringResource(R.string.location_label_altitude),
                stringResource(R.string.location_value_meters, latest.altitudeMeters.toInt()),
            )
            MetricRow(
                stringResource(R.string.location_label_speed),
                stringResource(R.string.location_value_kmh, latest.speedMps * 3.6f),
            )
            MetricRow(
                stringResource(R.string.location_label_bearing),
                stringResource(R.string.location_value_degrees, latest.bearingDegrees.toInt()),
            )
            MetricRow(stringResource(R.string.location_label_provider), latest.provider)
            Button(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, latest.googleMapsUrl().toUri())
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.map_24px),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.location_action_open_maps),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** Reverse-geocoded address for the latest fix (OpenStreetMap Nominatim). */
@Composable
private fun PlaceCard(
    placeState: UiState<PlaceInfo>?,
    onRetry: () -> Unit,
    onCopied: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    AppCard(
        modifier = Modifier.padding(
            horizontal = AppTheme.spacing.lg,
            vertical = AppTheme.spacing.sm
        ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.location_place_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (placeState is UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onRetry, modifier = Modifier.size(AppTheme.dimens.iconXl)) {
                    Icon(
                        painter = painterResource(R.drawable.refresh_24px),
                        contentDescription = stringResource(R.string.location_place_refresh),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AppTheme.dimens.iconMd),
                    )
                }
            }
        }
        when (placeState) {
            null, is UiState.Loading -> Text(
                text = stringResource(R.string.location_place_loading),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            is UiState.Error -> Text(
                text = stringResource(R.string.location_place_error, placeState.message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )

            is UiState.Content -> {
                val place = placeState.data
                Text(
                    text = place.displayName.ifBlank { place.shortLabel },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        clipboard.setText(AnnotatedString(place.displayName))
                        onCopied()
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    if (place.type.isNotBlank()) {
                        StatusChip(
                            label = place.type.replace('_', ' '),
                            color = AppTheme.colors.info,
                        )
                    }
                    if (place.category.isNotBlank()) LabelChip(label = place.category)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                place.address.rows().forEach { (label, value) -> MetricRow(label, value) }
                if (place.osmId != 0L) {
                    MetricRow(
                        stringResource(R.string.location_place_osm),
                        "${place.osmType} ${place.osmId}"
                    )
                }
                Text(
                    text = place.licence.ifBlank { stringResource(R.string.location_place_attribution) },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun HistoryRow(item: LocationSnapshot) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, item.googleMapsUrl().toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.history_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.coordinatesLabel(),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = "${formatDateTime(item.capturedAt)} · ±${item.accuracyMeters.toInt()} m",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SourceChip(item.source)
    }
}

@Composable
private fun SourceChip(source: LocationSource) {
    StatusChip(
        label = stringResource(
            if (source == LocationSource.ON_DEMAND) R.string.location_source_on_demand
            else R.string.location_source_periodic
        ),
        color = if (source == LocationSource.ON_DEMAND) AppTheme.colors.callbackInfo else AppTheme.colors.offline,
    )
}

@Composable
private fun sharingStateAppearance(state: LocationSharingState): Pair<Int, Color> = when (state) {
    LocationSharingState.ACTIVE -> R.string.location_state_active to AppTheme.colors.online
    LocationSharingState.STARTING -> R.string.location_state_starting to AppTheme.colors.callbackDenied
    LocationSharingState.LOCATION_OFF -> R.string.location_state_gps_off to AppTheme.colors.callbackDenied
    LocationSharingState.PERMISSION_REQUIRED -> R.string.location_state_permission to AppTheme.colors.error
    LocationSharingState.DISABLED -> R.string.location_state_disabled to AppTheme.colors.offline
}

private fun sharingStateHint(state: LocationSharingState): Int = when (state) {
    LocationSharingState.ACTIVE -> R.string.location_hint_active
    LocationSharingState.STARTING -> R.string.location_hint_starting
    LocationSharingState.LOCATION_OFF -> R.string.location_hint_gps_off
    LocationSharingState.PERMISSION_REQUIRED -> R.string.location_hint_permission
    LocationSharingState.DISABLED -> R.string.location_hint_disabled
}
