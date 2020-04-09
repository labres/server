package com.healthmetrix.labres.lab

import com.healthmetrix.labres.logger
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import java.time.Instant
import java.util.Date
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class UpdateResultUseCase(
    private val orderInformationRepository: OrderInformationRepository,
    @Value("\${notification-endpoint}")
    private val notificationEndpoint: String
) {
    operator fun invoke(
        orderNumber: String,
        labResult: LabResult,
        now: Date = Date.from(Instant.now())
    ): UpdateStatusResponse {
        val orderInfo = OrderNumber.from(orderNumber)?.let {
            orderInformationRepository.findById(orderNumber)
        } ?: return UpdateStatusResponse.OrderNotFound

        orderInformationRepository.save(
            OrderInformation(
                number = orderInfo.number,
                status = labResult.result.asStatus(),
                updatedAt = now
            )
        )

        try {
            WebClient.create().post().apply {
                uri(notificationEndpoint)
                bodyValue(orderInfo.number)
            }.retrieve().toBodilessEntity().block()
        } catch (ex: Exception) {
            logger.warn("Failed to notify", ex)
        }

        return UpdateStatusResponse.Success
    }
}
