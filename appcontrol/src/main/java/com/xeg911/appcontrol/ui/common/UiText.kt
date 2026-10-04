package com.xeg911.appcontrol.ui.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed interface UiText {

    data class Dynamic(val value: String) : UiText

    /**
     * String resource with format args. Args may themselves be [UiText]
     * and are resolved recursively.
     */
    class Res(
        @param:StringRes val id: Int,
        vararg val args: Any,
    ) : UiText

    @Composable
    fun asString(): String = when (this) {
        is Dynamic -> value
        is Res -> stringResource(
            id,
            *args.map { if (it is UiText) it.asString() else it }.toTypedArray()
        )
    }

    fun asString(context: Context): String = when (this) {
        is Dynamic -> value
        is Res -> context.getString(
            id,
            *args.map { if (it is UiText) it.asString(context) else it }.toTypedArray()
        )
    }
}
