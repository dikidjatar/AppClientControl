package com.xeg911.appclient.data.remote.channel.telegram.format

/**
 * HTML formatting helpers for Telegram Bot API (parse_mode = HTML).
 */
object TelegramHtml {

    fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    fun bold(text: String) = "<b>${escape(text)}</b>"
    fun italic(text: String) = "<i>${escape(text)}</i>"
    fun underline(text: String) = "<u>${escape(text)}</u>"
    fun strike(text: String) = "<s>${escape(text)}</s>"
    fun code(text: String) = "<code>${escape(text)}</code>"
    fun spoiler(text: String) = "<tg-spoiler>${escape(text)}</tg-spoiler>"
    fun link(text: String, url: String) =
        "<a href=\"$url\">${escape(text)}</a>"

    fun pre(text: String) = "<pre>${escape(text)}</pre>"
    fun preCode(text: String, language: String = "") =
        "<pre><code class=\"language-$language\">${escape(text)}</code></pre>"

    fun truncate(text: String, maxLength: Int, suffix: String = "…"): String {
        if (text.length <= maxLength) return text
        return text.take(maxLength - suffix.length).trimEnd() + suffix
    }
}