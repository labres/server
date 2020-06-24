package com.healthmetrix.labres.lab

import com.healthmetrix.labres.order.EON_ISSUER_ID
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.springframework.stereotype.Component

@Component
class LabMetrics(
    private val meterRegistry: MeterRegistry
) {

    fun countPersistedTestResults(testResult: Result, labId: String, issuerId: String?): Unit = Counter
        .builder("issuers.${issuerId ?: EON_ISSUER_ID}.labs.$labId.testResults.persisted.$testResult.count")
        .description("Increments the sum of $testResult testResults for issuer $issuerId and $labId")
        .tags(
            listOf(
                Tag.of("api", "lab"),
                Tag.of("operation", "updateResult"),
                Tag.of("metric", "count"),
                Tag.of("scope", "testResult"),
                Tag.of("issuerId", issuerId ?: EON_ISSUER_ID),
                Tag.of("labId", labId)
            )
        )
        .register(meterRegistry) // idempotent
        .increment()

    fun countUpdateResults(updateResult: UpdateResult, labId: String, issuerId: String?): Unit = Counter
        .builder("issuers.${issuerId ?: EON_ISSUER_ID}.labs.$labId.updateResults.$updateResult.count")
        .description("Increments the sum of $updateResult updateResults for issuer $issuerId and $labId")
        .tags(
            listOf(
                Tag.of("api", "lab"),
                Tag.of("operation", "updateResult"),
                Tag.of("metric", "count"),
                Tag.of("scope", "updateResult"),
                Tag.of("issuerId", issuerId ?: EON_ISSUER_ID),
                Tag.of("labId", labId)
            )
        )
        .register(meterRegistry) // idempotent
        .increment()

    fun countUnauthorized() = Counter
        .builder("labApi.unauthorized.count")
        .description("Increments the sum of unauthorized requests on the lab facing API")
        .tags(
            listOf(
                Tag.of("api", "lab"),
                Tag.of("operation", "updateResult"),
                Tag.of("metric", "count"),
                Tag.of("scope", "unauthorized")
            )
        )
        .register(meterRegistry) // idempotent
        .increment()
}
