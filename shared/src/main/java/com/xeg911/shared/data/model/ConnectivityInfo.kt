package com.xeg911.shared.data.model

data class ConnectivityInfo(
    val transportType: String = "NONE",   // WIFI | MOBILE | ETHERNET | VPN | NONE
    val ipAddress: String = "",
    val lastUpdated: Long = 0L,
    val pingRequest: Long = 0L,
    val pingResponse: Long = 0L,
    val pingLatencyMs: Long = 0L
)
