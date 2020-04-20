package com.healthmetrix.labres.order

import com.healthmetrix.labres.persistence.OrderInformationRepository
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class UpdateOrderUseCase(
    private val orderInformationRepository: OrderInformationRepository
) {
    operator fun invoke(orderId: String, notificationId: String): Result {
        val id = try {
            UUID.fromString(orderId)
        } catch (ex: IllegalArgumentException) {
            return Result.INVALID_ORDER_ID
        }

        val order = orderInformationRepository.findById(id) ?: return Result.NOT_FOUND

        orderInformationRepository.save(order.copy(notificationId = notificationId))

        return Result.SUCCESS
    }

    enum class Result {
        INVALID_ORDER_ID,
        NOT_FOUND,
        SUCCESS
    }
}
