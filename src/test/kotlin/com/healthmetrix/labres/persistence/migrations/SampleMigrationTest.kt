package com.healthmetrix.labres.persistence.migrations

import com.healthmetrix.labres.lab.TestType
import com.healthmetrix.labres.order.Sample
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.persistence.OrderInformationRepository
import com.healthmetrix.labres.persistence.RawOrderInformation
import io.mockk.mockk
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class SampleMigrationTest {

    private val repository: OrderInformationRepository = mockk()
    private val underTest = SampleMigration(repository)

    private val id = UUID.randomUUID()
    private val issuerId = "issuer"
    private val status = Status.POSITIVE
    private val issuedAt = Date.from(Instant.now().minusSeconds(180))
    private val enteredLabAt = Date.from(Instant.now().minusSeconds(120))
    private val reportedAt = Date.from(Instant.now().minusSeconds(60))
    private val notifiedAt = Date.from(Instant.now().minusSeconds(30))
    private val notificationUrl = "http://notify.test"
    private val testType = TestType.PCR
    private val labId = "test-lab"
    private val testSiteId = "test-test-site"
    private val orderNumberString = "1234567890"

    private val raw = RawOrderInformation(
        id = id,
        issuerId = issuerId,
        orderNumber = orderNumberString,
        status = status.toString(),
        labId = labId,
        sample = Sample.SALIVA.toString(),
        testSiteId = testSiteId,
        testType = testType.toString(),
        notificationUrl = notificationUrl,
        issuedAt = issuedAt,
        enteredLabAt = enteredLabAt,
        reportedAt = reportedAt,
        notifiedAt = notifiedAt
    )

    @ParameterizedTest
    @EnumSource(Sample::class)
    fun `migrateSample should return null for correct testTypes`(testType: Sample) {
        val input = raw.copy(sample = testType.toString())

        val result = underTest.migrateSample(input)

        Assertions.assertThat(result).isNull()
    }

    @Test
    fun `migrateSample should migrate from null to SALIVA`() {
        val input = raw.copy(sample = null)

        val result = underTest.migrateSample(input)

        Assertions.assertThat(result).isEqualTo(input.copy(sample = Sample.SALIVA.toString()))
    }

    @Test
    fun `migrateSample should return null for invalid test types`() {
        val input = raw.copy(sample = "invalid")

        val result = underTest.migrateSample(input)

        Assertions.assertThat(result).isNull()
    }
}
