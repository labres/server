package com.healthmetrix.labres.order

import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class RegisterOrderUseCase(
    private val repository: OrderInformationRepository,
    private val idGenerator: () -> UUID = UUID::randomUUID
) {
    operator fun invoke(
        orderNumber: OrderNumber,
        testSiteId: String?,
        sample: Sample,
        notificationUrl: String?,
        now: Instant = Instant.now()
    ): OrderInformation? {
        val existing = repository.findByOrderNumber(orderNumber)

        if (existing != null)
            return null

        val orderInfo = OrderInformation(
            id = idGenerator(),
            orderNumber = orderNumber,
            status = Status.IN_PROGRESS,
            notificationUrl = notificationUrl,
            issuedAt = Date.from(now),
            testSiteId = testSiteId,
            sample = sample
        )

        return repository.save(orderInfo)
    }
}
