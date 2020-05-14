package com.healthmetrix.labres.order

import com.healthmetrix.labres.persistence.OrderInformationRepository
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class UpdateOrderUseCase(
    private val orderInformationRepository: OrderInformationRepository
) {
    operator fun invoke(orderId: UUID, issuerId: String? = null, notificationId: String): Result {
        val order = orderInformationRepository.findById(orderId) ?: return Result.NOT_FOUND

        if (order.orderNumber.issuerId != (issuerId ?: EON_ISSUER_ID))
            return Result.NOT_FOUND

        orderInformationRepository.save(order.copy(notificationUrl = notificationId))

        return Result.SUCCESS
    }

    enum class Result {
        NOT_FOUND,
        SUCCESS
    }
}
