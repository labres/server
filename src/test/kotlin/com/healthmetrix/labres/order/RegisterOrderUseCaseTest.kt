package com.healthmetrix.labres.order

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.unwrap
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Date
import java.util.UUID

internal class RegisterOrderUseCaseTest {

    private val orderId = UUID.randomUUID()
    private val orderNumberString = "1234567890"
    private val issuerId = "issuerA"
    private val eon = OrderNumber.External.from(orderNumberString)
    private val preIssuedOrderNumber = OrderNumber.PreIssued(issuerId, orderNumberString)
    private val testSiteId = "testSiteA"
    private val notificationUrl = "http://callme.test"
    private val verificationSecret = UUID.randomUUID().toString()
    private val now = Instant.now()
    private val sampledAt = now.minusSeconds(3600).toEpochMilli()
    private val metadata: JsonNode = JsonNodeFactory.instance.objectNode().apply {
        put("hello", "world")
    }

    private val repository: OrderInformationRepository = mockk()
    private val metrics: OrderMetrics = mockk(relaxed = true)

    private val underTest = RegisterOrderUseCase(repository, metrics) { orderId }

    @BeforeEach
    internal fun setUp() {
        clearMocks(repository)
        every { repository.save(any()) } answers { arg(0) }
        every { repository.findByOrderNumberAndSample(any(), any()) } returns emptyList()
    }

    @Test
    fun `it should return a new order after saving`() {
        val result = underTest.invoke(
            orderNumber = eon,
            testSiteId = null,
            sample = Sample.SALIVA,
            notificationUrl = null,
            verificationSecret = null,
            sampledAt = null,
            metadata = null,
            now = now
        )

        assertThat(result.unwrap()).isEqualTo(
            OrderInformation(
                id = orderId,
                orderNumber = eon,
                status = Status.IN_PROGRESS,
                notificationUrls = emptyList(),
                testSiteId = null,
                issuedAt = Date.from(now),
                sample = Sample.SALIVA,
                sampledAt = null,
                metadata = null,
                verificationSecret = null
            )
        )
    }

    @Test
    fun `it should return a new order with all optional parameters after saving`() {
        val result = underTest.invoke(
            orderNumber = preIssuedOrderNumber,
            testSiteId = testSiteId,
            sample = Sample.SALIVA,
            notificationUrl = notificationUrl,
            verificationSecret = verificationSecret,
            sampledAt = sampledAt,
            metadata = metadata,
            now = now
        )

        assertThat(result.unwrap()).isEqualTo(
            OrderInformation(
                id = orderId,
                orderNumber = preIssuedOrderNumber,
                status = Status.IN_PROGRESS,
                notificationUrls = listOf(notificationUrl),
                testSiteId = testSiteId,
                issuedAt = Date.from(now),
                sample = Sample.SALIVA,
                sampledAt = sampledAt,
                metadata = metadata,
                verificationSecret = verificationSecret
            )
        )
    }

    @Test
    fun `it should save a new order`() {
        underTest.invoke(
            orderNumber = preIssuedOrderNumber,
            testSiteId = null,
            sample = Sample.SALIVA,
            notificationUrl = null,
            verificationSecret = null,
            sampledAt = null,
            metadata = null,
            now = now
        )

        verify(exactly = 1) {
            repository.save(
                OrderInformation(
                    id = orderId,
                    orderNumber = preIssuedOrderNumber,
                    status = Status.IN_PROGRESS,
                    notificationUrls = emptyList(),
                    testSiteId = null,
                    issuedAt = Date.from(now),
                    sample = Sample.SALIVA,
                    sampledAt = null,
                    metadata = null,
                    verificationSecret = null
                )
            )
        }
    }

