package com.healthmetrix.labres.lab

import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.persistence.OrderInformation
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
        orderNumber: String,
        labResult: LabResult,
        now: Date = Date.from(Instant.now())
    ): UpdateStatusResponse {
        val orderInfo = OrderNumber.from(orderNumber)?.let {
            orderInformationRepository.findById(orderNumber)
        } ?: return UpdateStatusResponse.OrderNotFound

        orderInformationRepository.save(
            OrderInformation(
                number = orderInfo.number,
                status = labResult.result.asStatus(),
                updatedAt = now
            )
        )

        notifier(orderInfo.number)

        return UpdateStatusResponse.Success
    }
}
