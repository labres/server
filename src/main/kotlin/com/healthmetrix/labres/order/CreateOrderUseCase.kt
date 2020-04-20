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
    operator fun invoke(): Pair<UUID, OrderNumber.External> {
        var orderNumber = OrderNumber.External.random()
        while (orderInformationRepository.findByOrderNumber(orderNumber) != null)
            orderNumber = OrderNumber.External.random()

        val orderInfo = OrderInformation(
            UUID.randomUUID(),
            orderNumber,
            Status.IN_PROGRESS,
            labId = null,
            issuedAt = Date.from(Instant.now()),
            reportedAt = null,
            notifiedAt = null,
            notificatonId = null
        )

        orderInformationRepository.save(orderInfo)

        return orderInfo.id to orderNumber
    }
}
