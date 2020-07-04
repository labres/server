package com.healthmetrix.labres.order

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.springframework.stereotype.Component

@Component
class OrderMetrics(private val meterRegistry: MeterRegistry) {

    fun countRegisteredOrders(issuerId: String?, testSiteId: String?): Unit = Counter
        .builder("issuers.${issuerId ?: EON_ISSUER_ID}.orders.registration.registered")
        .description("Increments the sum of registered preissued order for issuer $issuerId")
        .tags(
            listOfNotNull(
                Tag.of("api", "orders"),
                Tag.of("operation", "registerOrder"),
                Tag.of("metric", "count"),
                Tag.of("scope", "orders"),
                Tag.of("issuerId", issuerId ?: EON_ISSUER_ID),
                testSiteId?.let { Tag.of("testSiteId", it) }
            )
        )
        .register(meterRegistry) // idempotent
        .increment()

    fun countRegisteringOrdersMultipleTimes(issuerId: String?, testSiteId: String?): Unit = Counter
        .builder("issuers.${issuerId ?: EON_ISSUER_ID}.orders.registration.multiple_times")
        .description("Increments the sum of conflicts on registering an order for issuer $issuerId")
        .tags(
            listOfNotNull(
                Tag.of("api", "orders"),
                Tag.of("operation", "registerOrder"),
                Tag.of("metric", "count"),
                Tag.of("scope", "orders"),
                Tag.of("issuerId", issuerId ?: EON_ISSUER_ID),
                testSiteId?.let { Tag.of("testSiteId", it) }
            )
        )
        .register(meterRegistry) // idempotent
        .increment()

    fun countConflictOnRegisteringOrders(issuerId: String?, testSiteId: String?): Unit = Counter
        .builder("issuers.${issuerId ?: EON_ISSUER_ID}.orders.registration.conflict")
        .description("Increments the sum of conflicts on registering an order for issuer $issuerId")
        .tags(
            listOfNotNull(
                Tag.of("api", "orders"),
                Tag.of("operation", "registerOrder"),
                Tag.of("metric", "count"),
                Tag.of("scope", "orders"),
                Tag.of("issuerId", issuerId ?: EON_ISSUER_ID),
                testSiteId?.let { Tag.of("testSiteId", it) }
            )
        )
        .register(meterRegistry) // idempotent
        .increment()

    fun countErrorOnParsingOrderNumbersOnGet(issuerId: String?): Unit = Counter
        .builder("issuers.${issuerId ?: EON_ISSUER_ID}.orders.get.orderNumberParseErrors")
        .description("Increments the sum of errors parsing an order number for issuer $issuerId when getting an order")
        .tags(
            listOfNotNull(
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
        .builder("issuers.${issuerId ?: EON_ISSUER_ID}.orders.get.orderNotFound")
        .description("Increments the sum of orderNotFound for issuer $issuerId when getting an order")
        .tags(
            listOfNotNull(
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
        .builder("issuers.${issuerId ?: EON_ISSUER_ID}.orders.update.orderNumberParseErrors")
        .description("Increments the sum of errors parsing an order number for issuer $issuerId when updating an order")
        .tags(
            listOfNotNull(
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
        .builder("issuers.${issuerId ?: EON_ISSUER_ID}.orders.update.orderNotFound")
        .description("Increments the sum of orderNotFound for issuer $issuerId when updating an order")
        .tags(
            listOfNotNull(
                Tag.of("api", "orders"),
                Tag.of("operation", "updateOrder"),
                Tag.of("metric", "count"),
                Tag.of("scope", "orders"),
                Tag.of("issuerId", issuerId ?: EON_ISSUER_ID)
            )
        )
        .register(meterRegistry) // idempotent
        .increment()

    fun countRewriteIssuerIdForIosBug(): Unit = Counter
        .builder("issuers.mvz.orders.iosbug.rewrite")
        .description("Counts how many times issuerId and testSiteId are being switched to tidy up the iOS bug")
        .tags(
            listOfNotNull(
                Tag.of("api", "orders"),
                Tag.of("metric", "count"),
                Tag.of("scope", "orders"),
                Tag.of("issuerId", "mvz")
            )
        )
        .register(meterRegistry) // idempotent
        .increment()

    fun countTruncateKevbSuffix(): Unit = Counter
        .builder("issuers.kevb.orders.truncate_suffix")
        .description("Counts how many times the analyt suffix is being truncated for kevb")
        .tags(
            listOfNotNull(
                Tag.of("api", "orders"),
                Tag.of("operation", "registerOrder"),
                Tag.of("metric", "count"),
                Tag.of("scope", "orders"),
                Tag.of("issuerId", "mvz")
            )
        )
        .register(meterRegistry) // idempotent
        .increment()
}
