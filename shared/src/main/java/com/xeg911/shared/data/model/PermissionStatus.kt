package com.xeg911.shared.data.model

enum class PermissionType {
    RUNTIME,
    SPECIAL,
    INSTALL_TIME
}

data class PermissionStatus(
    val name: String = "",
    val simpleName: String = "",
    val type: PermissionType = PermissionType.INSTALL_TIME,
    val granted: Boolean = false,
    val updatedAt: Long = 0L
)

data class PermissionSnapshot(
    val items: Map<String, PermissionStatus> = emptyMap(),
    val grantedCount: Int = 0,
    val totalCount: Int = 0,
    val updatedAt: Long = 0L
)
