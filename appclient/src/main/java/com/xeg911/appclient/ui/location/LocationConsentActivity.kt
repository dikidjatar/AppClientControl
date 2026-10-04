package com.xeg911.appclient.ui.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.xeg911.appclient.R
import com.xeg911.appclient.core.preference.AppPreferences
import com.xeg911.appclient.domain.usecase.ObserveDeviceConfigUseCase
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.appclient.location.LocationProvider
import com.xeg911.appclient.location.LocationSharingController
import com.xeg911.appclient.notification.action.command.StartLocationSharingCommandExecutor
import com.xeg911.appclient.permission.PermissionMonitor
import com.xeg911.appclient.permission.PermissionSettingsLauncher
import com.xeg911.appclient.ui.components.AppConfirmDialog
import com.xeg911.appclient.ui.theme.AppClientTheme
import com.xeg911.shared.data.model.DeviceConfig
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.notification.NotificationActionDef
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationConsentActivity : ComponentActivity() {

    private enum class Step { WARNING, BACKGROUND_RATIONALE, PERMISSION_DENIED, LOCATION_OFF }

    @Inject
    lateinit var controller: LocationSharingController

    @Inject
    lateinit var locationProvider: LocationProvider

    @Inject
    lateinit var permissionMonitor: PermissionMonitor

    @Inject
    lateinit var settingsLauncher: PermissionSettingsLauncher

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var eventReporter: DeviceEventReporter

    @Inject
    lateinit var observeDeviceConfig: ObserveDeviceConfigUseCase

    private val notificationId: String by lazy {
        intent.getStringExtra(EXTRA_NOTIFICATION_ID).orEmpty()
    }

    private var step by mutableStateOf(Step.WARNING)
    private var intervalMs by mutableLongStateOf(DeviceConfig.DEFAULT_LOCATION_INTERVAL_MS)
    private var awaitingLocationSettings = false

    private val foregroundLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionMonitor.refresh()
        val granted = result.values.any { it }
        when {
            granted && needsBackgroundPermission() -> step = Step.BACKGROUND_RATIONALE
            granted -> checkLocationServices()
            !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ->
                step = Step.PERMISSION_DENIED

            else -> finishWith(DeviceEventStatus.DENIED, "permission_denied")
        }
    }

    private val backgroundLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Background permission is optional: sharing works from the foreground service either way.
        permissionMonitor.refresh()
        checkLocationServices()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            preferences.markLocationConsentPrompted()
            intervalMs = observeDeviceConfig().first().locationIntervalMs
        }
        if (controller.isEnabled() && controller.isRunning()) {
            finishWith(DeviceEventStatus.SUCCESS, "already_active"); return
        }
        setContent {
            AppClientTheme {
                when (step) {
                    Step.WARNING -> LocationConsentWarningDialog(
                        intervalLabel = formatInterval(intervalMs),
                        onAccept = ::requestForegroundPermission,
                        onDecline = { finishWith(DeviceEventStatus.CANCELLED, "declined") },
                    )

                    Step.BACKGROUND_RATIONALE -> AppConfirmDialog(
                        title = getString(R.string.location_background_title),
                        message = getString(R.string.location_background_message),
                        confirmLabel = getString(R.string.common_action_continue),
                        dismissLabel = getString(R.string.location_action_skip),
                        onConfirm = {
                            backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        },
                        onDismiss = ::checkLocationServices,
                    )

                    Step.PERMISSION_DENIED -> AppConfirmDialog(
                        title = getString(R.string.location_permission_denied_title),
                        message = getString(R.string.location_permission_denied_message),
                        confirmLabel = getString(R.string.common_action_open_settings),
                        onConfirm = {
                            settingsLauncher.openAppDetails()
                            finishWith(DeviceEventStatus.DENIED, "permanently_denied")
                        },
                        onDismiss = { finishWith(DeviceEventStatus.DENIED, "permanently_denied") },
                    )

                    Step.LOCATION_OFF -> AppConfirmDialog(
                        title = getString(R.string.location_gps_off_title),
                        message = getString(R.string.location_gps_off_message),
                        confirmLabel = getString(R.string.location_action_enable_location),
                        dismissLabel = getString(R.string.location_action_not_now),
                        onConfirm = {
                            awaitingLocationSettings = true
                            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        },
                        // Consent was given; the service will show "location off" until GPS is on.
                        onDismiss = ::activateSharing,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (awaitingLocationSettings) {
            awaitingLocationSettings = false
            activateSharing()
        }
    }

    private fun requestForegroundPermission() {
        if (locationProvider.hasForegroundPermission()) {
            if (needsBackgroundPermission()) step = Step.BACKGROUND_RATIONALE
            else checkLocationServices()
            return
        }
        foregroundLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }

    private fun needsBackgroundPermission(): Boolean =
        !locationProvider.hasBackgroundPermission()

    private fun checkLocationServices() {
        if (locationProvider.isLocationEnabled()) activateSharing()
        else step = Step.LOCATION_OFF
    }

    private fun activateSharing() {
        controller.grantConsentAndStart()
        finishWith(
            DeviceEventStatus.SUCCESS,
            if (locationProvider.isLocationEnabled()) "started" else "started_location_off",
        )
    }

    private fun finishWith(status: DeviceEventStatus, outcome: String) {
        if (notificationId.isNotBlank()) {
            eventReporter.reportAction(
                notificationId = notificationId,
                actionId = NotificationActionDef.START_COMMAND.id,
                status = status,
                data = mapOf(
                    "command" to StartLocationSharingCommandExecutor.COMMAND_ID,
                    "outcome" to outcome,
                ),
            )
        }
        finish()
    }

    private fun formatInterval(ms: Long): String {
        val minutes = ms / 60_000
        return if (minutes >= 1) resources.getQuantityString(
            R.plurals.location_interval_minutes, minutes.toInt(), minutes.toInt()
        ) else getString(R.string.location_interval_seconds, ms / 1_000)
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        fun intent(context: Context, notificationId: String? = null): Intent =
            Intent(context, LocationConsentActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId)
    }
}
