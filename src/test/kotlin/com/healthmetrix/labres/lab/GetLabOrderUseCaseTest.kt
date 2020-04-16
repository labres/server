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

class GetLabOrderUseCaseTest {
    private val orderInformationRepository: OrderInformationRepository = mockk()
    private val underTest = GetLabOrderUseCase(orderInformationRepository)

    private val orderInformation = OrderInformation(
        UUID.randomUUID(),
        OrderNumber.Internal.from("fake", "fake")!!,
        Status.POSITIVE,
        Date.from(Instant.now()),
        null
    )

    @Test
    fun `it returns a result with status`() {
        every { orderInformationRepository.findById(any()) } returns orderInformation
        val result = underTest(UUID.randomUUID().toString())
        assertThat(result?.status).isEqualTo(Status.POSITIVE)
    }

    @Test
    fun `it returns NotFound when no order is found`() {
        every { orderInformationRepository.findById(any()) } returns null
        assertThat(underTest(UUID.randomUUID().toString())).isNull()
    }
}
