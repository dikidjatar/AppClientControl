package com.xeg911.appcontrol.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.components.AppTopBar
import com.xeg911.appcontrol.ui.components.ToolRow
import com.xeg911.appcontrol.ui.home.Tool
import com.xeg911.appcontrol.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllToolsScreen(
    onOpenTool: (Tool) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.home_all_tools),
                onNavigateBack = onNavigateBack
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(AppTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            items(Tool.entries, key = { it.name }) { tool ->
                ToolRow(
                    iconRes = tool.iconRes,
                    title = stringResource(tool.titleRes),
                    description = stringResource(tool.descriptionRes),
                    onClick = { onOpenTool(tool) },
                )
            }
        }
    }
}
