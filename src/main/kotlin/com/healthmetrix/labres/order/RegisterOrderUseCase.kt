package com.healthmetrix.labres.order

import com.fasterxml.jackson.databind.JsonNode
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
    private val metrics: OrderMetrics,
    private val idGenerator: () -> UUID = UUID::randomUUID
) {
    operator fun invoke(
        orderNumber: OrderNumber,
        testSiteId: String?,
        sample: Sample,
        notificationUrl: String?,
        verificationSecret: String?,
        sampledAt: Long?,
        metadata: JsonNode?,
        now: Instant = Instant.now()
    ): Result<OrderInformation, String> {
        val existingOrders = repository.findByOrderNumberAndSample(orderNumber, sample)

        val newestExisting = existingOrders.maxBy { it.issuedAt }
            ?: return OrderInformation(
                id = idGenerator(),
                orderNumber = orderNumber,
                status = Status.IN_PROGRESS,
                notificationUrls = listOfNotNull(notificationUrl),
                issuedAt = Date.from(now),
                testSiteId = testSiteId,
                sample = sample,
                verificationSecret = verificationSecret,
                sampledAt = sampledAt,
                metadata = metadata
            ).let(repository::save).let(::Ok)

        if (!newestExisting.notificationUrls.contains(notificationUrl)) {
            metrics.countRegisteringOrdersMultipleTimes(orderNumber.issuerId, testSiteId)
            logger.debug(
                "[{}] Order already exists with a different notificationUrl",
                StructuredArguments.kv("method", "registerOrder"),
                StructuredArguments.kv("issuerId", orderNumber.issuerId),
                StructuredArguments.kv("orderNumber", orderNumber.number),
                StructuredArguments.kv("sample", sample)
            )
        }

        if (newestExisting.alreadyHasResult()) {
            return Err("Order already has a result")
        }

        if (willHaveMoreThanThreeNotificationsUrls(newestExisting, notificationUrl)) {
            return Err("Order already has three notificationUrls")
        }

        return newestExisting.copy(
            testSiteId = testSiteId ?: newestExisting.testSiteId,
            issuedAt = Date.from(now),
            notificationUrls = mergeNotificationUrls(newestExisting.notificationUrls, notificationUrl),
            sampledAt = sampledAt ?: newestExisting.sampledAt,
            metadata = metadata ?: newestExisting.metadata
            // don't update verification secret
        ).let(repository::save).let(::Ok)
    }

    private fun OrderInformation.alreadyHasResult() = this.status != Status.IN_PROGRESS

    private fun willHaveMoreThanThreeNotificationsUrls(order: OrderInformation, notificationUrl: String?) =
        order.notificationUrls.plus(notificationUrl).distinct().size > 3

    private fun mergeNotificationUrls(existing: List<String>, new: String?) =
        existing.plus(listOfNotNull(new)).distinct()
}
