package com.healthmetrix.labres.order

import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date
import java.util.UUID

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
        val existing = repository.findByOrderNumberAndSample(orderNumber, sample)
            ?: return OrderInformation(
                id = idGenerator(),
                orderNumber = orderNumber,
                status = Status.IN_PROGRESS,
                notificationUrls = notificationUrl.asList(),
                issuedAt = Date.from(now),
                testSiteId = testSiteId,
                sample = sample
            ).let(repository::save)

        if (alreadyHasResult(existing) || willHaveMoreThanThreeNotificationsUrls(existing, notificationUrl)) {
            return null
        }

        return existing.copy(
            testSiteId = testSiteId,
            issuedAt = Date.from(now),
            notificationUrls = mergeNotificationUrls(existing.notificationUrls, notificationUrl)
        ).let(repository::save)
    }

    private fun alreadyHasResult(order: OrderInformation?) = order?.status != Status.IN_PROGRESS

    private fun willHaveMoreThanThreeNotificationsUrls(order: OrderInformation, notificationUrl: String?) =
        order.notificationUrls.plus(notificationUrl).distinct().size > 3

    private fun mergeNotificationUrls(existing: List<String>, new: String?) = existing.plus(new.asList()).distinct()

    private fun String?.asList() = if (this == null) {
        emptyList()
    } else {
        listOf(this)
    }
}
