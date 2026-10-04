package com.xeg911.appcontrol.di

import com.xeg911.appcontrol.data.remote.fcm.FcmPushSender
import com.xeg911.appcontrol.data.repository.AppSettingsRepositoryImpl
import com.xeg911.appcontrol.data.repository.AuthRepositoryImpl
import com.xeg911.appcontrol.data.repository.DeviceConfigRepositoryImpl
import com.xeg911.appcontrol.data.repository.DeviceRepositoryImpl
import com.xeg911.appcontrol.data.repository.FileStorageRepositoryImpl
import com.xeg911.appcontrol.data.repository.GeocodingRepositoryImpl
import com.xeg911.appcontrol.data.repository.LocationRepositoryImpl
import com.xeg911.appcontrol.data.repository.NotificationFilterRepositoryImpl
import com.xeg911.appcontrol.data.repository.NotificationRepositoryImpl
import com.xeg911.appcontrol.data.repository.RuleRepositoryImpl
import com.xeg911.appcontrol.data.repository.TemplateRepositoryImpl
import com.xeg911.appcontrol.data.repository.TransferHistoryRepositoryImpl
import com.xeg911.appcontrol.domain.repository.AppSettingsRepository
import com.xeg911.appcontrol.domain.repository.AuthRepository
import com.xeg911.appcontrol.domain.repository.DeviceConfigRepository
import com.xeg911.appcontrol.domain.repository.DeviceRepository
import com.xeg911.appcontrol.domain.repository.FileStorageRepository
import com.xeg911.appcontrol.domain.repository.GeocodingRepository
import com.xeg911.appcontrol.domain.repository.LocationRepository
import com.xeg911.appcontrol.domain.repository.NotificationFilterRepository
import com.xeg911.appcontrol.domain.repository.NotificationRepository
import com.xeg911.appcontrol.domain.repository.PushSender
import com.xeg911.appcontrol.domain.repository.RuleRepository
import com.xeg911.appcontrol.domain.repository.TemplateRepository
import com.xeg911.appcontrol.domain.repository.ThemeRepository
import com.xeg911.appcontrol.data.repository.ThemeRepositoryImpl
import com.xeg911.appcontrol.domain.repository.TransferHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindPushSender(impl: FcmPushSender): PushSender

    @Binds
    @Singleton
    abstract fun bindTemplateRepository(impl: TemplateRepositoryImpl): TemplateRepository

    @Binds
    @Singleton
    abstract fun bindDeviceConfigRepository(impl: DeviceConfigRepositoryImpl): DeviceConfigRepository

    @Binds
    @Singleton
    abstract fun bindNotificationFilterRepository(
        impl: NotificationFilterRepositoryImpl,
    ): NotificationFilterRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindFileStorageRepository(impl: FileStorageRepositoryImpl): FileStorageRepository

    @Binds
    @Singleton
    abstract fun bindTransferHistoryRepository(
        impl: TransferHistoryRepositoryImpl,
    ): TransferHistoryRepository

    @Binds
    @Singleton
    abstract fun bindAppSettingsRepository(impl: AppSettingsRepositoryImpl): AppSettingsRepository

    @Binds
    @Singleton
    abstract fun bindGeocodingRepository(impl: GeocodingRepositoryImpl): GeocodingRepository

    @Binds
    @Singleton
    abstract fun bindRuleRepository(impl: RuleRepositoryImpl): RuleRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository
}
