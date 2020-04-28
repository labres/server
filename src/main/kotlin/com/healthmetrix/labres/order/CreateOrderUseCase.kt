package com.healthmetrix.labres.order

import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class CreateOrderUseCase(
    private val orderInformationRepository: OrderInformationRepository
) {
    operator fun invoke(notificationUrl: String?): Pair<UUID, OrderNumber.External> {
        var orderNumber = OrderNumber.External.random()
        while (orderInformationRepository.findByOrderNumber(orderNumber) != null)
            orderNumber = OrderNumber.External.random()

        val orderInfo = OrderInformation(
            id = UUID.randomUUID(),
            number = orderNumber,
            status = Status.IN_PROGRESS,
            notificationUrl = notificationUrl,
            issuedAt = Date.from(Instant.now())
        )

        orderInformationRepository.save(orderInfo)

        return orderInfo.id to orderNumber
    }
}
