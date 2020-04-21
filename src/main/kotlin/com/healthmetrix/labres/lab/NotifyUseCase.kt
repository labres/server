package com.healthmetrix.labres.lab

import com.healthmetrix.labres.logger
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

interface NotifyUseCase {
    operator fun invoke(notificationId: String)
}

@ConfigurationProperties(prefix = "notification")
@ConstructorBinding
data class NotificationConfig(
    val host: String,
    val path: String,
    val basicAuth: BasicAuth
) {

    data class BasicAuth(
        val user: String,
        val pass: String
    )
}

@Component
@Profile("notify")
class NotifyD4LUseCase(
    private val config: NotificationConfig
) : NotifyUseCase {
    override fun invoke(notificationId: String) {
        try {
            WebClient.create(config.host).post().uri { builder ->
                builder.path(config.path)
                builder.build(mapOf("notificationId" to notificationId))
            }.headers { headers ->
                if (config.basicAuth.user.isNotBlank() && config.basicAuth.pass.isNotBlank())
                    headers.setBasicAuth(config.basicAuth.user, config.basicAuth.pass)
                else {
                    val userBlank = config.basicAuth.user.isBlank()
                    val passBlank = config.basicAuth.pass.isBlank()
                    logger.warn("Notification basic auth missing, user blank? $userBlank pass blank? $passBlank")
                }
            }.retrieve().toBodilessEntity().block()
        } catch (ex: Exception) {
            logger.warn("Failed to notify", ex)
        }
    }
}

@Component
@Profile("!notify")
class NotifyLogsUseCase : NotifyUseCase {
    override fun invoke(notificationId: String) {
        logger.info("Notification sent with id $notificationId")
    }
}
