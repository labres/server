package com.healthmetrix.labres.order

import com.github.michaelbull.result.Result
import com.healthmetrix.labres.logger
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.stereotype.Service

@Service
class IssueExternalOrderNumberUseCase(
    private val repository: OrderInformationRepository,
    private val registerOrder: RegisterOrderUseCase
) {
    operator fun invoke(
        notificationUrl: String?,
        sample: Sample,
        verificationSecret: String?
    ): Result<OrderInformation, String> {
        val eon = issueNewEon()
        return registerOrder.invoke(
            eon,
            testSiteId = null,
            sample = sample,
            notificationUrl = notificationUrl,
            verificationSecret = verificationSecret,
            sampledAt = null,
            metadata = null
        )
    }

    private fun issueNewEon(): OrderNumber.External {
        var eon = OrderNumber.External.random()
        var failedCounter = 0
        while (repository.findByOrderNumber(eon).isNotEmpty()) {
            failedCounter++
            logger.warn("Tried issuing new EON {} $failedCounter times", kv("orderNumber", eon), kv("issuerId", EON_ISSUER_ID))
            eon = OrderNumber.External.random()
        }
        return eon
    }
}
