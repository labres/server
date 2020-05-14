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
        notificationUrl: String?,
        now: Instant = Instant.now()
    ): Pair<UUID, OrderNumber> {
        // TODO check for existing?

        val orderInfo = OrderInformation(
            id = idGenerator(),
            orderNumber = orderNumber,
            status = Status.IN_PROGRESS,
            notificationUrl = notificationUrl,
            issuedAt = Date.from(now),
            testSiteId = testSiteId
        )

        val saved = repository.save(orderInfo)

        return saved.id to saved.orderNumber
    }
}
