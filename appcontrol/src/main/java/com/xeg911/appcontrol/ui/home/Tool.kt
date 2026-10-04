package com.xeg911.appcontrol.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.xeg911.appcontrol.R

enum class Tool(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    COMPOSER(R.string.tool_composer, R.string.tool_composer_desc, R.drawable.notification_add_24px),
    FILTERS(
        R.string.tool_filters,
        R.string.tool_filters_desc,
        R.drawable.notification_settings_24px
    ),
    TEMPLATES(R.string.tool_templates, R.string.tool_templates_desc, R.drawable.bookmark_24px),
    RULES(R.string.tool_rules, R.string.tool_rules_desc, R.drawable.tune_24px),
    FILES(R.string.tool_files, R.string.tool_files_desc, R.drawable.swap_vert_24px),
    TRANSFER_HISTORY(R.string.tool_history, R.string.tool_history_desc, R.drawable.history_24px),
    DEFAULT_CONFIG(
        R.string.tool_default_config,
        R.string.tool_default_config_desc,
        R.drawable.tune_24px
    ),
    REQUIRED_PERMISSIONS(
        R.string.tool_required_permissions,
        R.string.tool_required_permissions_desc,
        R.drawable.verified_user_24px
    ),
    SETTINGS(R.string.tool_settings, R.string.tool_settings_desc, R.drawable.settings_24px);

    companion object {
        const val MAX_HOME_TILES = 6

        val homeTools: List<Tool>
            get() = if (hasMore) entries.take(MAX_HOME_TILES - 1) else entries.take(MAX_HOME_TILES)

        val hasMore: Boolean get() = entries.size > MAX_HOME_TILES
    }
}
