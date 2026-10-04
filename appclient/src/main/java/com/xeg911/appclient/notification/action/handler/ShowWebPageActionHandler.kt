package com.xeg911.appclient.notification.action.handler

import android.content.Context
import android.content.Intent
import android.util.Log
import com.xeg911.appclient.notification.action.ActionResult
import com.xeg911.appclient.notification.action.NotificationActionHandler
import com.xeg911.appclient.ui.activity.WebViewActivity
import com.xeg911.appclient.ui.webview.WebViewConfig
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.notification.NotificationPayloadAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowWebPageActionHandler @Inject constructor() : NotificationActionHandler {

    override val actionId: String = NotificationActionDef.SHOW_WEB_PAGE.id

    override fun buildActivityIntent(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload,
    ): Intent? {
        val params = action.params
        val url = params[PARAM_URL]?.takeIf { it.isNotBlank() }
        val html = params[PARAM_HTML]?.takeIf { it.isNotBlank() }

        if (url == null && html == null) {
            return null
        }

        return Intent(context, WebViewActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS

            putExtra(WebViewConfig.EXTRA_NOTIFICATION_ID, payload.notificationId)

            url?.let { putExtra(WebViewConfig.EXTRA_URL, it) }
            html?.let { putExtra(WebViewConfig.EXTRA_HTML, it) }
            params[PARAM_BASE_URL]?.let { putExtra(WebViewConfig.EXTRA_BASE_URL, it) }
            params[PARAM_TITLE]?.let { putExtra(WebViewConfig.EXTRA_TITLE, it) }
            params[PARAM_SHOW_TOOLBAR]?.let {
                putExtra(WebViewConfig.EXTRA_SHOW_TOOLBAR, it != "false")
            }
            params[PARAM_DISPLAY_MODE]?.let { raw ->
                val mode = when (raw.lowercase().trim()) {
                    "bare" -> WebViewConfig.DisplayMode.BARE
                    "normal" -> WebViewConfig.DisplayMode.NORMAL
                    else -> {
                        WebViewConfig.DisplayMode.NORMAL
                    }
                }
                putExtra(WebViewConfig.EXTRA_DISPLAY_MODE, mode.name)
            }
            params[PARAM_DIM_AMOUNT]?.toFloatOrNull()?.let {
                putExtra(WebViewConfig.EXTRA_DIM_AMOUNT, it.coerceIn(0f, 1f))
            }

            // JavaScript / bridge
            params[PARAM_ALLOW_JS]?.let { putExtra(WebViewConfig.EXTRA_ALLOW_JS, it != "false") }
            params[PARAM_JS_BRIDGE]?.let { putExtra(WebViewConfig.EXTRA_JS_BRIDGE, it == "true") }
            params[PARAM_INJECTED_CSS]?.let { putExtra(WebViewConfig.EXTRA_INJECTED_CSS, it) }
            params[PARAM_INJECTED_JS]?.let { putExtra(WebViewConfig.EXTRA_INJECTED_JS, it) }

            // User-agent
            // "default" | "mobile" | "desktop". Stored as the enum name.
            // Anything else is treated as a raw custom UA string.
            params[PARAM_USER_AGENT]?.let { userAgent ->
                when (userAgent.lowercase().trim()) {
                    "default" -> putExtra(
                        WebViewConfig.EXTRA_USER_AGENT_MODE,
                        WebViewConfig.UserAgentMode.DEFAULT.name
                    )

                    "mobile" -> putExtra(
                        WebViewConfig.EXTRA_USER_AGENT_MODE, WebViewConfig.UserAgentMode.MOBILE.name
                    )

                    "desktop" -> putExtra(
                        WebViewConfig.EXTRA_USER_AGENT_MODE,
                        WebViewConfig.UserAgentMode.DESKTOP.name
                    )

                    else -> {
                        putExtra(
                            WebViewConfig.EXTRA_USER_AGENT_MODE,
                            WebViewConfig.UserAgentMode.CUSTOM.name
                        )
                        putExtra(WebViewConfig.EXTRA_CUSTOM_USER_AGENT, userAgent)
                    }
                }
            }

            params[PARAM_ALLOW_EXTERNAL_NAV]?.let {
                putExtra(WebViewConfig.EXTRA_ALLOW_EXTERNAL_NAV, it != "false")
            }
            params[PARAM_ZOOM_ENABLED]?.let {
                putExtra(WebViewConfig.EXTRA_ZOOM_ENABLED, it == "true")
            }
            params[PARAM_DOWNLOAD_ENABLED]?.let {
                putExtra(WebViewConfig.EXTRA_DOWNLOAD_ENABLED, it == "true")
            }
            params[PARAM_CLEAR_ON_EXIT]?.let {
                putExtra(WebViewConfig.EXTRA_CLEAR_ON_EXIT, it != "false")
            }
        }
    }

    override fun handle(
        context: Context,
        action: NotificationPayloadAction,
        payload: FcmNotificationPayload,
        replyText: String?,
    ): ActionResult {
        val intent = buildActivityIntent(context, action, payload) ?: return ActionResult.Completed(
            status = DeviceEventStatus.NOT_AVAILABLE,
            data = mapOf("error" to "Missing 'url' or 'html' param"),
        )

        runCatching { context.startActivity(intent) }.onFailure {
            Log.e(
                TAG, "Failed to start WebViewActivity", it
            )
        }

        // WebViewActivity (via AppClientBridge) sends its own callback when
        // the JS page calls AppClient.callback(), so we delegate here.
        return ActionResult.Delegated
    }

    private companion object {
        private const val TAG = "ShowWebPageActionHandler"

        private const val PARAM_URL = "url"
        private const val PARAM_HTML = "html"
        private const val PARAM_BASE_URL = "baseUrl"
        private const val PARAM_TITLE = "title"
        private const val PARAM_SHOW_TOOLBAR = "showToolbar"
        private const val PARAM_ALLOW_JS = "allowJs"
        private const val PARAM_JS_BRIDGE = "jsBridge"
        private const val PARAM_INJECTED_CSS = "injectedCss"
        private const val PARAM_INJECTED_JS = "injectedJs"
        private const val PARAM_USER_AGENT = "userAgent"
        private const val PARAM_ALLOW_EXTERNAL_NAV = "allowExternalNav"
        private const val PARAM_ZOOM_ENABLED = "zoomEnabled"
        private const val PARAM_DOWNLOAD_ENABLED = "downloadEnabled"
        private const val PARAM_CLEAR_ON_EXIT = "clearOnExit"
        private const val PARAM_DISPLAY_MODE = "displayMode"
        private const val PARAM_DIM_AMOUNT = "dimAmount"
    }
}
