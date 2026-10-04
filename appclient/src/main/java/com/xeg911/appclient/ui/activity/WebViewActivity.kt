package com.xeg911.appclient.ui.activity

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.xeg911.appclient.ui.theme.AppClientTheme
import com.xeg911.appclient.ui.webview.AppClientBridge
import com.xeg911.appclient.ui.webview.WebViewConfig
import com.xeg911.appclient.ui.webview.WebViewScreen
import dagger.hilt.EntryPoints

class WebViewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val config = WebViewConfig.from(intent)

        if (!config.hasContent) {
            finish()
            return
        }

        if (config.displayMode == WebViewConfig.DisplayMode.BARE) {
            // Apply window dim amount behind the overlay.
            // FLAG_DIM_BEHIND is not set by Theme.Translucent by default, so we
            // add it explicitly when the caller wants a dimmed backdrop.
            if (config.dimAmount > 0f) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setDimAmount(config.dimAmount)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
        }

        val bridge: AppClientBridge? = if (config.jsBridgeEnabled) {
            val eventReporter = EntryPoints
                .get(applicationContext, AppClientEntryPoint::class.java)
                .eventReporter()
            AppClientBridge(
                notificationId = config.notificationId,
                eventReporter = eventReporter,
                onClose = ::finish,
                onToast = { msg ->
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                },
            )
        } else {
            null
        }

        setContent {
            AppClientTheme {
                WebViewScreen(
                    config = config,
                    bridge = bridge,
                    onClose = ::finish,
                )
            }
        }
    }
}
