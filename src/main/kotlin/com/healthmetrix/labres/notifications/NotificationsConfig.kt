package com.healthmetrix.labres.notifications

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.healthmetrix.labres.logger
import com.healthmetrix.labres.secrets.Secrets
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.io.ByteArrayInputStream

@Configuration
class NotificationsConfig(
    @Value("\${labres.stage}")
    private val stage: String,
    private val secrets: Secrets,
    private val metrics: NotificationMetrics
) {

    @Bean
    @Profile("notify")
    fun httpNotifier(objectMapper: ObjectMapper): Notifier<Notification.HttpNotification> =
        secrets.get("lab-res/$stage/notification/basic-auth")
            ?.let { objectMapper.readValue(it, HttpNotificationConfig.BasicAuth::class.java) }
            ?.let(::HttpNotificationConfig)
            ?.let { HttpNotifier(it, metrics) }
            ?: throw InternalError("Could not retrieve basic auth credentials for HTTP notification")

    @Bean
    @Profile("notify")
    fun firebaseNotifier(): Notifier<Notification.FcmNotification> {
        val credentials = secrets.get("lab-res/$stage/fcm/credentials")
            ?.toByteArray(Charsets.UTF_8)
            ?.let(::ByteArrayInputStream)
            ?.let(GoogleCredentials::fromStream)
            ?: throw InternalError("Could not retrieve FCM credentials")

        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()

        val app = FirebaseApp.initializeApp(options)

        val messaging = FirebaseMessaging.getInstance(app)

        val dryRun = stage == "local"
        logger.info("Initializing google FCM with dryRun==$dryRun")

        return FirebaseNotifier(messaging, dryRun, metrics)
    }

    @Bean
    @Profile("!notify")
    fun loggingFcmNotifier() = LoggingNotifier<Notification.FcmNotification>()

    @Bean
    @Profile("!notify")
    fun loggingHttpNotifier() = LoggingNotifier<Notification.HttpNotification>()
}
