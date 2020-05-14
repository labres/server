package com.healthmetrix.labres.lab

import com.healthmetrix.labres.logger
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
        labResult: LabResult,
        now: Date = Date.from(Instant.now())
    ): OrderInformation? {
        val orderInfo = orderInformationRepository.findByOrderNumber(labResult.orderNumber)
            ?: return null

        val update = orderInfo.copy(
            status = labResult.result.asStatus(),
            labId = labResult.labId,
            testType = labResult.testType
        )

        return if (labResult.isEmptyLabResult)
            orderInformationRepository.save(update.copy(enteredLabAt = now))
        else
            orderInformationRepository
                .save(update.copy(reportedAt = now))
                .also(this::notify)
    }

    private fun notify(orderInformation: OrderInformation): Unit = if (orderInformation.notificationUrl != null)
        notifier(orderInformation.notificationUrl)
    else
        logger.warn("No notification url for ${orderInformation.id}")
}
