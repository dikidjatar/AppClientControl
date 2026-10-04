package com.xeg911.appclient.ui.webview

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.notification.NotificationActionDef
import org.json.JSONObject

class AppClientBridge(
    private val notificationId: String,
    private val eventReporter: DeviceEventReporter,
    private val onClose: () -> Unit,
    private val onToast: (String) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var webView: WebView? = null

    @JavascriptInterface
    fun getVersion(): Int = BRIDGE_VERSION

    @JavascriptInterface
    fun getNotificationId(): String = notificationId

    @JavascriptInterface
    fun getActionId(): String = NotificationActionDef.SHOW_WEB_PAGE.id

    @JavascriptInterface
    fun close() = mainHandler.post(onClose)

    @JavascriptInterface
    fun navigate(url: String) = mainHandler.post { webView?.loadUrl(url) }

    @JavascriptInterface
    fun toast(message: String) = mainHandler.post {
        onToast(message.take(MAX_TOAST_LENGTH))
    }

    @JavascriptInterface
    @JvmOverloads
    fun callback(status: String = "SUCCESS", dataJson: String = "{}") {
        val cbStatus = DeviceEventStatus.entries
            .firstOrNull { it.name.equals(status.trim(), ignoreCase = true) }
            ?: DeviceEventStatus.SUCCESS

        val dataMap: Map<String, String> = try {
            val json = JSONObject(dataJson)
            buildMap { json.keys().forEach { key -> put(key, json.optString(key)) } }
        } catch (_: Exception) {
            mapOf("raw" to dataJson)
        }

        eventReporter.reportAction(
            notificationId = notificationId,
            actionId = NotificationActionDef.SHOW_WEB_PAGE.id,
            status = cbStatus,
            data = dataMap,
        )
    }

    companion object {
        const val BRIDGE_VERSION = 2
        const val JS_OBJECT_NAME = "AppClient"
        private const val MAX_TOAST_LENGTH = 200
    }
}
