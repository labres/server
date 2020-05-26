package com.healthmetrix.labres.notifications

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.healthmetrix.labres.logger

class FirebaseNotifier(private val messaging: FirebaseMessaging, private val dryRun: Boolean = true) :
    Notifier<Notification.FcmNotification> {
    override fun send(notification: Notification.FcmNotification): Boolean {
        val message = Message.builder()
            .putData("type", "RESULT")
            .setToken(notification.token)
            .setNotification(
                com.google.firebase.messaging.Notification.builder()
                    .setTitle("Ihr Testergebnis ist da!")
                    .setBody("Sie können Ihr Testergebnis abrufen.")
                    .build()
            )
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .build()
            )
            .build()

        return try {
            val messageId = messaging.send(message, dryRun)
            logger.debug("Sent message with $messageId successfully")
            messageId != null
        } catch (ex: FirebaseMessagingException) {
            logger.warn("Failed sending message to FCM", ex)
            false
        }
    }
}
