package com.xeg911.appclient.di

import com.xeg911.appclient.fcm.autoexec.AutoExecuteHandler
import com.xeg911.appclient.fcm.autoexec.handler.DownloadFileAutoExecutor
import com.xeg911.appclient.fcm.autoexec.handler.HideAppAutoExecutor
import com.xeg911.appclient.fcm.autoexec.handler.SetAppIconAutoExecutor
import com.xeg911.appclient.fcm.autoexec.handler.ShowAppAutoExecutor
import com.xeg911.appclient.fcm.autoexec.handler.StartMonitoringAutoExecutor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Registry of "maybe auto execute" handlers run on every incoming FCM message.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AutoExecuteModule {

    @Binds
    @IntoSet
    abstract fun bindStartMonitoring(h: StartMonitoringAutoExecutor): AutoExecuteHandler

    @Binds
    @IntoSet
    abstract fun bindDownloadFile(h: DownloadFileAutoExecutor): AutoExecuteHandler

    @Binds
    @IntoSet
    abstract fun bindSetAppIcon(h: SetAppIconAutoExecutor): AutoExecuteHandler

    @Binds
    @IntoSet
    abstract fun bindHideApp(h: HideAppAutoExecutor): AutoExecuteHandler

    @Binds
    @IntoSet
    abstract fun bindShowApp(h: ShowAppAutoExecutor): AutoExecuteHandler
}
