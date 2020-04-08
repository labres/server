package com.healthmetrix.labres.order

import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import org.springframework.stereotype.Component

@Component
class CreateOrderUseCase(
    private val orderInformationRepository: OrderInformationRepository
) {
    operator fun invoke(): OrderInformation {
        var orderNumber = OrderNumber.random()
        while (orderInformationRepository.findById(orderNumber.externalOrderNumber) != null)
            orderNumber = OrderNumber.random()

        val orderInfo = OrderInformation(
            orderNumber,
            Status.IN_PROGRESS,
            updatedAt = null,
            hash = null
        )

        orderInformationRepository.save(orderInfo)

        return orderInfo
    }
}
