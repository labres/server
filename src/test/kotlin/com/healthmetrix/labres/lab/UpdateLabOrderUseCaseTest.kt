package com.healthmetrix.labres.lab

import com.healthmetrix.labres.OrderId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UpdateLabOrderUseCaseTest {
    private val underTest = UpdateLabOrderUseCase()

    @Test
    fun `it returns a result with id and lab order number`() {
        val result = underTest("orderId")
        assertThat(result.id).isInstanceOf(OrderId::class.java)
        assertThat(result.labOrderNumber).isEqualTo("orderId")
    }
}
