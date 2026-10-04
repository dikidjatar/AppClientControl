package com.xeg911.appcontrol.ui.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.core.launcher.LauncherIconManager
import com.xeg911.appcontrol.domain.model.AppSettings
import com.xeg911.appcontrol.domain.model.ControlIconStyle
import com.xeg911.appcontrol.domain.model.ThemeMode
import com.xeg911.appcontrol.domain.repository.AppSettingsRepository
import com.xeg911.appcontrol.ui.common.UiText
import com.xeg911.appcontrol.ui.components.AppCard
import com.xeg911.appcontrol.ui.components.AppTopBar
import com.xeg911.appcontrol.ui.components.CardHeader
import com.xeg911.appcontrol.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AppSettingsRepository,
    private val launcherIconManager: LauncherIconManager,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _messages = Channel<UiText>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }

    fun setIconStyle(style: ControlIconStyle) = viewModelScope.launch {
        launcherIconManager.apply(style).fold(
            onSuccess = {
                repository.setIconStyle(style)
                _messages.send(UiText.Res(R.string.settings_icon_applied))
            },
            onFailure = { _messages.send(UiText.Dynamic(it.message ?: it.javaClass.simpleName)) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it.asString(context)) }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.tool_settings),
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(AppTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            item(key = "appearance") {
                AppCard(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                    CardHeader(
                        title = "Appearance",
                        subtitle = "Customize the app theme and colors",
                    )

                    val themeViewModel: com.xeg911.appcontrol.ui.theme.ThemeViewModel =
                        hiltViewModel()
                    val themeState by themeViewModel.themeState.collectAsStateWithLifecycle()

                    AppearanceSection(theme = themeState, viewModel = themeViewModel)
                }
            }
            item(key = "icon") {
                AppCard(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                    CardHeader(
                        title = stringResource(R.string.settings_icon_title),
                        subtitle = stringResource(R.string.settings_icon_subtitle),
                    )
                    ControlIconStyle.entries.forEach { style ->
                        IconStyleRow(
                            style = style,
                            selected = settings.iconStyle == style,
                            onSelect = { viewModel.setIconStyle(style) },
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings_icon_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}


@Composable
private fun IconStyleRow(style: ControlIconStyle, selected: Boolean, onSelect: () -> Unit) {
    val labelRes = when (style) {
        ControlIconStyle.DEFAULT -> R.string.settings_icon_default
        ControlIconStyle.TERMINAL -> R.string.settings_icon_terminal
        ControlIconStyle.RADAR -> R.string.settings_icon_radar
        ControlIconStyle.HEX -> R.string.settings_icon_hex
    }
    SelectableRow(selected = selected, onSelect = onSelect, label = stringResource(labelRes)) {
        IconPreview(style = style)
    }
}

@Composable
private fun IconPreview(style: ControlIconStyle) {
    val shape = RoundedCornerShape(10.dp)
    when (style) {
        ControlIconStyle.DEFAULT -> {
            val context = LocalContext.current
            // Adaptive-icon XML cannot be loaded by painterResource, rasterize the drawable.
            val bitmap = remember(context) {
                ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
                    ?.toBitmap(PREVIEW_PX, PREVIEW_PX)?.asImageBitmap()
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(PREVIEW_SIZE)
                        .clip(shape),
                )
            }
        }

        else -> {
            val (bgRes, fgRes) = when (style) {
                ControlIconStyle.TERMINAL -> R.color.ic_launcher_terminal_bg to R.drawable.ic_launcher_terminal_fg
                ControlIconStyle.RADAR -> R.color.ic_launcher_radar_bg to R.drawable.ic_launcher_radar_fg
                else -> R.color.ic_launcher_hex_bg to R.drawable.ic_launcher_hex_fg
            }
            Box(
                modifier = Modifier
                    .size(PREVIEW_SIZE)
                    .clip(shape)
                    .background(colorResource(bgRes)),
                contentAlignment = Alignment.Center,
            ) {
                // Adaptive foregrounds draw in the center 2/3 of the canvas, scale up to fill.
                Image(
                    painter = painterResource(fgRes),
                    contentDescription = null,
                    modifier = Modifier.size(PREVIEW_SIZE * 1.5f),
                )
            }
        }
    }
}

@Composable
private fun SelectableRow(
    selected: Boolean,
    onSelect: () -> Unit,
    label: String,
    leading: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = AppTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        RadioButton(selected = selected, onClick = null)
        leading()
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

private val PREVIEW_SIZE = 40.dp
private const val PREVIEW_PX = 160