    @Test
    fun `it should save a new order with all optional parameters`() {
        underTest.invoke(
            orderNumber = preIssuedOrderNumber,
            testSiteId = testSiteId,
            sample = Sample.SALIVA,
            notificationUrl = notificationUrl,
            verificationSecret = verificationSecret,
            sampledAt = sampledAt,
            metadata = metadata,
            now = now
        )

        verify(exactly = 1) {
            repository.save(
                OrderInformation(
                    id = orderId,
                    orderNumber = preIssuedOrderNumber,
                    status = Status.IN_PROGRESS,
                    notificationUrls = listOf(notificationUrl),
                    testSiteId = testSiteId,
                    issuedAt = Date.from(now),
                    sample = Sample.SALIVA,
                    sampledAt = sampledAt,
                    metadata = metadata,
                    verificationSecret = verificationSecret
                )
            )
        }
    }

    @Test
    fun `it should update the order when there is an existing order with less than 3 notification urls`() {
        val existingNotificationUrls = listOf("a", "b")
        val initialId = UUID.randomUUID()
        val initialIssuedAt = now.minusSeconds(36000)
        val initialSampledAt = now.minusSeconds(36005).toEpochMilli()
        val initialVerificationSecret = "initial"
        val initialTestSiteId = "initialTestSite"
        val initialMetadata = JsonNodeFactory.instance.objectNode().apply {
            put("hallo", "welt")
        }

        every { repository.findByOrderNumberAndSample(any(), any()) } returns listOf(
            OrderInformation(
                id = initialId,
                orderNumber = preIssuedOrderNumber,
                status = Status.IN_PROGRESS,
                issuedAt = Date.from(initialIssuedAt),
                sample = Sample.SALIVA,
                notificationUrls = existingNotificationUrls,
                sampledAt = initialSampledAt,
                verificationSecret = initialVerificationSecret,
                testSiteId = initialTestSiteId,
                metadata = initialMetadata
            )
        )

        underTest.invoke(
            orderNumber = preIssuedOrderNumber,
            testSiteId = null,
            sample = Sample.SALIVA,
            notificationUrl = null,
            verificationSecret = null,
            sampledAt = null,
            metadata = null,
            now = now
        )

        verify(exactly = 1) {
            repository.save(
                OrderInformation(
                    id = initialId,
                    orderNumber = preIssuedOrderNumber,
                    status = Status.IN_PROGRESS,
                    notificationUrls = existingNotificationUrls,
                    testSiteId = initialTestSiteId,
                    issuedAt = Date.from(now),
                    sample = Sample.SALIVA,
                    sampledAt = initialSampledAt,
                    metadata = initialMetadata,
                    verificationSecret = initialVerificationSecret
                )
            )
        }
    }

    @Test
    fun `it should update the order with optional parameters except verificationSecret when there is an existing order with less than 3 notification urls`() {
        val existingNotificationUrls = listOf("a", "b")
        val initialId = UUID.randomUUID()
        val initialIssuedAt = now.minusSeconds(36000)
        val initialSampledAt = now.minusSeconds(36005).toEpochMilli()
        val initialVerificationSecret = "initial"
        val initialTestSiteId = "initialTestSite"
        val initialMetadata = JsonNodeFactory.instance.objectNode().apply {
            put("hallo", "welt")
        }

        every { repository.findByOrderNumberAndSample(any(), any()) } returns listOf(
            OrderInformation(
                id = initialId,
                orderNumber = preIssuedOrderNumber,
                status = Status.IN_PROGRESS,
                issuedAt = Date.from(initialIssuedAt),
                sample = Sample.SALIVA,
                notificationUrls = existingNotificationUrls,
                sampledAt = initialSampledAt,
                verificationSecret = initialVerificationSecret,
                testSiteId = initialTestSiteId,
                metadata = initialMetadata
            )
        )

        underTest.invoke(
            orderNumber = preIssuedOrderNumber,
            testSiteId = testSiteId,
            sample = Sample.SALIVA,
            notificationUrl = notificationUrl,
            verificationSecret = verificationSecret,
            sampledAt = sampledAt,
            metadata = metadata,
            now = now
        )

        verify(exactly = 1) {
            repository.save(
                OrderInformation(
                    id = initialId,
                    orderNumber = preIssuedOrderNumber,
                    status = Status.IN_PROGRESS,
                    notificationUrls = existingNotificationUrls.plus(notificationUrl),
                    testSiteId = testSiteId,
                    issuedAt = Date.from(now),
                    sample = Sample.SALIVA,
                    sampledAt = sampledAt,
                    metadata = metadata,
                    verificationSecret = initialVerificationSecret
                )
            )
        }
    }

