package com.healthmetrix.labres.persistence.migrations

import com.healthmetrix.labres.lab.PCR_LOINC
import com.healthmetrix.labres.lab.TestType
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.persistence.OrderInformationRepository
import com.healthmetrix.labres.persistence.RawOrderInformation
import io.mockk.mockk
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class TestTypeMigrationTest {

    private val repository: OrderInformationRepository = mockk()
    private val underTest = TestTypeMigration(repository)

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
        testSiteId = testSiteId,
        testType = testType.toString(),
        notificationUrl = notificationUrl,
        issuedAt = issuedAt,
        enteredLabAt = enteredLabAt,
        reportedAt = reportedAt,
        notifiedAt = notifiedAt
    )

    @ParameterizedTest
    @EnumSource(TestType::class)
    fun `migrateTestType should return null for correct testTypes`(testType: TestType) {
        val input = raw.copy(testType = testType.toString())

        val result = underTest.migrateTestType(input)

        assertThat(result).isNull()
    }

    @Test
    fun `migrateTestType should migrate from LOINC code to PCR`() {
        val input = raw.copy(testType = PCR_LOINC)

        val result = underTest.migrateTestType(input)

        assertThat(result).isEqualTo(input.copy(testType = TestType.PCR.toString()))
    }

    @Test
    fun `migrateTestType should migrate from null to PCR`() {
        val input = raw.copy(testType = null)

        val result = underTest.migrateTestType(input)

        assertThat(result).isEqualTo(input.copy(testType = TestType.PCR.toString()))
    }

    @Test
    fun `migrateTestType should return null for invalid test types`() {
        val input = raw.copy(testType = "invalid")

        val result = underTest.migrateTestType(input)

        assertThat(result).isNull()
    }
}
