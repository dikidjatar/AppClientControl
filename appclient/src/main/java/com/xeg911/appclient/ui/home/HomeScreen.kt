package com.xeg911.appclient.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xeg911.appclient.R
import com.xeg911.appclient.permission.AppPermission
import com.xeg911.appclient.ui.activity.PermissionRequestActivity
import com.xeg911.appclient.ui.components.FullScreenLoading
import com.xeg911.appclient.ui.components.OfflineBanner
import com.xeg911.appclient.ui.components.StatusBanner
import com.xeg911.appclient.ui.permission.PermissionSetupScreen
import com.xeg911.appclient.ui.theme.Spacing
import com.xeg911.appclient.ui.webview.WebViewConfig
import com.xeg911.appclient.ui.webview.WebViewScreen

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun HomeRoute(
    onExit: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    val runtimeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.refreshPermissions() }

    // One batched request on first entry, afterward each card drives its own flow.
    var autoRequested by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState) {
        val state = uiState
        if (autoRequested || state !is HomeUiState.PermissionsRequired) return@LaunchedEffect
        autoRequested = true
        val runtime = state.missing.filter { it.isRuntime }.map { it.manifestName }
        if (runtime.isNotEmpty()) runtimeLauncher.launch(runtime.toTypedArray())
    }

    LaunchedEffect(uiState is HomeUiState.Ready) {
        if (uiState is HomeUiState.Ready) {
            viewModel.ensureMonitoringRunning()
            viewModel.promptLocationConsentIfNeeded()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            // The dedicated Offline state already explains the situation; only show the
            // slim banner on top of other content.
            val bannerVisible = !isOnline && uiState !is HomeUiState.Offline
            OfflineBanner(visible = bannerVisible)

            AnimatedContent(
                targetState = uiState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                contentKey = { it::class },
                label = "home_state",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(
                        if (bannerVisible) Modifier.consumeWindowInsets(WindowInsets.statusBars)
                        else Modifier
                    ),
            ) { state ->
                when (state) {
                    HomeUiState.Loading -> FullScreenLoading()
                    HomeUiState.Offline -> OfflineContent()
                    is HomeUiState.PermissionsRequired -> PermissionSetupScreen(
                        state = state,
                        onRequestRuntime = { permission: AppPermission ->
                            // runtimeLauncher.launch(arrayOf(permission.manifestName))
                            Intent(context, PermissionRequestActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                                putExtra(
                                    PermissionRequestActivity.EXTRA_PERMISSION,
                                    permission.manifestName
                                )
                                putExtra(
                                    PermissionRequestActivity.EXTRA_TITLE,
                                    context.getString(permission.titleRes)
                                )
                                putExtra(
                                    PermissionRequestActivity.EXTRA_RATIONALE,
                                    context.getString(permission.descriptionRes)
                                )
                                context.startActivity(this)
                            }
                        },
                        onOpenSettings = viewModel::openSettings,
                    )

                    is HomeUiState.Ready -> HomeWebView(url = state.webviewUrl, onClose = onExit)
                }
            }
        }
    }
}

@Composable
private fun OfflineContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StatusBanner(
            title = stringResource(R.string.offline_state_title),
            message = stringResource(R.string.offline_state_message),
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun HomeWebView(url: String, onClose: () -> Unit) {
    WebViewScreen(
        config = WebViewConfig(
            url = url,
            showToolbar = false,
            allowJs = true,
            clearOnExit = true,
            allowExternalNavigation = true,
        ),
        bridge = null,
        onClose = onClose,
    )
}
