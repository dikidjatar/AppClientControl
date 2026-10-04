package com.xeg911.shared.data.model

/**
 * A single contact entry from the device's address book.
 */
data class DeviceContact(
    val contactId: String = "",
    val displayName: String = "",
    val phoneNumbers: List<String> = emptyList(),
    /**
     * Lower cased [displayName]
     */
    val sortKey: String = ""
)
