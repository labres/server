package com.healthmetrix.labres.lab

import com.healthmetrix.labres.order.Status
import org.springframework.stereotype.Component

@Component
class GetLabOrderUseCase {
    operator fun invoke(orderId: String): Result? {
        return Result(Status.POSITIVE)
    }

    data class Result(val status: Status)
}
