package com.xeg911.appcontrol.ui.feature.devicehub.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.domain.model.DeviceDetail
import com.xeg911.appcontrol.ui.components.EmptyState
import com.xeg911.appcontrol.ui.components.InfoRow
import com.xeg911.appcontrol.ui.components.InfoSection
import com.xeg911.appcontrol.ui.components.UsageBar
import com.xeg911.appcontrol.ui.util.formatBytes
import com.xeg911.shared.data.model.RamInfo
import com.xeg911.shared.data.model.StorageInfo

@Composable
fun HardwareTab(device: DeviceDetail) {
    val hardware = device.hardware

    if (hardware == null) {
        EmptyState(
            title = stringResource(R.string.hardware_empty_title),
            subtitle = stringResource(R.string.hardware_empty_subtitle),
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        MemorySection(ram = hardware.ram)
        StorageSection(
            internal = hardware.internalStorage,
            external = hardware.externalStorage,
        )
        ScreenSection(display = hardware.display)
        ProcessorSection(hardware = hardware)
        SystemSection(hardware = hardware)
    }
}

@Composable
private fun MemorySection(ram: RamInfo) {
    val usedBytes = ram.totalBytes - ram.availableBytes

    InfoSection(title = stringResource(R.string.memory_section_title)) {
        UsageBar(
            usedBytes = usedBytes,
            totalBytes = ram.totalBytes,
            freeBytes = ram.availableBytes,
        )

        Spacer(Modifier.height(12.dp))
        InfoRow(
            label = stringResource(R.string.memory_label_low_memory),
            value = if (ram.lowMemory) stringResource(R.string.memory_value_yes)
            else stringResource(R.string.memory_value_no)
        )
        if (ram.lowMemory && ram.lowMemoryThresholdBytes > 0) {
            InfoRow(
                label = stringResource(R.string.memory_label_low_memory_threshold),
                value = formatBytes(ram.lowMemoryThresholdBytes),
            )
        }
    }
}

@Composable
private fun StorageSection(internal: StorageInfo, external: StorageInfo?) {
    InfoSection(title = stringResource(id = R.string.storage_section_title)) {
        UsageBar(
            usedBytes = internal.totalBytes - internal.freeBytes,
            totalBytes = internal.totalBytes,
            freeBytes = internal.freeBytes,
            label = stringResource(R.string.storage_label_internal),
        )
        external?.let { ext ->
            Spacer(Modifier.height(16.dp))
            UsageBar(
                usedBytes = ext.totalBytes - ext.freeBytes,
                totalBytes = ext.totalBytes,
                freeBytes = ext.freeBytes,
                label = stringResource(R.string.storage_label_external),
            )
        }
    }
}

@Composable
private fun ScreenSection(display: com.xeg911.shared.data.model.DisplayInfo) {
    InfoSection(title = stringResource(R.string.screen_section_title)) {
        with(display) {
            InfoRow(
                label = stringResource(R.string.screen_label_resolution),
                value = stringResource(R.string.screen_value_resolution, widthPx, heightPx),
            )
            InfoRow(
                label = stringResource(R.string.screen_label_density),
                value = stringResource(R.string.screen_value_density, densityDpi),
            )
            InfoRow(
                label = stringResource(R.string.screen_label_refresh_rate),
                value = stringResource(R.string.screen_value_refresh_rate, refreshRate),
            )
            InfoRow(
                label = stringResource(R.string.screen_label_font_scale),
                value = stringResource(R.string.screen_value_font_scale, fontScaleFactor),
            )
        }
    }
}

@Composable
private fun ProcessorSection(hardware: com.xeg911.shared.data.model.HardwareInfo) {
    InfoSection(title = stringResource(R.string.processor_section_title)) {
        InfoRow(
            label = stringResource(R.string.processor_label_architecture),
            value = hardware.processorAbi,
        )
        InfoRow(
            label = stringResource(R.string.processor_label_core_count),
            value = stringResource(R.string.processor_value_core_count, hardware.cpuCoreCount),
        )
    }
}

@Composable
private fun SystemSection(hardware: com.xeg911.shared.data.model.HardwareInfo) {
    InfoSection(title = stringResource(R.string.system_section_title)) {
        InfoRow(label = stringResource(R.string.system_label_locale), value = hardware.locale)
        InfoRow(label = stringResource(R.string.system_label_timezone), value = hardware.timezone)
        InfoRow(
            label = stringResource(R.string.system_label_root_status),
            value = if (hardware.rooted) stringResource(R.string.system_value_rooted)
            else stringResource(R.string.system_value_not_rooted),
        )
    }
}
