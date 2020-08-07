package com.healthmetrix.labres.lab

import com.healthmetrix.labres.logger
import com.healthmetrix.labres.notifications.NotifyOnStatusChangeUseCase
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import net.logstash.logback.argument.StructuredArguments
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date

@Component
class UpdateResultUseCase(
    private val repository: OrderInformationRepository,
    private val notifyOnStatusChange: NotifyOnStatusChangeUseCase,
    private val metrics: LabMetrics
) {
    operator fun invoke(
        updateResultRequest: UpdateResultRequest,
        labId: String,
        issuerId: String?,
        now: Date = Date.from(Instant.now())
    ): UpdateResult {
        val orderNumber = try {
            OrderNumber.from(issuerId, updateResultRequest.orderNumber)
        } catch (ex: IllegalArgumentException) {
            return UpdateResult.INVALID_ORDER_NUMBER
        }

        val sample = updateResultRequest.type.sample
        val existingOrders = repository.findByOrderNumberAndSample(orderNumber, sample)

        if (existingOrders.isEmpty())
            return UpdateResult.ORDER_NOT_FOUND

        existingOrders
            .map { updateExistingOrder(it, updateResultRequest, labId) }
            .map { updateTimestamp(it, updateResultRequest.result, now) }
            .map(repository::save)
            .forEach { notifyOnStatusChange(it.id, it.notificationUrls) }

        return UpdateResult.SUCCESS
    }

    private fun updateExistingOrder(
        existing: OrderInformation,
        updateResultRequest: UpdateResultRequest,
        labId: String
    ) = existing.also {
        if (it.verificationSecret != updateResultRequest.verificationSecret)
            logger.warn(
                "[{}]: Overwriting verificationSecret",
                StructuredArguments.kv("method", "updateResult"),
                StructuredArguments.kv("issuerId", existing.orderNumber.issuerId),
                StructuredArguments.kv("orderNumber", existing.orderNumber.number),
                StructuredArguments.kv("testType", existing.testType)
            )
        metrics.countOverwritingVerificationSecret(labId, existing.orderNumber.issuerId)
    }.copy(
        status = updateResultRequest.result.asStatus(),
        labId = labId,
        testType = updateResultRequest.type,
        sampledAt = updateResultRequest.sampledAt,
        verificationSecret = updateResultRequest.verificationSecret
    )

    private fun updateTimestamp(orderInformation: OrderInformation, result: Result, now: Date) =
        if (result == Result.IN_PROGRESS) {
            orderInformation.copy(enteredLabAt = now)
        } else {
            orderInformation.copy(reportedAt = now)
        }
}

enum class UpdateResult {
    INVALID_ORDER_NUMBER,
    ORDER_NOT_FOUND,
    SUCCESS
}
