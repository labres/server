package com.healthmetrix.labres.lab

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

        orderInformationRepository.save(orderInfo.copy(status = labResult.result.asStatus(), updatedAt = now))

        notifier(orderInfo.id)

        return UpdateStatusResponse.Success
    }
}
