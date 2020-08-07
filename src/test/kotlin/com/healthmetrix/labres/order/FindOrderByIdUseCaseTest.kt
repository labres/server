package com.healthmetrix.labres.order

import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Date
import java.util.UUID

internal class FindOrderByIdUseCaseTest {

    private val repository: OrderInformationRepository = mockk()
    private val underTest = FindOrderByIdUseCase(repository)

    private val orderStatus = Status.IN_PROGRESS
    private val orderId = UUID.randomUUID()
    private val issuerId = "lekevin"
    private val eon = OrderNumber.External.from("1234567890")
    private val pon = OrderNumber.PreIssued(issuerId, "1234567890")
    private val orderInformation = OrderInformation(
        id = UUID.randomUUID(),
        orderNumber = eon,
        status = orderStatus,
        issuedAt = Date.from(Instant.now()),
        sample = Sample.SALIVA
    )

    @Test
    fun `it should return the order status for eon`() {
        every { repository.findById(any()) } returns orderInformation

        assertThat(underTest(orderId, null)).isEqualTo(orderInformation)
    }

    @Test
    fun `it should return the order status for pon`() {
        every { repository.findById(any()) } returns orderInformation.copy(orderNumber = pon)

        assertThat(underTest(orderId, issuerId)).isEqualTo(orderInformation.copy(orderNumber = pon))
    }

    @Test
    fun `it should return null if orderId and issuer do not match for eon`() {
        every { repository.findById(any()) } returns orderInformation.copy(orderNumber = pon)

        assertThat(underTest(orderId, null)).isNull()
    }

    @Test
    fun `it should return null if orderId and issuer do not match for pon`() {
        every { repository.findById(any()) } returns orderInformation

        assertThat(underTest(orderId, issuerId)).isNull()
    }

    @Test
    fun `it should return null if order could not be found for orderId`() {
        every { repository.findById(any()) } returns null

        assertThat(underTest(orderId, issuerId)).isNull()
    }
}
