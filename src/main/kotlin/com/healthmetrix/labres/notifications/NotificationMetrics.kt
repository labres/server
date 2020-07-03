package com.healthmetrix.labres.notifications

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag

class NotificationMetrics(private val meterRegistry: MeterRegistry) {

    fun countTargetEmpty() = Counter.builder("notifications.target.empty")
        .tags(
            listOf(
                Tag.of("api", "lab"),
                Tag.of("operation", "updateResult"),
                Tag.of("metric", "count"),
                Tag.of("scope", "notifications")
            )
        )
        .register(meterRegistry)
        .increment()

    fun countTargetNotSupported() = Counter.builder("notifications.target.not_supported")
        .tags(
            listOf(
                Tag.of("api", "lab"),
                Tag.of("operation", "updateResult"),
                Tag.of("metric", "count"),
                Tag.of("scope", "notifications")
            )
        )
        .register(meterRegistry)
        .increment()

    fun countFcmNotificationFailed() = Counter.builder("notifications.target.fcm.failed")
        .tags(
            listOf(
                Tag.of("api", "lab"),
                Tag.of("operation", "updateResult"),
                Tag.of("metric", "count"),
                Tag.of("scope", "notifications")
            )
        )
        .register(meterRegistry)
        .increment()

    fun countHttpNotificationFailed() = Counter.builder("notifications.target.http.failed")
        .tags(
            listOf(
                Tag.of("api", "lab"),
                Tag.of("operation", "updateResult"),
                Tag.of("metric", "count"),
                Tag.of("scope", "notifications")
            )
        )
        .register(meterRegistry)
        .increment()
}
