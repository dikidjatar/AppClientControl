package com.xeg911.appcontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xeg911.appcontrol.core.network.NetworkMonitor
import com.xeg911.appcontrol.core.network.NetworkState
import com.xeg911.appcontrol.domain.model.AppSettings
import com.xeg911.appcontrol.domain.repository.AppSettingsRepository
import com.xeg911.appcontrol.domain.repository.AuthRepository
import com.xeg911.appcontrol.ui.components.ConnectivityBanner
import com.xeg911.appcontrol.ui.navigation.AppNavGraph
import com.xeg911.appcontrol.ui.navigation.Screen
import com.xeg911.appcontrol.ui.theme.AppControlTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var settingsRepository: AppSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startDestination = if (authRepository.isSignedIn()) Screen.HOME else Screen.AUTH

        setContent {
            val settings by settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            val networkState by networkMonitor.state
                .collectAsStateWithLifecycle(initialValue = NetworkState())

            val themeViewModel: com.xeg911.appcontrol.ui.theme.ThemeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val themeState by themeViewModel.themeState.collectAsStateWithLifecycle()

            AppControlTheme(theme = themeState) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Pinned above every screen so the offline state is always visible.
                        ConnectivityBanner(state = networkState)
                        AppNavGraph(
                            startDestination = startDestination,
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (networkState.isFullyConnected) Modifier
                                    else Modifier.consumeWindowInsets(WindowInsets.statusBars)
                                ),
                        )
                    }
                }
            }
        }
    }
}
