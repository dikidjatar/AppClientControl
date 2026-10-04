package com.xeg911.appcontrol.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.domain.model.AppFont
import com.xeg911.appcontrol.domain.model.AppTheme
import com.xeg911.appcontrol.domain.model.ThemeMode
import com.xeg911.appcontrol.ui.theme.ThemeDefinitions
import com.xeg911.appcontrol.ui.theme.ThemeViewModel
import com.xeg911.appcontrol.ui.theme.AppTheme as ThemeObj

@Composable
fun AppearanceSection(
    theme: AppTheme,
    viewModel: ThemeViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(ThemeObj.spacing.md)) {

        Text(
            text = "Theme Preset",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(ThemeObj.spacing.sm),
            contentPadding = PaddingValues(horizontal = ThemeObj.spacing.xs)
        ) {
            items(ThemeDefinitions.all) { definition ->
                val isSelected = theme.preset == definition.preset
                val previewColor =
                    definition.darkPalette?.primary ?: definition.lightPalette?.primary
                    ?: Color.Gray
                val previewBg =
                    definition.darkPalette?.background ?: definition.lightPalette?.background
                    ?: Color.DarkGray

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { viewModel.updatePreset(definition.preset) }
                        .padding(ThemeObj.spacing.xs)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(previewBg)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(previewColor)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = definition.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(ThemeObj.spacing.xs))

        Text(
            text = "Mode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        val currentDef = ThemeDefinitions.get(theme.preset)

        Row(horizontalArrangement = Arrangement.spacedBy(ThemeObj.spacing.md)) {
            if (currentDef.supportsLight && currentDef.supportsDark) {
                ModeChip(ThemeMode.SYSTEM, theme.mode, viewModel)
            }
            if (currentDef.supportsLight) {
                ModeChip(ThemeMode.LIGHT, theme.mode, viewModel)
            }
            if (currentDef.supportsDark) {
                ModeChip(ThemeMode.DARK, theme.mode, viewModel)
            }
        }

        Spacer(modifier = Modifier.height(ThemeObj.spacing.xs))

        Text(
            text = "Typography",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(ThemeObj.spacing.sm)
        ) {
            items(AppFont.entries) { font ->
                FilterChip(
                    selected = theme.font == font,
                    onClick = { viewModel.updateFont(font) },
                    label = { Text(font.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(ThemeObj.spacing.xs))

        Text(
            text = "Color Customization",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        ColorOverrideRow("Primary", theme.primaryColorOverride) { color ->
            viewModel.overridePrimaryColor(color)
        }
        ColorOverrideRow("Secondary", theme.secondaryColorOverride) { color ->
            viewModel.overrideSecondaryColor(color)
        }
        ColorOverrideRow("Accent (Tertiary)", theme.accentColorOverride) { color ->
            viewModel.overrideAccentColor(color)
        }
        ColorOverrideRow("Background", theme.backgroundColorOverride) { color ->
            viewModel.overrideBackgroundColor(color)
        }
        ColorOverrideRow("Surface", theme.surfaceColorOverride) { color ->
            viewModel.overrideSurfaceColor(color)
        }

    }
}

@Composable
private fun ColorOverrideRow(label: String, currentColor: Long?, onColorSelected: (Long?) -> Unit) {
    // For simplicity, we just provide a few preset colors to pick from, plus a clear button
    val overrideColors = listOf(
        0xFFE53935L, 0xFFD81B60L, 0xFF8E24AAL, 0xFF3949ABL, 0xFF1E88E5L,
        0xFF00ACC1L, 0xFF43A047L, 0xFFFDD835L, 0xFFFF9800L, 0xFF6D4C41L
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            if (currentColor != null) {
                Text(
                    text = "Clear",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clickable { onColorSelected(null) }
                        .padding(4.dp)
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(overrideColors) { colorValue ->
                val isSelected = currentColor == colorValue
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(colorValue))
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(colorValue) }
                )
            }
        }
    }
}

@Composable
private fun ModeChip(
    mode: ThemeMode,
    currentMode: ThemeMode,
    viewModel: ThemeViewModel
) {
    val label = when (mode) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }
    FilterChip(
        selected = currentMode == mode,
        onClick = { viewModel.updateMode(mode) },
        label = { Text(label) }
    )
}
