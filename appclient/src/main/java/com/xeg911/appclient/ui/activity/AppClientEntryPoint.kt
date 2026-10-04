package com.xeg911.appclient.ui.activity

import com.xeg911.appclient.event.DeviceEventReporter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppClientEntryPoint {
    fun eventReporter(): DeviceEventReporter
}
