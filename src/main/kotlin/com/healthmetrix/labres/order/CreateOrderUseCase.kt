package com.healthmetrix.labres.order

import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class CreateOrderUseCase(
    private val orderInformationRepository: OrderInformationRepository
) {
    operator fun invoke(): Pair<OrderInformation, OrderNumber.External> {
        var orderNumber = OrderNumber.External.random()
        while (orderInformationRepository.findByExternalOrderNumber(orderNumber.number) != null)
            orderNumber = OrderNumber.External.random()

        val orderInfo = OrderInformation(
            UUID.randomUUID(),
            orderNumber,
            Status.IN_PROGRESS,
            updatedAt = null
        )

        orderInformationRepository.save(orderInfo)

        return orderInfo to orderNumber
    }
}
