package com.xeg911.appcontrol.ui.common

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>

    data class Content<T>(val data: T) : UiState<T>

    data class Error(val message: String) : UiState<Nothing>
}

fun <T> Result<T>.toUiState(): UiState<T> = fold(
    onSuccess = { UiState.Content(it) },
    onFailure = { UiState.Error(it.localizedMessage ?: it.message ?: "Unknown error") },
)

val <T> UiState<T>.hasContent: Boolean
    get() = this is UiState.Content