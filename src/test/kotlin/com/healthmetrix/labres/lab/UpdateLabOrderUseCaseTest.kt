package com.healthmetrix.labres.lab

import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UpdateLabOrderUseCaseTest {
    private val orderInformationRepository: OrderInformationRepository = mockk()
    private val underTest = UpdateLabOrderUseCase(orderInformationRepository)

    private val internalOrderNumber = OrderNumber.Internal.from("labId", "labOrderNumber")!!

    private val orderInformation = OrderInformation(
        UUID.randomUUID(),
        internalOrderNumber,
        Status.IN_PROGRESS,
        createdAt = Date.from(Instant.now()),
        updatedAt = null
    )

    @Test
    fun `it returns a result with id and lab order number when created`() {
        every { orderInformationRepository.findByOrderNumber(any()) } returns null
        every { orderInformationRepository.save(any()) } returns orderInformation
        val result = underTest("labId", "labOrderNumber")

        assertThat(result).isInstanceOf(UpdateLabOrderUseCase.Result.Created::class.java)
        result as UpdateLabOrderUseCase.Result.Created
        assertThat(result.id).isNotNull()
        assertThat(result.labOrderNumber).isEqualTo("labOrderNumber")
    }

    @Test
    fun `it returns a result with id and lab order when found`() {
        every { orderInformationRepository.findByOrderNumber(any()) } returns orderInformation

        val result = underTest("labId", "labOrderNumber")
        assertThat(result).isInstanceOf(UpdateLabOrderUseCase.Result.Found::class.java)
        result as UpdateLabOrderUseCase.Result.Found
        assertThat(result.id).isNotNull()
        assertThat(result.labOrderNumber).isEqualTo("labOrderNumber")
    }

    @Test
    fun `it returns InvalidOrderNumber when the lab order number is invalid`() {
        val result = underTest("labId", null)
        assertThat(result).isInstanceOf(UpdateLabOrderUseCase.Result.InvalidOrderNumber::class.java)
    }
}
