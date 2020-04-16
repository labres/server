package com.healthmetrix.labres.lab

import com.healthmetrix.labres.order.OrderNumber
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
        externalOrderNumber: String,
        labResult: LabResult,
        now: Date = Date.from(Instant.now())
    ): UpdateStatusResponse {
        val orderInfo = OrderNumber.External.from(externalOrderNumber)
            ?.let(orderInformationRepository::findByExternalOrderNumber)
            ?: return UpdateStatusResponse.OrderNotFound

        orderInformationRepository.save(orderInfo.copy(status = labResult.result.asStatus(), updatedAt = now))

        notifier(orderInfo.number)

        return UpdateStatusResponse.Success
    }
}
