package com.xeg911.appclient.data.remote.channel.telegram.format

/**
 * Fluent builder for a Telegram `InlineKeyboardMarkup` JSON string.
 */
class TelegramKeyboard {

    private data class Button(
        val text: String,
        val url: String? = null,
        val callbackData: String? = null
    )

    private val rows = mutableListOf<List<Button>>()
    private var currentRow = mutableListOf<Button>()

    /**
     * Adds a button that opens [url] in the user's browser or app.
     */
    fun urlButton(text: String, url: String): TelegramKeyboard {
        currentRow.add(Button(text = text, url = url))
        return this
    }

    /**
     * Builds the `InlineKeyboardMarkup` JSON string.
     * Returns null when no buttons have been added.
     */
    fun build(): String? {
        commitCurrentRow()
        if (rows.isEmpty()) return null

        val rowsJson = rows.joinToString(",") { row ->
            "[" + row.joinToString(",") { btn ->
                buildString {
                    append("{\"text\":\"${btn.text.jsonEscape()}\"")
                    btn.url?.let { append(",\"url\":\"${it.jsonEscape()}\"") }
                    btn.callbackData?.let { append(",\"callback_data\":\"${it.jsonEscape()}\"") }
                    append("}")
                }
            } + "]"
        }

        return "{\"inline_keyboard\":[$rowsJson]}"
    }

    private fun commitCurrentRow() {
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow.toList())
            currentRow = mutableListOf()
        }
    }

    private fun String.jsonEscape(): String = this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}