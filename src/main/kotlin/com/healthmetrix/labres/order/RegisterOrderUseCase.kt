package com.healthmetrix.labres.order

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.healthmetrix.labres.logger
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import net.logstash.logback.argument.StructuredArguments
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
    ): Result<OrderInformation, String> {
        val existing = repository.findByOrderNumberAndSample(orderNumber, sample)
            ?: return OrderInformation(
                id = idGenerator(),
                orderNumber = orderNumber,
                status = Status.IN_PROGRESS,
                notificationUrls = notificationUrl.asList(),
                issuedAt = Date.from(now),
                testSiteId = testSiteId,
                sample = sample
            ).let(repository::save).let(::Ok)

        if (!existing.notificationUrls.contains(notificationUrl)) {
            logger.warn(
                "[{}] Order already exists with a different notificationUrl",
                StructuredArguments.kv("method", "registerOrder"),
                StructuredArguments.kv("issuerId", orderNumber.issuerId),
                StructuredArguments.kv("orderNumber", orderNumber.number),
                StructuredArguments.kv("sample", sample)
            )
        }

        if (alreadyHasResult(existing)) {
            return Err("Order already has a result")
        }

        if (alreadyHasResult(existing) || willHaveMoreThanThreeNotificationsUrls(existing, notificationUrl)) {
            return Err("Order already has three notificationUrls")
        }

        return existing.copy(
            testSiteId = testSiteId,
            issuedAt = Date.from(now),
            notificationUrls = mergeNotificationUrls(existing.notificationUrls, notificationUrl)
        ).let(repository::save).let(::Ok)
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
