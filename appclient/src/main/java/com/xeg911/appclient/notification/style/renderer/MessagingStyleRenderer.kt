package com.xeg911.appclient.notification.style.renderer

import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.xeg911.appclient.notification.icon.NotificationBadgeOverlay
import com.xeg911.appclient.notification.icon.NotificationIconResolver
import com.xeg911.appclient.notification.style.NotificationStyleRenderer
import com.xeg911.shared.data.model.notification.ChatMessage
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import com.xeg911.shared.data.model.notification.MessagingStylePayload
import com.xeg911.shared.data.model.notification.NotificationStyleDef
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingStyleRenderer @Inject constructor(
    private val iconResolver: NotificationIconResolver
) : NotificationStyleRenderer {

    override val styleId: String = NotificationStyleDef.MESSAGING.id

    override suspend fun render(
        context: Context,
        payload: FcmNotificationPayload,
        builder: NotificationCompat.Builder
    ) {
        val message = payload.messagingStyle ?: return
        val avatarBitmap: Bitmap? = message.senderAvatar?.let { iconResolver.resolveLargeIcon(it) }
        val badgeBitmap: Bitmap? = message.sourceBadge?.let { iconResolver.resolveLargeIcon(it) }
        val composedBitmap: Bitmap? = when {
            avatarBitmap != null && badgeBitmap != null ->
                NotificationBadgeOverlay.compose(avatarBitmap, badgeBitmap)

            else ->
                avatarBitmap
        }

        val senderPerson = buildPerson(message.senderName, composedBitmap)
        val me = Person.Builder().setName(ME_LABEL).build()
        val messagingStyle = NotificationCompat.MessagingStyle(me).also { style ->
            // Conversation title appears next to the sender name in the header.
            message.conversationTitle?.let { style.conversationTitle = it }
            style.isGroupConversation = message.isGroupConversation

            val dispatchTs = payload.timestamp ?: System.currentTimeMillis()

            if (message.messages.isNotEmpty()) {
                message.messages.forEach { msg ->
                    style.addMessage(msg.toStyleMessage(message, dispatchTs, senderPerson))
                }
            } else {
                style.addMessage(
                    NotificationCompat.MessagingStyle.Message(
                        /* text      = */ payload.body,
                        /* timestamp = */ dispatchTs,
                        /* person    = */ senderPerson
                    )
                )
            }
        }

        builder.setStyle(messagingStyle)
        composedBitmap?.let { builder.setLargeIcon(it) }
    }

    private fun buildPerson(name: String, avatar: Bitmap?): Person {
        val icon = avatar?.let { IconCompat.createWithBitmap(it) }
        return Person.Builder()
            .setName(name.ifBlank { "?" })
            .apply { icon?.let { setIcon(it) } }
            .build()
    }

    private fun ChatMessage.toStyleMessage(
        message: MessagingStylePayload,
        fallbackTimestamp: Long,
        defaultSender: Person
    ): NotificationCompat.MessagingStyle.Message {
        val ts = timestamp ?: fallbackTimestamp
        val person = if (senderName != null && senderName != message.senderName) {
            // Different sender in a group chat, build a lightweight Person without avatar.
            Person.Builder().setName(senderName).build()
        } else {
            defaultSender
        }
        return NotificationCompat.MessagingStyle.Message(text, ts, person)
    }

    private companion object {
        const val ME_LABEL = "Me"
    }
}