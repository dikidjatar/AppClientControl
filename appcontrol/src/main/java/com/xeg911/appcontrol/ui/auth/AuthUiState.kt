package com.xeg911.appcontrol.ui.auth

import com.xeg911.appcontrol.ui.common.UiText

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: UiText? = null,
)

sealed interface AuthEvent {
    data object NavigateToDeviceList : AuthEvent
}
