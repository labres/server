package com.healthmetrix.labres.lab

import com.healthmetrix.labres.notifications.NotifyOnStatusChangeUseCase
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import java.time.Instant
import java.util.Date
import org.springframework.stereotype.Component

@Component
class UpdateResultUseCase(
    private val repository: OrderInformationRepository,
    private val notifyOnStatusChange: NotifyOnStatusChangeUseCase
) {
    operator fun invoke(
        updateResultRequest: UpdateResultRequest,
        labId: String,
        issuerId: String?,
        now: Date = Date.from(Instant.now())
    ): UpdateResult {
        val orderNumber = try {
            OrderNumber.from(issuerId, updateResultRequest.orderNumber)
        } catch (ex: IllegalArgumentException) {
            return UpdateResult.INVALID_ORDER_NUMBER
        }

        val orderInfo = repository.findByOrderNumber(orderNumber)
            ?: return UpdateResult.ORDER_NOT_FOUND

        val update = orderInfo.copy(
            status = updateResultRequest.result.asStatus(),
            labId = labId,
            testType = updateResultRequest.type.toString() // TODO
        )

        updateTimestamp(update, updateResultRequest.result, now)
            .let(repository::save)
            .also { notifyOnStatusChange(it.id, it.notificationUrl) }

        return UpdateResult.SUCCESS
    }

    private fun updateTimestamp(orderInformation: OrderInformation, result: Result, now: Date) =
        if (result == Result.IN_PROGRESS) {
            orderInformation.copy(enteredLabAt = now)
        } else {
            orderInformation.copy(reportedAt = now)
        }
}

enum class UpdateResult {
    INVALID_ORDER_NUMBER,
    ORDER_NOT_FOUND,
    SUCCESS
}
