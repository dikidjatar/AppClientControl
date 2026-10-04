package com.xeg911.appclient.ui.webview

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.xeg911.appclient.R
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    config: WebViewConfig,
    bridge: AppClientBridge?,
    onClose: () -> Unit,
) {
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val stateHolder = remember { mutableStateOf(WebViewState(title = config.title)) }
    val lastLoadedUrl = remember { mutableStateOf(config.url) }

    BackHandler {
        val wv = webViewRef.value
        if (wv?.canGoBack() == true) wv.goBack() else onClose()
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef.value?.let { wv ->
                if (config.clearOnExit) {
                    wv.clearCache(true)
                    wv.clearHistory()
                    WebStorage.getInstance().deleteAllData()
                    CookieManager.getInstance().removeAllCookies(null)
                }
                wv.destroy()
                webViewRef.value = null
                bridge?.webView = null
            }
        }
    }

    when (config.displayMode) {
        WebViewConfig.DisplayMode.BARE ->
            BareContent(config, bridge, webViewRef, stateHolder, lastLoadedUrl)

        WebViewConfig.DisplayMode.NORMAL ->
            NormalContent(config, bridge, webViewRef, stateHolder, lastLoadedUrl, onClose)
    }
}

@Composable
private fun BareContent(
    config: WebViewConfig,
    bridge: AppClientBridge?,
    webViewRef: MutableState<WebView?>,
    stateHolder: MutableState<WebViewState>,
    lastLoadedUrl: MutableState<String?>,
) {
    // The Box itself is transparent, the Activity window's translucent theme
    // lets whatever is behind the Activity show through any area the HTML
    // does not paint with an opaque color.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                buildWebView(ctx, config, bridge, stateHolder).also { wv ->
                    webViewRef.value = wv
                    bridge?.webView = wv
                }
            },
            update = { wv ->
                val newUrl = config.url
                if (newUrl != null && newUrl != lastLoadedUrl.value) {
                    lastLoadedUrl.value = newUrl
                    wv.loadUrl(newUrl)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NormalContent(
    config: WebViewConfig,
    bridge: AppClientBridge?,
    webViewRef: MutableState<WebView?>,
    stateHolder: MutableState<WebViewState>,
    lastLoadedUrl: MutableState<String?>,
    onClose: () -> Unit,
) {
    val state by stateHolder

    Scaffold(
        topBar = {
            if (config.showToolbar) {
                WebViewToolbar(
                    state = state,
                    onBack = {
                        val wv = webViewRef.value
                        if (wv?.canGoBack() == true) wv.goBack() else onClose()
                    },
                    onForward = { webViewRef.value?.goForward() },
                    onReload = {
                        stateHolder.value = stateHolder.value.copy(error = null)
                        webViewRef.value?.reload()
                    },
                    onClose = onClose,
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    buildWebView(ctx, config, bridge, stateHolder).also { wv ->
                        webViewRef.value = wv
                        bridge?.webView = wv
                    }
                },
                update = { wv ->
                    val newUrl = config.url
                    if (newUrl != null && newUrl != lastLoadedUrl.value) {
                        lastLoadedUrl.value = newUrl
                        wv.loadUrl(newUrl)
                    }
                }
            )

            AnimatedVisibility(
                visible = state.isLoading && state.error == null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.error != null) {
                ErrorOverlay(
                    error = state.error!!,
                    onRetry = {
                        stateHolder.value = stateHolder.value.copy(
                            error = null,
                            isLoading = true,
                            progress = 0,
                        )
                        webViewRef.value?.reload()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebViewToolbar(
    state: WebViewState,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onClose: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back_24px),
                    contentDescription = stringResource(R.string.webview_action_back),
                )
            }
        },
        title = {
            Text(
                text = state.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            IconButton(
                onClick = onForward,
                enabled = state.canGoForward,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_forward_24px),
                    contentDescription = stringResource(R.string.webview_action_forward),
                )
            }
            IconButton(
                onClick = onReload,
                enabled = !state.isLoading,
            ) {
                Icon(
                    painter = painterResource(R.drawable.refresh_24px),
                    contentDescription = stringResource(R.string.webview_action_reload),
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.close_24px),
                    contentDescription = stringResource(R.string.webview_action_close),
                )
            }
        }
    )
}

@Composable
private fun ErrorOverlay(
    error: WebViewState.PageError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.warning_24px),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.webview_error_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = error.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        error.failingUrl?.takeIf { it.isNotBlank() }?.let { url ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onRetry) { Text(stringResource(R.string.common_action_retry)) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun buildWebView(
    context: Context,
    config: WebViewConfig,
    bridge: AppClientBridge?,
    stateHolder: MutableState<WebViewState>,
): WebView = WebView(context).apply {
    if (config.displayMode == WebViewConfig.DisplayMode.BARE) {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
    }

    settings.apply {
        javaScriptEnabled = config.allowJs
        domStorageEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true

        // Lock down local file access, the hosted page cannot read the device's
        // file system or app content URIs.
        allowFileAccess = false
        allowContentAccess = false

        // Zoom controls (the native +/- overlay buttons are always hidden).
        setSupportZoom(config.zoomEnabled)
        builtInZoomControls = config.zoomEnabled
        displayZoomControls = false

        // User-agent override.
        when (config.userAgentMode) {
            WebViewConfig.UserAgentMode.DESKTOP ->
                userAgentString = WebViewConfig.UA_DESKTOP

            WebViewConfig.UserAgentMode.MOBILE ->
                userAgentString = WebViewConfig.UA_MOBILE

            WebViewConfig.UserAgentMode.CUSTOM ->
                config.customUserAgent?.let { userAgentString = it }

            WebViewConfig.UserAgentMode.DEFAULT -> { /* keep the system default */
            }
        }
    }

    bridge?.let { addJavascriptInterface(it, AppClientBridge.JS_OBJECT_NAME) }

    if (config.downloadEnabled) {
        setDownloadListener { url, userAgent, _, mimeType, _ ->
            runCatching {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val request = DownloadManager.Request(url.toUri()).apply {
                    setMimeType(mimeType)
                    addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
                    addRequestHeader("User-Agent", userAgent)
                    setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    )
                    setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        url.toUri().lastPathSegment ?: "download"
                    )
                }
                dm.enqueue(request)
            }
        }
    }

    webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            stateHolder.value = stateHolder.value.copy(
                progress = newProgress,
                isLoading = newProgress < 100,
            )
        }

        override fun onReceivedTitle(view: WebView, title: String) {
            stateHolder.value = stateHolder.value.copy(
                title = title.ifBlank { config.title }
            )
        }
    }

    webViewClient = object : WebViewClient() {
        // Reset state at the beginning of each navigation.
        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            stateHolder.value = stateHolder.value.copy(
                currentUrl = url,
                isLoading = true,
                error = null,
                progress = 0,
            )
        }

        // Update navigation flags and fire CSS/JS injection once page is ready.
        override fun onPageFinished(view: WebView, url: String?) {
            stateHolder.value = stateHolder.value.copy(
                currentUrl = url ?: stateHolder.value.currentUrl,
                isLoading = false,
                canGoBack = view.canGoBack(),
                canGoForward = view.canGoForward(),
            )
            injectContent(view, config)
        }

        // Surface main-frame errors through the error overlay.
        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError,
        ) {
            // Only main-frame failures are meaningful to show to the user;
            // sub-resource errors (ads, analytics scripts, etc.) are ignored.
            if (!request.isForMainFrame) return

            stateHolder.value = stateHolder.value.copy(
                isLoading = false,
                error = WebViewState.PageError(
                    code = error.errorCode,
                    description = error.description?.toString()
                        ?: view.context.getString(R.string.webview_error_unknown),
                    failingUrl = request.url?.toString(),
                )
            )
        }

        // Optionally redirect external-host navigations to the system browser.
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ): Boolean {
            // Feature disabled, let the WebView handle everything.
            if (config.allowExternalNavigation) return false

            // In HTML-only mode there is no initial host to compare against.
            val initialHost = config.url?.toUri()?.host ?: return false
            val requestHost = request.url?.host ?: return false

            if (requestHost == initialHost) return false

            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, request.url).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }
            return true
        }
    }

    when {
        config.url != null -> loadUrl(config.url)
        config.html != null -> loadDataWithBaseURL(
            /* baseUrl   = */ config.baseUrl,
            /* data      = */ config.html,
            /* mimeType  = */ "text/html",
            /* encoding  = */ "UTF-8",
            /* historyUrl= */ null,
        )
    }
}

internal fun injectContent(view: WebView, config: WebViewConfig) {
    config.injectedCss?.takeIf { it.isNotBlank() }?.let { css ->
        val quoted = JSONObject.quote(css)
        view.evaluateJavascript(
            """(function(){
                var s=document.createElement('style');
                s.textContent=$quoted;
                (document.head||document.documentElement).appendChild(s);
            })();""",
            null
        )
    }

    config.injectedJs?.takeIf { it.isNotBlank() }?.let { js ->
        val quoted = JSONObject.quote(js)
        view.evaluateJavascript(
            """(function(){
                try{eval($quoted);}
                catch(e){console.error('[AppClient] injectedJs error:',e);}
            })();""",
            null
        )
    }
}
