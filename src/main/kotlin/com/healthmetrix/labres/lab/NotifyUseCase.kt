package com.healthmetrix.labres.lab

import com.healthmetrix.labres.OrderId
import com.healthmetrix.labres.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

interface NotifyUseCase {
    operator fun invoke(orderId: OrderId)
}

@Component
@Profile("notify")
class NotifyD4LUseCase(
    @Value("\${notification-endpoint}")
    private val endpoint: String
) : NotifyUseCase {
    override fun invoke(orderId: OrderId) {
        try {
            WebClient.create().post().apply {
                uri(endpoint)
                bodyValue(orderId)
            }.retrieve().toBodilessEntity().block()
        } catch (ex: Exception) {
            logger.warn("Failed to notify", ex)
        }
    }
}

@Component
@Profile("!notify")
class NotifyLogsUseCase : NotifyUseCase {
    override fun invoke(orderId: OrderId) {
        logger.info("Order number updated: $orderId")
    }
}