    @Test
    fun `it should save orderInformation when there is an existing order with 3 notification urls including the new notificationUrl`() {
        val existingNotificationUrls = listOf("a", "b", notificationUrl)

        every { repository.findByOrderNumberAndSample(any(), any()) } returns listOf(
            OrderInformation(
                id = orderId,
                orderNumber = preIssuedOrderNumber,
                status = Status.IN_PROGRESS,
                issuedAt = Date.from(now),
                sample = Sample.SALIVA,
                notificationUrls = existingNotificationUrls
            )
        )

        underTest.invoke(
            orderNumber = preIssuedOrderNumber,
            testSiteId = testSiteId,
            sample = Sample.SALIVA,
            notificationUrl = notificationUrl,
            verificationSecret = null,
            sampledAt = null,
            metadata = null,
            now = now
        )

        verify(exactly = 1) {
            repository.save(
                OrderInformation(
                    id = orderId,
                    orderNumber = preIssuedOrderNumber,
                    status = Status.IN_PROGRESS,
                    notificationUrls = existingNotificationUrls,
                    testSiteId = testSiteId,
                    issuedAt = Date.from(now),
                    sample = Sample.SALIVA
                )
            )
        }
    }

    @Test
    fun `it should return an error if order has already been registered with more than 3 different notificationUrls`() {
        every { repository.findByOrderNumberAndSample(any(), any()) } returns listOf(
            OrderInformation(
                id = orderId,
                orderNumber = eon,
                status = Status.IN_PROGRESS,
                issuedAt = Date.from(now),
                sample = Sample.SALIVA,
                notificationUrls = listOf("a", "b", "c")
            )
        )

        val res = underTest.invoke(
            preIssuedOrderNumber,
            testSiteId,
            Sample.SALIVA,
            notificationUrl,
            null,
            null,
            null,
            now
        )

        assertThat(res).isInstanceOf(Err::class.java)
    }

    @Test
    fun `it should not save anything if the order has already been registered three times`() {
        every { repository.findByOrderNumberAndSample(any(), any()) } returns listOf(
            OrderInformation(
                id = orderId,
                orderNumber = eon,
                status = Status.IN_PROGRESS,
                issuedAt = Date.from(now),
                sample = Sample.SALIVA,
                notificationUrls = listOf("a", "b", "c")
            )
        )

        underTest.invoke(
            preIssuedOrderNumber,
            testSiteId,
            Sample.SALIVA,
            notificationUrl,
            null,
            null,
            null,
            now
        )

        verify(exactly = 0) {
            repository.save(any())
        }
    }

    @Test
    fun `it should return an error if the order already has a status that is not IN_PROGRESS`() {
        every { repository.findByOrderNumberAndSample(any(), any()) } returns listOf(
            OrderInformation(
                id = orderId,
                orderNumber = eon,
                status = Status.POSITIVE,
                issuedAt = Date.from(now),
                sample = Sample.SALIVA,
                notificationUrls = listOf("a")
            )
        )

        val res = underTest.invoke(
            preIssuedOrderNumber,
            testSiteId,
            Sample.SALIVA,
            notificationUrl,
            null,
            null,
            null,
            now
        )

        assertThat(res).isInstanceOf(Err::class.java)
    }

    @Test
    fun `it should not save anything when the order already has a status that is not IN_PROGRESS`() {
        every { repository.findByOrderNumberAndSample(any(), any()) } returns listOf(
            OrderInformation(
                id = orderId,
                orderNumber = eon,
                status = Status.POSITIVE,
                issuedAt = Date.from(now),
                sample = Sample.SALIVA,
                notificationUrls = listOf("a")
            )
        )

        underTest.invoke(
            preIssuedOrderNumber,
            testSiteId,
            Sample.SALIVA,
            notificationUrl,
            null,
            null,
            null,
            now
        )

        verify(exactly = 0) {
            repository.save(any())
        }
    }
}
