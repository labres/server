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
import org.junit.jupiter.api.Test

class UpdateOrderUseCaseTest {
    private val orderInformationRepository: OrderInformationRepository = mockk()

    private val underTest = UpdateOrderUseCase(orderInformationRepository)

    val orderId = UUID.randomUUID()

    private val orderNumberString = "1234567891"
    private val issuerId = "leKevin"
    private val eon = OrderNumber.External.from(orderNumberString)
    private val preissuedOrderNumber = OrderNumber.PreIssued(issuerId, orderNumberString)

    private val notificationUrl = "http://textme69.test"

    private val orderInfo = OrderInformation(
        id = orderId,
        orderNumber = eon,
        status = Status.IN_PROGRESS,
        issuedAt = Date.from(Instant.now())
    )

    @Test
    fun `it returns Success with notification url`() {
        every { orderInformationRepository.findById(any()) } returns orderInfo
        every { orderInformationRepository.save(any()) } returns orderInfo

        val result = underTest(orderId, null, notificationUrl)
        assertThat(result).isEqualTo(UpdateOrderUseCase.Result.SUCCESS)
    }

    @Test
    fun `it returns Success with notification url for with issuerId`() {
        every { orderInformationRepository.findById(any()) } returns orderInfo.copy(orderNumber = preissuedOrderNumber)
        every { orderInformationRepository.save(any()) } returns orderInfo.copy(orderNumber = preissuedOrderNumber)

        val result = underTest(orderId, issuerId, notificationUrl)
        assertThat(result).isEqualTo(UpdateOrderUseCase.Result.SUCCESS)
    }

    @Test
    fun `it updates the order with the new notification url`() {
        clearMocks(orderInformationRepository)

        every { orderInformationRepository.findById(any()) } returns orderInfo
        every { orderInformationRepository.save(any()) } returns orderInfo

        underTest(orderId, null, notificationUrl)

        verify(exactly = 1) { orderInformationRepository.save(orderInfo.copy(notificationUrl = notificationUrl)) }
    }

    @Test
    fun `it returns NOT_FOUND if order id is not valid UUID for an EON`() {
        every { orderInformationRepository.findById(any()) } returns orderInfo.copy(orderNumber = preissuedOrderNumber)

        assertThat(
            underTest(
                orderId,
                null,
                "notification"
            )
        ).isEqualTo(UpdateOrderUseCase.Result.NOT_FOUND)
    }

    @Test
    fun `it returns NOT_FOUND if order id is not valid UUID for a preissued order number`() {
        every { orderInformationRepository.findById(any()) } returns orderInfo

        assertThat(
            underTest(
                orderId,
                issuerId,
                "notification"
            )
        ).isEqualTo(UpdateOrderUseCase.Result.NOT_FOUND)
    }

    @Test
    fun `it does not update the order if order id is not valid UUID for an EON`() {
        clearMocks(orderInformationRepository)

        every { orderInformationRepository.findById(any()) } returns orderInfo.copy(orderNumber = preissuedOrderNumber)

        underTest(orderId, null, notificationUrl)

        verify(exactly = 0) { orderInformationRepository.save(any()) }
    }

    @Test
    fun `it returns NotFound if order cannot be found`() {
        every { orderInformationRepository.findById(any()) } returns null
        assertThat(
            underTest(
                UUID.randomUUID(),
                null,
                notificationUrl
            )
        ).isEqualTo(UpdateOrderUseCase.Result.NOT_FOUND)
    }
}
