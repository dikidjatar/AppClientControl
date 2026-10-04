package com.xeg911.appclient.notification.icon

sealed interface NotificationIconSpec {

    /**
     * No icon was specified
     */
    data object Default : NotificationIconSpec

    /**
     * A drawable resource name
     */
    data class DrawableName(val name: String) : NotificationIconSpec

    /**
     * A publicly accessible image URL
     */
    data class RemoteUrl(val url: String) : NotificationIconSpec

    /**
     * An inline base64-encoded image in the form `data:<mimeType>;base64,<data>`
     */
    data class Base64Image(val raw: String) : NotificationIconSpec

    companion object {
        /**
         * Parses the raw server string into the appropriate [NotificationIconSpec].
         */
        fun parse(source: String?): NotificationIconSpec {
            if (source.isNullOrBlank()) return Default
            return when {
                source.startsWith("http://") || source.startsWith("https://") -> RemoteUrl(source)

                source.startsWith("data:image/") -> Base64Image(source)

                else -> DrawableName(source)
            }
        }
    }
}