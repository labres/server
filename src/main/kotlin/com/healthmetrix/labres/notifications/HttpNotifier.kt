package com.healthmetrix.labres.notifications

import com.healthmetrix.labres.logger
import org.springframework.web.reactive.function.client.WebClient

class HttpNotifier(private val configHttp: HttpNotificationConfig) : Notifier<Notification.HttpNotification> {

    override fun send(notification: Notification.HttpNotification) = try {
            val response = WebClient.create(notification.url)
                    .post()
                    .headers { headers ->
                        if (configHttp.basicAuth.user.isNotBlank() && configHttp.basicAuth.pass.isNotBlank())
                            headers.setBasicAuth(configHttp.basicAuth.user, configHttp.basicAuth.pass)
                        else {
                            val userBlank = configHttp.basicAuth.user.isBlank()
                            val passBlank = configHttp.basicAuth.pass.isBlank()
                            logger.warn("Notification basic auth missing, user blank? $userBlank pass blank? $passBlank")
                        }
                    }.retrieve().toBodilessEntity().block()
            response?.statusCode?.is2xxSuccessful ?: false
        } catch (ex: Exception) {
            logger.warn("Failed to notify", ex)
            false
        }
}

data class HttpNotificationConfig(
    val basicAuth: BasicAuth
) {

    data class BasicAuth(
        val user: String,
        val pass: String
    )
}
