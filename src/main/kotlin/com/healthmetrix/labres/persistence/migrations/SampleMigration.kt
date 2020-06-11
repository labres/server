package com.healthmetrix.labres.persistence.migrations

import com.healthmetrix.labres.logger
import com.healthmetrix.labres.order.Sample
import com.healthmetrix.labres.persistence.OrderInformationRepository
import com.healthmetrix.labres.persistence.RawOrderInformation
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

// TODO delete when successfully migrated
@Service
@Profile("migrate-samples")
class SampleMigration(private val repository: OrderInformationRepository) {

    @EventListener
    fun onApplicationStarted(applicationStartedEvent: ApplicationStartedEvent) =
        repository.migrate(this::migrateSample)

    fun migrateSample(rawOrder: RawOrderInformation): RawOrderInformation? {
        // smart casts
        val rawSample = rawOrder.sample
            ?: return rawOrder.apply {
                sample = Sample.SALIVA.toString()
            }

        try {
            Sample.valueOf(rawSample)
        } catch (ex: IllegalArgumentException) {
            logger.warn("Failed to migrate ${rawOrder.id}: Sample is '$rawSample'")
        }

        return null
    }
}
