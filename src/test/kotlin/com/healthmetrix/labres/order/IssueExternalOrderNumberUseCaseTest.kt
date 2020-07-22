package com.healthmetrix.labres.order

import com.github.michaelbull.result.Ok
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Date
import java.util.UUID

internal class IssueExternalOrderNumberUseCaseTest {

    private val registerOrder: RegisterOrderUseCase = mockk()
    private val repository: OrderInformationRepository = mockk()
    private val notificationUrl = "notificationUrl"
    private val orderInformation: OrderInformation = mockk()
    private val orderNumber = OrderNumber.External.from("1234567890")
    private val orderId = UUID.randomUUID()
    private val order = OrderInformation(
        id = orderId,
        orderNumber = orderNumber,
        status = Status.IN_PROGRESS,
        issuedAt = Date.from(Instant.now()),
        sample = Sample.SALIVA
    )

    private val underTest = IssueExternalOrderNumberUseCase(repository, registerOrder)

    @Test
    fun `it returns registered orderId and orderNumber`() {
        every { repository.findByOrderNumber(any()) } returns emptyList()
        every { registerOrder(any(), any(), any(), any(), any()) } returns Ok(order)

        assertThat(underTest(notificationUrl, Sample.SALIVA)).isEqualTo(order)
    }

    @Test
    fun `it eventually called registerOrder after retrying to issue a new random eon`() {
        every { repository.findByOrderNumber(any()) } returnsMany listOf(
            listOf(orderInformation),
            listOf(orderInformation, orderInformation),
            emptyList()
        )
        every { registerOrder(any(), any(), any(), any(), any()) } returns Ok(order)

        underTest(notificationUrl, Sample.SALIVA)

        verify { registerOrder(any(), null, any(), notificationUrl, any()) }
    }

    @Test
    fun `it calls registerOrder with default sample type SALIVA`() {
        every { repository.findByOrderNumber(any()) } returns emptyList()
        every { registerOrder(any(), any(), any(), any(), any()) } returns Ok(order)

        underTest(notificationUrl, Sample.SALIVA)

        verify {
            registerOrder(
                orderNumber = any(),
                testSiteId = any(),
                sample = Sample.SALIVA,
                notificationUrl = any(),
                now = any()
            )
        }
    }
}
