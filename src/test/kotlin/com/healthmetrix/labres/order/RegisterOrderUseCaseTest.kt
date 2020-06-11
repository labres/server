package com.healthmetrix.labres.order

import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    fun `it should return the registered order for an eon`() {
        val result = underTest.invoke(eon, null, Sample.SALIVA, null, now)

        assertThat(result).isEqualTo(OrderInformation(
            id = orderId,
            orderNumber = eon,
            status = Status.IN_PROGRESS,
            issuedAt = Date.from(now),
            sample = Sample.SALIVA
        ))
    }

    @Test
    fun `it should save orderInformation`() {
        underTest.invoke(preIssuedOrderNumber, testSiteId, Sample.SALIVA, notificationUrl, now)

        verify(exactly = 1) {
            repository.save(OrderInformation(
                id = orderId,
                orderNumber = preIssuedOrderNumber,
                status = Status.IN_PROGRESS,
                notificationUrl = notificationUrl,
                testSiteId = testSiteId,
                issuedAt = Date.from(now),
                sample = Sample.SALIVA
            ))
        }
    }

    @Test
    fun `it should return null if order has already been registered`() {
        every { repository.findByOrderNumberAndSample(any(), any()) } returns mockk()

        val res = underTest.invoke(preIssuedOrderNumber, testSiteId, Sample.SALIVA, notificationUrl, now)

        assertThat(res).isNull()
    }

    @Test
    fun `it should not save anything if order has already been registered`() {
        every { repository.findByOrderNumberAndSample(any(), any()) } returns mockk()

        underTest.invoke(preIssuedOrderNumber, testSiteId, Sample.SALIVA, notificationUrl, now)

        verify(exactly = 0) {
            repository.save(any())
        }
    }
}
