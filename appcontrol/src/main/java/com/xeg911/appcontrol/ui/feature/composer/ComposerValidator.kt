package com.xeg911.appcontrol.ui.feature.composer

import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.common.UiText
import com.xeg911.shared.data.model.notification.NotificationStyleDef

private val COLOR_REGEX = Regex("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")

object ComposerValidator {

    fun validate(form: ComposerForm, hasTargets: Boolean): List<UiText> = buildList {
        if (!hasTargets) add(UiText.Res(R.string.composer_error_no_target))
        if (form.notificationId.isBlank()) add(UiText.Res(R.string.composer_error_id_required))
        // A cancel-only payload never renders: only the target and the id matter.
        if (form.cancelOnly) return@buildList

        val hasMessages = form.style == NotificationStyleDef.MESSAGING.id &&
                form.messaging.messages.any { it.text.isNotBlank() }
        if (form.title.isBlank() && form.body.isBlank() && !hasMessages) {
            add(UiText.Res(R.string.composer_error_content_required))
        }

        if (form.color.isNotBlank() && !COLOR_REGEX.matches(form.color.trim())) {
            add(UiText.Res(R.string.composer_error_color_format))
        }

        checkNumber(form.progress, R.string.composer_field_progress)
        checkNumber(form.progressMax, R.string.composer_field_progress_max)
        checkNumber(form.badge, R.string.composer_field_badge)
        checkNumber(form.timestamp, R.string.composer_field_timestamp)
        checkNumber(form.expiresAt, R.string.composer_field_expires_at)
        checkNumber(form.cancelAfterMs, R.string.composer_field_cancel_after)

        if (form.tapActionEnabled) {
            form.tapParams.missingRequired().forEach {
                add(UiText.Res(R.string.composer_error_tap_param_required, it))
            }
        }

        if (form.actions.size > ComposerOptions.MAX_ACTIONS) {
            add(UiText.Res(R.string.composer_error_too_many_actions, ComposerOptions.MAX_ACTIONS))
        }
        form.actions.forEachIndexed { index, action ->
            val position = index + 1
            if (action.action.isBlank()) add(
                UiText.Res(
                    R.string.composer_error_action_type,
                    position
                )
            )
            if (action.label.isBlank()) add(
                UiText.Res(
                    R.string.composer_error_action_label,
                    position
                )
            )
            action.params.missingRequired().forEach {
                add(UiText.Res(R.string.composer_error_action_param_required, position, it))
            }
            checkNumber(action.semantic, R.string.composer_field_semantic)
        }
    }

    private fun MutableList<UiText>.checkNumber(value: String, labelRes: Int) {
        if (value.isNotBlank() && value.trim().toLongOrNull() == null) {
            add(UiText.Res(R.string.composer_error_numeric, UiText.Res(labelRes)))
        }
    }

    private fun List<KeyValueEntry>.missingRequired(): List<String> =
        filter { it.required && it.value.isBlank() }.map { it.key }
}
