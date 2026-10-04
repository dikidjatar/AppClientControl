package com.xeg911.shared.data.model

data class ClipboardSnapshot(
    val items: List<String> = emptyList(),
    val label: String? = null,
    val mimeType: String = "text/plain",
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * True when the clipboard was empty or inaccessible.
     */
    val isEmpty: Boolean get() = items.isEmpty()
}
