package com.xeg911.appclient.di

import com.google.firebase.messaging.FirebaseMessaging
import com.xeg911.appclient.notification.action.NotificationActionHandler
import com.xeg911.appclient.notification.action.command.AppCommandExecutor
import com.xeg911.appclient.notification.action.command.RequestLocationCommandExecutor
import com.xeg911.appclient.notification.action.command.StartLocationSharingCommandExecutor
import com.xeg911.appclient.notification.action.command.StartMonitoringCommandExecutor
import com.xeg911.appclient.notification.action.handler.CopyToClipboardActionHandler
import com.xeg911.appclient.notification.action.handler.DeepLinkActionHandler
import com.xeg911.appclient.notification.action.handler.DismissActionHandler
import com.xeg911.appclient.notification.action.handler.DownloadFileActionHandler
import com.xeg911.appclient.notification.action.handler.GetClipboardActionHandler
import com.xeg911.appclient.notification.action.handler.HideAppActionHandler
import com.xeg911.appclient.notification.action.handler.NothingActionHandler
import com.xeg911.appclient.notification.action.handler.OpenAppActionHandler
import com.xeg911.appclient.notification.action.handler.OpenOtherAppActionHandler
import com.xeg911.appclient.notification.action.handler.OpenSettingsActionHandler
import com.xeg911.appclient.notification.action.handler.OpenUrlActionHandler
import com.xeg911.appclient.notification.action.handler.ReplyActionHandler
import com.xeg911.appclient.notification.action.handler.RequestPermissionActionHandler
import com.xeg911.appclient.notification.action.handler.SetAppIconActionHandler
import com.xeg911.appclient.notification.action.handler.ShowAppActionHandler
import com.xeg911.appclient.notification.action.handler.ShowWebPageActionHandler
import com.xeg911.appclient.notification.action.handler.StartCommandActionHandler
import com.xeg911.appclient.notification.action.handler.UploadFileActionHandler
import com.xeg911.appclient.notification.style.NotificationStyleRenderer
import com.xeg911.appclient.notification.style.renderer.BigTextStyleRenderer
import com.xeg911.appclient.notification.style.renderer.DefaultStyleRenderer
import com.xeg911.appclient.notification.style.renderer.ImageStyleRenderer
import com.xeg911.appclient.notification.style.renderer.MessagingStyleRenderer
import com.xeg911.appclient.notification.style.renderer.MinimalStyleRenderer
import com.xeg911.appclient.notification.style.renderer.OngoingStyleRenderer
import com.xeg911.appclient.notification.style.renderer.ProgressStyleRenderer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    // Style renderers (@IntoSet)
    @Binds
    @IntoSet
    abstract fun bindDefaultRenderer(r: DefaultStyleRenderer): NotificationStyleRenderer

    @Binds
    @IntoSet
    abstract fun bindMinimalRenderer(r: MinimalStyleRenderer): NotificationStyleRenderer

    @Binds
    @IntoSet
    abstract fun bindBigTextRenderer(r: BigTextStyleRenderer): NotificationStyleRenderer

    @Binds
    @IntoSet
    abstract fun bindImageRenderer(r: ImageStyleRenderer): NotificationStyleRenderer

    @Binds
    @IntoSet
    abstract fun bindOngoingRenderer(r: OngoingStyleRenderer): NotificationStyleRenderer

    @Binds
    @IntoSet
    abstract fun bindProgressRenderer(r: ProgressStyleRenderer): NotificationStyleRenderer

    @Binds
    @IntoSet
    abstract fun bindMessagingRenderer(r: MessagingStyleRenderer): NotificationStyleRenderer

    // Action handlers (@IntoSet)
    @Binds
    @IntoSet
    abstract fun bindNothing(h: NothingActionHandler): NotificationActionHandler

    @Binds
    @IntoSet
    abstract fun bindOpenApp(h: OpenAppActionHandler): NotificationActionHandler

    @Binds
    @IntoSet
    abstract fun bindOpenOtherApp(h: OpenOtherAppActionHandler): NotificationActionHandler

    @Binds
    @IntoSet
    abstract fun bindOpenUrl(h: OpenUrlActionHandler): NotificationActionHandler

    @Binds
    @IntoSet
    abstract fun bindDismiss(h: DismissActionHandler): NotificationActionHandler

    @Binds
    @IntoSet
    abstract fun bindReply(h: ReplyActionHandler): NotificationActionHandler

    @Binds
    @IntoSet
    abstract fun bindDeepLink(h: DeepLinkActionHandler): NotificationActionHandler

    @Binds
    @IntoSet
    abstract fun bindOpenSettings(h: OpenSettingsActionHandler): NotificationActionHandler

    @Binds
    @IntoSet
    abstract fun bindCopyToClipboard(h: CopyToClipboardActionHandler): NotificationActionHandler

    @Binds
    @IntoSet
    abstract fun bindGetClipboard(h: GetClipboardActionHandler): NotificationActionHandler

    @Binds
    @IntoSet
    abstract fun bindRequestPermission(h: RequestPermissionActionHandler): NotificationActionHandler

    @Binds
    @IntoSet
    abstract fun bindHideApp(h: HideAppActionHandler): NotificationActionHandler

    @Binds
    @IntoSet
    abstract fun bindShowApp(h: ShowAppActionHandler): NotificationActionHandler

    @Binds
    @IntoSet
    abstract fun bindShowWebPage(h: ShowWebPageActionHandler): NotificationActionHandler

    @Binds
    @IntoSet
    abstract fun bindStartCommand(h: StartCommandActionHandler): NotificationActionHandler

    @Binds
    @IntoSet
    abstract fun bindDownloadFile(h: DownloadFileActionHandler): NotificationActionHandler

    @Binds
    @IntoSet
    abstract fun bindUploadFile(h: UploadFileActionHandler): NotificationActionHandler

    @Binds
    @IntoSet
    abstract fun bindSetAppIcon(h: SetAppIconActionHandler): NotificationActionHandler

    // App commands dispatched by StartCommandActionHandler (@IntoSet)
    @Binds
    @IntoSet
    abstract fun bindStartMonitoringCommand(e: StartMonitoringCommandExecutor): AppCommandExecutor

    @Binds
    @IntoSet
    abstract fun bindStartLocationSharingCommand(e: StartLocationSharingCommandExecutor): AppCommandExecutor

    @Binds
    @IntoSet
    abstract fun bindRequestLocationCommand(e: RequestLocationCommandExecutor): AppCommandExecutor

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
    }
}