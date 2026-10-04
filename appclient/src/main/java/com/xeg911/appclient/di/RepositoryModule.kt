package com.xeg911.appclient.di

import com.xeg911.appclient.data.remote.config.DeviceConfigRepositoryImpl
import com.xeg911.appclient.data.repository.DeviceRepositoryImpl
import com.xeg911.appclient.data.repository.LocationRepositoryImpl
import com.xeg911.appclient.data.repository.NotificationRepositoryImpl
import com.xeg911.appclient.data.repository.RuleRepositoryImpl
import com.xeg911.appclient.domain.repository.DeviceConfigRepository
import com.xeg911.appclient.domain.repository.DeviceRepository
import com.xeg911.appclient.domain.repository.LocationRepository
import com.xeg911.appclient.domain.repository.NotificationRepository
import com.xeg911.appclient.domain.repository.RuleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    @Binds
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    abstract fun bindDeviceConfigRepository(impl: DeviceConfigRepositoryImpl): DeviceConfigRepository

    @Binds
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    abstract fun bindRuleRepository(impl: RuleRepositoryImpl): RuleRepository
}
