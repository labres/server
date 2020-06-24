package com.healthmetrix.labres.order

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.springframework.stereotype.Component

@Component
class OrderMetrics(private val meterRegistry: MeterRegistry) {

    fun countRegisteredOrders(issuerId: String?): Unit = Counter
        .builder("issuers.${issuerId ?: EON_ISSUER_ID}.orders.registration.registered.count")
        .description("Increments the sum of registered preissued order for issuer $issuerId")
        .tags(
            listOf(
                Tag.of("api", "orders"),
                Tag.of("operation", "registerOrder"),
                Tag.of("metric", "count"),
                Tag.of("scope", "orders"),
                Tag.of("issuerId", issuerId ?: EON_ISSUER_ID)
            )
        )
        .register(meterRegistry) // idempotent
        .increment()

    fun countConflictOnRegisteringOrders(issuerId: String?): Unit = Counter
        .builder("issuers.${issuerId ?: EON_ISSUER_ID}.orders.registration.conflict.count")
        .description("Increments the sum of conflicts on registering preissued order for issuer $issuerId")
        .tags(
            listOf(
                Tag.of("api", "orders"),
                Tag.of("operation", "registerOrder"),
                Tag.of("metric", "count"),
                Tag.of("scope", "orders"),
                Tag.of("issuerId", issuerId ?: EON_ISSUER_ID)
            )
        )
        .register(meterRegistry) // idempotent
        .increment()

    fun countErrorOnParsingOrderNumbersOnGet(issuerId: String?): Unit = Counter
        .builder("issuers.${issuerId ?: EON_ISSUER_ID}.orders.get.orderNumberParseErrors.count")
        .description("Increments the sum of errors parsing an order number for issuer $issuerId when getting an order")
        .tags(
            listOf(
                Tag.of("api", "orders"),
                Tag.of("operation", "getOrder"),
                Tag.of("metric", "count"),
                Tag.of("scope", "orders"),
                Tag.of("issuerId", issuerId ?: EON_ISSUER_ID)
            )
        )
        .register(meterRegistry) // idempotent
        .increment()

    fun countOrderNotFoundOnGet(issuerId: String?): Unit = Counter
        .builder("issuers.${issuerId ?: EON_ISSUER_ID}.orders.get.orderNotFound.count")
        .description("Increments the sum of orderNotFound for issuer $issuerId when getting an order")
        .tags(
            listOf(
                Tag.of("api", "orders"),
                Tag.of("operation", "getOrder"),
                Tag.of("metric", "count"),
                Tag.of("scope", "orders"),
                Tag.of("issuerId", issuerId ?: EON_ISSUER_ID)
            )
        )
        .register(meterRegistry) // idempotent
        .increment()

    fun countErrorOnParsingOrderNumbersOnUpdate(issuerId: String?): Unit = Counter
        .builder("issuers.${issuerId ?: EON_ISSUER_ID}.orders.update.orderNumberParseErrors.count")
        .description("Increments the sum of errors parsing an order number for issuer $issuerId when updating an order")
        .tags(
            listOf(
                Tag.of("api", "orders"),
                Tag.of("operation", "updateOrder"),
                Tag.of("metric", "count"),
                Tag.of("scope", "orders"),
                Tag.of("issuerId", issuerId ?: EON_ISSUER_ID)
            )
        )
        .register(meterRegistry) // idempotent
        .increment()

    fun countOrderNotFoundOnUpdate(issuerId: String?): Unit = Counter
        .builder("issuers.${issuerId ?: EON_ISSUER_ID}.orders.update.orderNotFound.count")
        .description("Increments the sum of orderNotFound for issuer $issuerId when updating an order")
        .tags(
            listOf(
                Tag.of("api", "orders"),
                Tag.of("operation", "updateOrder"),
                Tag.of("metric", "count"),
                Tag.of("scope", "orders"),
                Tag.of("issuerId", issuerId ?: EON_ISSUER_ID)
            )
        )
        .register(meterRegistry) // idempotent
        .increment()
}
