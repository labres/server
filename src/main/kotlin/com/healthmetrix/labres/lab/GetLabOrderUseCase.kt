package com.healthmetrix.labres.lab

import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class GetLabOrderUseCase(
    private val orderInformationRepository: OrderInformationRepository
) {
    operator fun invoke(orderId: String): Result? {
        val id = try {
            UUID.fromString(orderId)
        } catch (ex: IllegalArgumentException) {
            null
        }
        return when (val order = id?.let(orderInformationRepository::findById)) {
            is OrderInformation -> Result(order.status)
            else -> null
        }
    }

    data class Result(val status: Status)
}
