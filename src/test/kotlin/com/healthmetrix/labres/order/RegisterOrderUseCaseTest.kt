package com.healthmetrix.labres.order

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
    private val now = Instant.now()

    private val repository: OrderInformationRepository = mockk()

    private val underTest = RegisterOrderUseCase(repository) { orderId }

    @BeforeEach
    internal fun setUp() {
        clearMocks(repository)
        every { repository.save(any()) } answers { arg(0) }
        every { repository.findByOrderNumberAndSample(any(), any()) } returns null
    }

    @Test
    fun `it should return the registered order when there is no existing order in the database`() {
        val result = underTest.invoke(eon, null, Sample.SALIVA, null, now)

        assertThat(result).isEqualTo(
            OrderInformation(
                id = orderId,
                orderNumber = eon,
                status = Status.IN_PROGRESS,
                issuedAt = Date.from(now),
                sample = Sample.SALIVA
            )
        )
    }

    @Test
    fun `it should save orderInformation when there is no existing order in the database`() {
        underTest.invoke(preIssuedOrderNumber, testSiteId, Sample.SALIVA, notificationUrl, now)

        verify(exactly = 1) {
            repository.save(
                OrderInformation(
                    id = orderId,
                    orderNumber = preIssuedOrderNumber,
                    status = Status.IN_PROGRESS,
                    notificationUrls = listOf(notificationUrl),
                    testSiteId = testSiteId,
                    issuedAt = Date.from(now),
                    sample = Sample.SALIVA
                )
            )
        }
    }

    @Test
    fun `it should save orderInformation when there is an existing order with less than 3 notification urls`() {
        val existingNotificationUrls = listOf("a", "b")

        every { repository.findByOrderNumberAndSample(any(), any()) } returns OrderInformation(
            id = orderId,
            orderNumber = preIssuedOrderNumber,
            status = Status.IN_PROGRESS,
            issuedAt = Date.from(now),
            sample = Sample.SALIVA,
            notificationUrls = existingNotificationUrls
        )

        underTest.invoke(preIssuedOrderNumber, testSiteId, Sample.SALIVA, notificationUrl, now)

        verify(exactly = 1) {
            repository.save(
                OrderInformation(
                    id = orderId,
                    orderNumber = preIssuedOrderNumber,
                    status = Status.IN_PROGRESS,
                    notificationUrls = existingNotificationUrls.plus(notificationUrl),
                    testSiteId = testSiteId,
                    issuedAt = Date.from(now),
                    sample = Sample.SALIVA
                )
            )
        }
    }

    @Test
    fun `it should save orderInformation when there is an existing order with 3 notification urls including the new notificationUrl`() {
        val existingNotificationUrls = listOf("a", "b", notificationUrl)

        every { repository.findByOrderNumberAndSample(any(), any()) } returns OrderInformation(
            id = orderId,
            orderNumber = preIssuedOrderNumber,
            status = Status.IN_PROGRESS,
            issuedAt = Date.from(now),
            sample = Sample.SALIVA,
            notificationUrls = existingNotificationUrls
        )

        underTest.invoke(preIssuedOrderNumber, testSiteId, Sample.SALIVA, notificationUrl, now)

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
    fun `it should return null if order has already been registered with more than 3 different notificationUrls`() {
        every { repository.findByOrderNumberAndSample(any(), any()) } returns OrderInformation(
            id = orderId,
            orderNumber = eon,
            status = Status.IN_PROGRESS,
            issuedAt = Date.from(now),
            sample = Sample.SALIVA,
            notificationUrls = listOf("a", "b", "c")
        )

        val res = underTest.invoke(preIssuedOrderNumber, testSiteId, Sample.SALIVA, notificationUrl, now)

        assertThat(res).isNull()
    }

    @Test
    fun `it should return null if the order already has a status that is not IN_PROGRESS`() {
        every { repository.findByOrderNumberAndSample(any(), any()) } returns OrderInformation(
            id = orderId,
            orderNumber = eon,
            status = Status.POSITIVE,
            issuedAt = Date.from(now),
            sample = Sample.SALIVA,
            notificationUrls = listOf("a")
        )

        val res = underTest.invoke(preIssuedOrderNumber, testSiteId, Sample.SALIVA, notificationUrl, now)

        assertThat(res).isNull()
    }

    @Test
    fun `it should not save anything if the order has already been registered three times`() {
        every { repository.findByOrderNumberAndSample(any(), any()) } returns OrderInformation(
            id = orderId,
            orderNumber = eon,
            status = Status.IN_PROGRESS,
            issuedAt = Date.from(now),
            sample = Sample.SALIVA,
            notificationUrls = listOf("a", "b", "c")
        )

        underTest.invoke(preIssuedOrderNumber, testSiteId, Sample.SALIVA, notificationUrl, now)

        verify(exactly = 0) {
            repository.save(any())
        }
    }
}
