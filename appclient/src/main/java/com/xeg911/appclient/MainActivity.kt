package com.xeg911.appclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.xeg911.appclient.monitoring.MonitoringLifecycleManager
import com.xeg911.appclient.ui.home.HomeRoute
import com.xeg911.appclient.ui.theme.AppClientTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var monitoringLifecycleManager: MonitoringLifecycleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The main screen is the only UI entry allowed to start monitoring.
        monitoringLifecycleManager.bindMainScreen(lifecycle)
        setContent {
            AppClientTheme {
                HomeRoute(onExit = ::finish)
            }
        }
    }
}
