package com.healthmetrix.labres.lab

import com.healthmetrix.labres.order.Status
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GetLabOrderUseCaseTest {
    private val underTest = GetLabOrderUseCase()

    @Test
    fun `it returns a result with status`() {
        val result = underTest("orderId")
        assertThat(result?.status).isEqualTo(Status.POSITIVE)
    }
}
