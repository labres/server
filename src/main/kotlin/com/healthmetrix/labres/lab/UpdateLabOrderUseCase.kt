package com.healthmetrix.labres.lab

import com.healthmetrix.labres.OrderId
import org.springframework.stereotype.Component

@Component
class UpdateLabOrderUseCase {

    // TODO implement actual business logic when Data model exists
    operator fun invoke(orderId: String) = Result(
        OrderId.randomUUID(),
        orderId
    )

    data class Result(val id: OrderId, val labOrderNumber: String)
}
