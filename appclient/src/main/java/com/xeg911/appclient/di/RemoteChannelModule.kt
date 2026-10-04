package com.xeg911.appclient.di

import com.xeg911.appclient.data.remote.channel.firebase.FirebaseRemoteChannel
import com.xeg911.appclient.data.remote.channel.telegram.TelegramRemoteChannel
import com.xeg911.shared.data.remote.channel.RemoteChannel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteChannelModule {

    @Binds
    @IntoSet
    abstract fun bindFirebaseRemoteChannel(channel: FirebaseRemoteChannel): RemoteChannel

    @Binds
    @IntoSet
    abstract fun bindTelegramRemoteChannel(channel: TelegramRemoteChannel): RemoteChannel
}