package com.xeg911.appclient.ui.activity

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.xeg911.appclient.R
import com.xeg911.appclient.core.preference.AppPreferences
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.appclient.permission.AppPermission
import com.xeg911.appclient.permission.PermissionChecker
import com.xeg911.appclient.permission.PermissionMonitor
import com.xeg911.appclient.permission.PermissionSettingsLauncher
import com.xeg911.appclient.ui.components.AppConfirmDialog
import com.xeg911.appclient.ui.theme.AppClientTheme
import com.xeg911.shared.data.model.PermissionType
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.notification.NotificationActionDef
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PermissionRequestActivity : ComponentActivity() {

    @Inject
    lateinit var eventReporter: DeviceEventReporter

    @Inject
    lateinit var permissionMonitor: PermissionMonitor

    @Inject
    lateinit var permissionChecker: PermissionChecker

    @Inject
    lateinit var settingsLauncher: PermissionSettingsLauncher

    @Inject
    lateinit var preferences: AppPreferences

    private val permission: String by lazy { intent.getStringExtra(EXTRA_PERMISSION).orEmpty() }
    private val notificationId: String by lazy {
        intent.getStringExtra(EXTRA_NOTIFICATION_ID).orEmpty()
    }
    private val specialPermission: AppPermission? by lazy {
        AppPermission.fromManifestName(permission)?.takeIf { it.type == PermissionType.SPECIAL }
    }

    /**
     * True once the Settings screen for a special permission has been opened.
     */
    private var awaitingSettingsResult = false

    private val launcher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val permanentlyDenied = !granted && !shouldShowRequestPermissionRationale(permission)
            finishWith(granted, permanentlyDenied)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (permission.isBlank()) {
            finish(); return
        }
        if (isGranted()) {
            finishWith(granted = true, permanentlyDenied = false); return
        }
        val title = intent.getStringExtra(EXTRA_TITLE)
            ?: getString(R.string.permission_request_default_title)
        val rationale = intent.getStringExtra(EXTRA_RATIONALE)
            ?: getString(R.string.permission_request_default_rationale)

        specialPermission?.let { special ->
            showDialog(title, rationale, R.string.common_action_open_settings) {
                awaitingSettingsResult = true
                settingsLauncher.open(special)
            }
            return
        }

        lifecycleScope.launch {
            val requestedBefore = preferences.permissionRequestCount(permission) > 0
            val canShowDialog = shouldShowRequestPermissionRationale(permission)
            when {
                !requestedBefore -> request()
                canShowDialog -> showDialog(
                    title, rationale, R.string.common_action_continue
                ) { request() }

                else -> showDialog(
                    title,
                    "$rationale\n\n${getString(R.string.permission_request_permanently_denied_hint)}",
                    R.string.common_action_open_settings,
                ) {
                    settingsLauncher.openAppDetails()
                    finishWith(granted = false, permanentlyDenied = true)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Returning from a Settings screen for a special permission.
        if (awaitingSettingsResult) {
            awaitingSettingsResult = false
            finishWith(granted = isGranted(), permanentlyDenied = false)
        }
    }

    private fun isGranted(): Boolean =
        if (specialPermission != null) permissionChecker.isGranted(permission)
        else checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun request() {
        lifecycleScope.launch { preferences.incrementPermissionRequestCount(permission) }
        launcher.launch(permission)
    }

    private fun showDialog(title: String, message: String, confirmRes: Int, onConfirm: () -> Unit) {
        setContent {
            AppClientTheme {
                AppConfirmDialog(
                    title = title,
                    message = message,
                    confirmLabel = getString(confirmRes),
                    onConfirm = onConfirm,
                    onDismiss = {
                        finishWith(
                            granted = false, permanentlyDenied = false, cancelled = true
                        )
                    },
                )
            }
        }
    }

    private fun finishWith(
        granted: Boolean, permanentlyDenied: Boolean, cancelled: Boolean = false
    ) {
        permissionMonitor.refresh()
        if (notificationId.isNotBlank()) {
            eventReporter.reportAction(
                notificationId = notificationId,
                actionId = NotificationActionDef.REQUEST_PERMISSION.id,
                status = when {
                    granted -> DeviceEventStatus.SUCCESS
                    cancelled -> DeviceEventStatus.CANCELLED
                    else -> DeviceEventStatus.DENIED
                },
                data = mapOf(
                    "permission" to permission,
                    "type" to (specialPermission?.type ?: PermissionType.RUNTIME).name,
                    "granted" to granted.toString(),
                    "permanentlyDenied" to permanentlyDenied.toString(),
                ),
            )
        }
        finish()
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_PERMISSION = "extra_permission"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_RATIONALE = "extra_rationale"
    }
}
