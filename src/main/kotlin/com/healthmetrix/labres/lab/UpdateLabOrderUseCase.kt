package com.healthmetrix.labres.lab

import com.healthmetrix.labres.OrderId
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class UpdateLabOrderUseCase(private val orderInformationRepository: OrderInformationRepository) {

    operator fun invoke(labId: String?, labOrderNumber: String?): Result {
        val internalOrderNumber = OrderNumber.Internal.from(labId, labOrderNumber) ?: return Result.InvalidOrderNumber
        val labOrder = orderInformationRepository.findByInternalOrderNumber(internalOrderNumber)

        if (labOrder != null) {
            return Result.Found(labOrder.id, labOrder.number.ion())
        }
        val created = orderInformationRepository.save(
            OrderInformation(
                UUID.randomUUID(),
                internalOrderNumber,
                Status.IN_PROGRESS,
                createdAt = Date.from(Instant.now()),
                updatedAt = null
            )
        )
        return Result.Created(created.id, created.number.ion())
    }

    sealed class Result {
        object InvalidOrderNumber : Result()
        data class Created(val id: OrderId, val labOrderNumber: String) : Result()
        data class Found(val id: OrderId, val labOrderNumber: String) : Result()
    }
}
