package com.healthmetrix.labres.order

import com.healthmetrix.labres.persistence.OrderInformationRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UpdateOrderUseCase(
    private val orderInformationRepository: OrderInformationRepository
) {
    operator fun invoke(orderId: String, notificationId: String): Result {
        val id = try {
            UUID.fromString(orderId)
        } catch (ex: IllegalArgumentException) {
            return Result.InvalidOrderId
        }

        val order = orderInformationRepository.findById(id) ?: return Result.NotFound

        orderInformationRepository.save(
            order.copy(
                notificationId = notificationId
            )
        )

        return Result.Success
    }

    sealed class Result {
        object InvalidOrderId : Result()
        object NotFound : Result()
        object Success : Result()
    }
}