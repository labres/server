package com.healthmetrix.labres.order

import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UpdateOrderUseCaseTest {
    private val orderInformationRepository: OrderInformationRepository = mockk()

    private val underTest = UpdateOrderUseCase(orderInformationRepository)

    val orderId = UUID.randomUUID()
    private val orderInfo = OrderInformation(
        id = orderId,
        number = OrderNumber.External.from("1234567891")!!,
        status = Status.IN_PROGRESS,
        issuedAt = Date.from(Instant.now())
    )

    @Test
    fun `it returns Success with notification id`() {
        every { orderInformationRepository.findById(any()) } returns orderInfo
        every { orderInformationRepository.save(any()) } returns orderInfo

        val result = underTest(orderId.toString(), "notificationId")
        assertThat(result).isInstanceOf(UpdateOrderUseCase.Result.Success::class.java)
    }

    @Test
    fun `it returns InvalidOrderId if order id is not valid UUID`() {
        assertThat(
            underTest(
                "notAnId",
                "notification"
            )
        ).isInstanceOf(UpdateOrderUseCase.Result.InvalidOrderId::class.java)
    }

    @Test
    fun `it returns NotFound if order cannot be found`() {
        every { orderInformationRepository.findById(any()) } returns null
        assertThat(
            underTest(
                UUID.randomUUID().toString(),
                "notification"
            )
        ).isInstanceOf(UpdateOrderUseCase.Result.NotFound::class.java)
    }
}
