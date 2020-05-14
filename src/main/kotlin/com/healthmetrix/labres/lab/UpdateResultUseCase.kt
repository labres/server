package com.healthmetrix.labres.lab

import com.healthmetrix.labres.notifications.NotifyOnStatusChangeUseCase
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
        labResult: LabResult,
        now: Date = Date.from(Instant.now())
    ): OrderInformation? {
        val orderInfo = repository.findByOrderNumber(labResult.orderNumber)
            ?: return null

        val update = orderInfo.copy(
            status = labResult.result.asStatus(),
            labId = labResult.labId,
            testType = labResult.testType
        )

        return updateTimestamp(update, labResult, now)
            .let(repository::save)
            .also { notifyOnStatusChange(it.id, it.notificationUrl) }
    }

    private fun updateTimestamp(orderInformation: OrderInformation, labResult: LabResult, now: Date) =
        if (labResult.isEmptyLabResult) {
            orderInformation.copy(enteredLabAt = now)
        } else {
            orderInformation.copy(reportedAt = now)
        }
}
