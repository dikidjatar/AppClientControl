package com.xeg911.appclient.ui.webview

import androidx.compose.runtime.Stable

@Stable
data class WebViewState(
    val title: String = "Web",
    val currentUrl: String? = null,
    val progress: Int = 0,
    val isLoading: Boolean = true,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val error: PageError? = null,
) {
    data class PageError(
        val code: Int,
        val description: String,
        val failingUrl: String?,
    )
}
