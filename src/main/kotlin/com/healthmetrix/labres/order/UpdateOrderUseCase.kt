package com.healthmetrix.labres.order

import com.healthmetrix.labres.persistence.OrderInformationRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UpdateOrderUseCase(
    private val orderInformationRepository: OrderInformationRepository
) {
    operator fun invoke(orderId: UUID, issuerId: String? = null, notificationId: String): Result {
        val order = orderInformationRepository.findById(orderId) ?: return Result.NOT_FOUND

        if (order.orderNumber.issuerId != (issuerId ?: EON_ISSUER_ID))
            return Result.NOT_FOUND

        orderInformationRepository.save(
            order.copy(notificationUrls = order.notificationUrls.plus(notificationId).distinct())
        )

        return Result.SUCCESS
    }

    enum class Result {
        NOT_FOUND,
        SUCCESS
    }
}
