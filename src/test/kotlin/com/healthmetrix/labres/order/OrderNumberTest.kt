package com.healthmetrix.labres.order

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OrderNumberTest {

    @Test
    fun `randomly generated IDs match the form of external order numbers`() {
        val allMatch = (0 until 100)
            .map { OrderNumber.random() }
            .all { it.externalOrderNumber.matches(Regex("^[0123456789]{10}$")) }

        assertThat(allMatch).isEqualTo(true)
    }
}
