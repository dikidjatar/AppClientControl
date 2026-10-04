package com.xeg911.appcontrol.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.components.SectionHeader
import com.xeg911.appcontrol.ui.components.ToolTile
import com.xeg911.appcontrol.ui.home.Tool
import com.xeg911.appcontrol.ui.theme.AppTheme

private const val COLUMNS = 3

private data class ToolEntry(val tool: Tool?)

fun LazyListScope.toolsSection(onOpenTool: (Tool) -> Unit, onOpenAll: () -> Unit) {
    item(key = "tools_header") {
        SectionHeader(title = stringResource(R.string.home_section_tools))
    }
    item(key = "tools_grid") {
        val entries = Tool.homeTools.map { ToolEntry(it) } +
                listOfNotNull(if (Tool.hasMore) ToolEntry(null) else null)
        ToolGrid(entries = entries, onOpenTool = onOpenTool, onOpenAll = onOpenAll)
    }
}

@Composable
private fun ToolGrid(entries: List<ToolEntry>, onOpenTool: (Tool) -> Unit, onOpenAll: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        entries.chunked(COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                row.forEach { entry ->
                    val tool = entry.tool
                    if (tool != null) {
                        ToolTile(
                            iconRes = tool.iconRes,
                            label = stringResource(tool.titleRes),
                            onClick = { onOpenTool(tool) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        ToolTile(
                            iconRes = R.drawable.grid_view_24px,
                            label = stringResource(R.string.home_all_tools),
                            onClick = onOpenAll,
                            modifier = Modifier.weight(1f),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                repeat(COLUMNS - row.size) { Column(Modifier.weight(1f)) {} }
            }
        }
    }
}
