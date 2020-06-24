package com.healthmetrix.labres

import io.micrometer.cloudwatch2.CloudWatchConfig
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Metrics
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import java.time.Duration

@Configuration
@Profile("cloudwatch")
class MicrometerCloudwatchConfig {

    @Bean
    fun provideConfig(
        @Value("\${labres.stage}")
        stage: String,
        @Value("\${metrics.cloudwatch.stepInSeconds}")
        stepInSeconds: Long
    ) = object : CloudWatchConfig {
        private val configuration = mapOf(
            "cloudwatch.namespace" to "lab-res/$stage",
            "cloudwatch.step" to Duration.ofSeconds(stepInSeconds).toString()
        )

        override fun get(key: String) = configuration[key]
    }

    @Bean
    fun provideRegistry(config: CloudWatchConfig) = CloudWatchMeterRegistry(
        config,
        Clock.SYSTEM,
        CloudWatchAsyncClient.create()
    ).also(Metrics::addRegistry)
}
