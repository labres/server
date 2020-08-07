package com.healthmetrix.labres.persistence

import com.healthmetrix.labres.lab.TestType
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Sample
import com.healthmetrix.labres.order.Status
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Date
import java.util.UUID

internal class RawOrderInformationTest {

    private val id = UUID.randomUUID()
    private val issuerId = "leKevin"
    private val status = Status.POSITIVE
    private val issuedAt = Date.from(Instant.now().minusSeconds(180))
    private val enteredLabAt = Date.from(Instant.now().minusSeconds(120))
    private val reportedAt = Date.from(Instant.now().minusSeconds(60))
    private val notifiedAt = Date.from(Instant.now().minusSeconds(30))
    private val notificationUrl = "http://lebignotifier.test"
    private val testType = TestType.PCR
    private val labId = "test-lab"
    private val testSiteId = "test-test-site"
    private val orderNumberString = "1234567890"
    private val sampledAt = 1596186947L
    private val verificationSecret = UUID.randomUUID().toString()

    private val raw = RawOrderInformation(
        id = id,
        issuerId = issuerId,
        orderNumber = orderNumberString,
        status = status.toString(),
        sample = Sample.BLOOD.toString(),
        labId = labId,
        testSiteId = testSiteId,
        testType = testType.toString(),
        notificationUrl = notificationUrl,
        issuedAt = issuedAt,
        enteredLabAt = enteredLabAt,
        reportedAt = reportedAt,
        notifiedAt = notifiedAt,
        sampledAt = sampledAt,
        verificationSecret = verificationSecret
    )

    private val cooked = OrderInformation(
        id = id,
        orderNumber = OrderNumber.from(issuerId, orderNumberString),
        status = status,
        labId = labId,
        testSiteId = testSiteId,
        testType = testType,
        issuedAt = issuedAt,
        notificationUrls = listOf(notificationUrl),
        reportedAt = reportedAt,
        notifiedAt = notifiedAt,
        enteredLabAt = enteredLabAt,
        sample = Sample.BLOOD,
        sampledAt = sampledAt,
        verificationSecret = verificationSecret
    )

    @Nested
    inner class Cooking {
        @Test
        fun `cooking should taste delicious`() {
            val result = raw.cook()
            assertThat(result).isEqualTo(cooked)
        }

        @Test
        fun `cooking should taste delicious for minimal rawOrderInformation`() {
            val result = raw.copy(
                notificationUrl = null,
                notifiedAt = null,
                reportedAt = null,
                labId = null,
                testSiteId = null,
                testType = null,
                enteredLabAt = null,
                sampledAt = null,
                verificationSecret = null
            ).cook()

            val expected = cooked.copy(
                notificationUrls = emptyList(),
                notifiedAt = null,
                reportedAt = null,
                labId = null,
                testSiteId = null,
                testType = null,
                enteredLabAt = null,
                sampledAt = null,
                verificationSecret = null
            )

            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `cooking should return null if id is null`() {
            val result = raw.copy(
                id = null
            ).cook()

            assertThat(result).isNull()
        }

        @Test
        fun `cooking should return null if issuerId is null`() {
            val result = raw.copy(
                issuerId = null
            ).cook()

            assertThat(result).isNull()
        }

        @Test
        fun `cooking should return null if orderNumber is null`() {
            val result = raw.copy(
                orderNumber = null
            ).cook()

            assertThat(result).isNull()
        }

        @Test
        fun `cooking should return null if status is null`() {
            val result = raw.copy(
                status = null
            ).cook()

            assertThat(result).isNull()
        }

        @Test
        fun `cooking should return null if issuedAt is null`() {
            val result = raw.copy(
                issuedAt = null
            ).cook()

            assertThat(result).isNull()
        }

        @Test
        fun `cooking should return null if testType is null but enteredLabAt isn't`() {
            val result = raw.copy(
                testType = null,
                enteredLabAt = enteredLabAt
            ).cook()

            assertThat(result).isNull()
        }

        @Test
        fun `cooking should return null if testType is null but reportedAt isn't`() {
            val result = raw.copy(
                testType = null,
                reportedAt = reportedAt
            ).cook()

            assertThat(result).isNull()
        }

        @Test
        fun `cooking should return null if testType can not be parsed`() {
            val result = raw.copy(
                testType = "invalid"
            ).cook()

            assertThat(result).isNull()
        }

        @Test
        fun `cooking should return null if sample is null`() {
            val result = raw.copy(
                sample = null
            ).cook()

            assertThat(result).isNull()
        }

        @Test
        fun `cooking should return null if sample can not be parsed`() {
            val result = raw.copy(
                sample = "invalid"
            ).cook()

            assertThat(result).isNull()
        }
    }

    @Nested
    inner class MappingToRaw {
        @Test
        fun `mapping should work`() {
            assertThat(cooked.raw()).isEqualTo(raw)
        }

        @Test
        fun `mapping should work for minimal example`() {
            val result = cooked.copy(
                labId = null,
                testSiteId = null,
                reportedAt = null,
                notifiedAt = null,
                notificationUrls = emptyList(),
                enteredLabAt = null,
                testType = null,
                sampledAt = null,
                verificationSecret = null
            ).raw()

            val expected = raw.copy(
                notificationUrl = null,
                notifiedAt = null,
                reportedAt = null,
                labId = null,
                testSiteId = null,
                testType = null,
                enteredLabAt = null,
                sampledAt = null,
                verificationSecret = null
            )

            assertThat(result).isEqualTo(expected)
        }
    }
}
