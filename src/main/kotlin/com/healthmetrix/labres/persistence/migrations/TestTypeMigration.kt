package com.healthmetrix.labres.persistence.migrations

import com.healthmetrix.labres.lab.TestType
import com.healthmetrix.labres.logger
import com.healthmetrix.labres.persistence.OrderInformationRepository
import com.healthmetrix.labres.persistence.RawOrderInformation
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
@Profile("migrate-test-types")
class TestTypeMigration(private val repository: OrderInformationRepository) {

    @EventListener
    fun onApplicationStarted(applicationStartedEvent: ApplicationStartedEvent) =
        repository.migrate(this::migrateTestType)

    fun migrateTestType(rawOrder: RawOrderInformation): RawOrderInformation? {
        val newTestType = when (TestType.from(rawOrder.testType)) {
            TestType.PCR -> TestType.PCR.toString()
            TestType.NGS -> TestType.NGS.toString()
            TestType.ANTIBODY -> TestType.ANTIBODY.toString()
            null -> null
        }

        if (newTestType == null) {
            logger.warn("Failed to migrate ${rawOrder.id}: TestType is ${rawOrder.testType}")
            return null
        }

        if (newTestType == rawOrder.testType) {
            return null
        }

        return rawOrder.apply {
            testType = newTestType
        }
    }
}
