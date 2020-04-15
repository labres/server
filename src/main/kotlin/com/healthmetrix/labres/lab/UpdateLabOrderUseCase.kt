package com.healthmetrix.labres.lab

import com.healthmetrix.labres.OrderId
import org.springframework.stereotype.Component

@Component
class UpdateLabOrderUseCase {

    // TODO implement actual business logic when Data model exists
    operator fun invoke(labOrderNumber: String) = Result(
        OrderId.randomUUID(),
        labOrderNumber
    )

    data class Result(val id: OrderId, val labOrderNumber: String)
}