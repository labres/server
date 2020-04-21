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

        val saved = orderInformationRepository.save(
            orderInfo.copy(
                status = labResult.result.asStatus(),
                reportedAt = now,
                labId = labResult.labId
            )
        )

        if (saved.notificationId != null && saved.status != Status.IN_PROGRESS)
            notifier(saved.notificationId)
        else if (saved.notificationId == null)
            logger.warn("No notification id for ${saved.id}")

        return UpdateStatusResponse.Success
    }
}
