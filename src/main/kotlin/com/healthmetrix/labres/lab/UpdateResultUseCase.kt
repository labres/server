package com.healthmetrix.labres.lab

import com.healthmetrix.labres.logger
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.persistence.OrderInformationRepository
import java.time.Instant
import java.util.Date
import org.springframework.stereotype.Component

@Component
class UpdateResultUseCase(
    private val orderInformationRepository: OrderInformationRepository,
    private val notifier: NotifyUseCase
) {
    operator fun invoke(
        labResult: LabResult,
        now: Date = Date.from(Instant.now())
    ): UpdateStatusResponse {
        val orderInfo = orderInformationRepository.findByOrderNumber(labResult.orderNumber)
            ?: return UpdateStatusResponse.OrderNotFound

        val update = orderInfo.copy(status = labResult.result.asStatus(), labId = labResult.labId)
        if (update.status == Status.IN_PROGRESS) {
            orderInformationRepository.save(update.copy(enteredLabAt = now))
        } else {
            orderInformationRepository.save(update.copy(reportedAt = now))
            if (update.notificationId != null)
                notifier(update.notificationId)
            else
                logger.warn("No notification id for ${update.id}")
        }

        return UpdateStatusResponse.Success
    }
}
