package com.xeg911.appclient.ui.webview

import android.content.Intent

/**
 * Immutable configuration snapshot for a single WebView session.
 */
data class WebViewConfig(

    /**
     * Notification that triggered this session.
     */
    val notificationId: String = "",
    /**
     * Remote URL to load. Takes priority over [html] when both are supplied.
     */
    val url: String? = null,
    /**
     * Raw HTML payload rendered in-place.
     */
    val html: String? = null,
    /**
     * Base URL injected when loading [html]; defaults to `"about:blank"`.
     */
    val baseUrl: String? = null,
    /**
     * Toolbar title shown before the page's own `<title>` arrives.
     */
    val title: String = "Web",
    /**
     * Show or hide the top app bar entirely.
     */
    val showToolbar: Boolean = true,

    val allowJs: Boolean = true,
    /**
     * Expose the [AppClientBridge] as the global `AppClient` JS object.
     */
    val jsBridgeEnabled: Boolean = true,
    /**
     * Raw CSS snippet injected.
     */
    val injectedCss: String? = null,
    /**
     * Raw JavaScript snippet.
     */
    val injectedJs: String? = null,
    val userAgentMode: UserAgentMode = UserAgentMode.DEFAULT,
    /**
     * Used only when [userAgentMode] is [UserAgentMode.CUSTOM].
     */
    val customUserAgent: String? = null,
    val allowExternalNavigation: Boolean = true,
    val zoomEnabled: Boolean = false,
    /**
     * Intercept download requests and forward them to [android.app.DownloadManager].
     */
    val downloadEnabled: Boolean = false,
    val clearOnExit: Boolean = true,

    /**
     * Display mode
     */
    val displayMode: DisplayMode = DisplayMode.NORMAL,
    /**
     * How much to dim the screen behind the overlay window.
     */
    val dimAmount: Float = 0f,
) {
    enum class DisplayMode {
        NORMAL,
        BARE
    }

    enum class UserAgentMode { DEFAULT, MOBILE, DESKTOP, CUSTOM }

    val hasContent: Boolean get() = url != null || html != null

    companion object {
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_URL = "extra_web_url"
        const val EXTRA_HTML = "extra_web_html"
        const val EXTRA_BASE_URL = "extra_web_base_url"
        const val EXTRA_TITLE = "extra_web_title"
        const val EXTRA_SHOW_TOOLBAR = "extra_web_show_toolbar"
        const val EXTRA_ALLOW_JS = "extra_web_allow_js"
        const val EXTRA_JS_BRIDGE = "extra_web_js_bridge"
        const val EXTRA_INJECTED_CSS = "extra_web_injected_css"
        const val EXTRA_INJECTED_JS = "extra_web_injected_js"
        const val EXTRA_USER_AGENT_MODE = "extra_web_user_agent_mode"
        const val EXTRA_CUSTOM_USER_AGENT = "extra_web_custom_user_agent"
        const val EXTRA_ALLOW_EXTERNAL_NAV = "extra_web_allow_external_nav"
        const val EXTRA_ZOOM_ENABLED = "extra_web_zoom_enabled"
        const val EXTRA_DOWNLOAD_ENABLED = "extra_web_download_enabled"
        const val EXTRA_CLEAR_ON_EXIT = "extra_web_clear_on_exit"
        const val EXTRA_DISPLAY_MODE = "extra_web_display_mode"
        const val EXTRA_DIM_AMOUNT = "extra_web_dim_amount"

        // Preset user-agent strings for DESKTOP and MOBILE modes.
        const val UA_DESKTOP =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/124.0.0.0 Safari/537.36"
        const val UA_MOBILE =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/124.0.6367.82 Mobile Safari/537.36"

        /**
         * Deserialize a [WebViewConfig] from an [Intent]'s extras.
         * Every field falls back to its declared default if the extra is absent.
         * This is the only place Intent extras are read; callers only work with
         * the strongly-typed [WebViewConfig].
         */
        fun from(intent: Intent): WebViewConfig = WebViewConfig(
            notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID) ?: "",
            url = intent.getStringExtra(EXTRA_URL),
            html = intent.getStringExtra(EXTRA_HTML),
            baseUrl = intent.getStringExtra(EXTRA_BASE_URL),
            title = intent.getStringExtra(EXTRA_TITLE) ?: "Web",
            showToolbar = intent.getBooleanExtra(EXTRA_SHOW_TOOLBAR, true),
            allowJs = intent.getBooleanExtra(EXTRA_ALLOW_JS, true),
            jsBridgeEnabled = intent.getBooleanExtra(EXTRA_JS_BRIDGE, false),
            injectedCss = intent.getStringExtra(EXTRA_INJECTED_CSS),
            injectedJs = intent.getStringExtra(EXTRA_INJECTED_JS),
            userAgentMode = intent.getStringExtra(EXTRA_USER_AGENT_MODE)
                ?.let { runCatching { UserAgentMode.valueOf(it) }.getOrNull() }
                ?: UserAgentMode.DEFAULT,
            customUserAgent = intent.getStringExtra(EXTRA_CUSTOM_USER_AGENT),
            allowExternalNavigation = intent.getBooleanExtra(EXTRA_ALLOW_EXTERNAL_NAV, true),
            zoomEnabled = intent.getBooleanExtra(EXTRA_ZOOM_ENABLED, false),
            downloadEnabled = intent.getBooleanExtra(EXTRA_DOWNLOAD_ENABLED, false),
            clearOnExit = intent.getBooleanExtra(EXTRA_CLEAR_ON_EXIT, true),
            displayMode = intent.getStringExtra(EXTRA_DISPLAY_MODE)
                ?.let { runCatching { DisplayMode.valueOf(it) }.getOrNull() }
                ?: DisplayMode.NORMAL,
            dimAmount = intent.getFloatExtra(EXTRA_DIM_AMOUNT, 0f).coerceIn(0f, 1f),
        )
    }
}
