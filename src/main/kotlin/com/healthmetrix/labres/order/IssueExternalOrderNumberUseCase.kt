package com.healthmetrix.labres.order

import com.healthmetrix.labres.logger
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import org.springframework.stereotype.Service

@Service
class IssueExternalOrderNumberUseCase(
    private val repository: OrderInformationRepository,
    private val registerOrder: RegisterOrderUseCase
) {
    operator fun invoke(notificationUrl: String?): OrderInformation? {
        val eon = issueNewEon()
        return registerOrder(
            eon,
            testSiteId = null,
            notificationUrl = notificationUrl,
            sample = Sample.SALIVA
        )
    }

    private fun issueNewEon(): OrderNumber.External {
        var eon = OrderNumber.External.random()
        var failedCounter = 0
        while (repository.findByOrderNumber(eon) != null) {
            failedCounter++
            logger.warn("Tried issuing new orderNumber $failedCounter times")
            eon = OrderNumber.External.random()
        }
        return eon
    }
}
