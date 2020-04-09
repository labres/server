package com.healthmetrix.labres.lab

import com.healthmetrix.labres.logger
import com.healthmetrix.labres.order.OrderNumber
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

interface NotifyUseCase {
    operator fun invoke(orderNumber: OrderNumber)
}

@Component
@Profile("notify")
class NotifyD4LUseCase(
    @Value("\${notification-endpoint}")
    private val endpoint: String
) : NotifyUseCase {
    override fun invoke(orderNumber: OrderNumber) {
        try {
            WebClient.create().post().apply {
                uri(endpoint)
                bodyValue(orderNumber)
            }.retrieve().toBodilessEntity().block()
        } catch (ex: Exception) {
            logger.warn("Failed to notify", ex)
        }
    }
}

@Component
@Profile("!notify")
class NotifyLogsUseCase : NotifyUseCase {
    override fun invoke(orderNumber: OrderNumber) {
        logger.info("Order number updated: $orderNumber")
    }
}
