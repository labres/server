package com.healthmetrix.labres.order

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OrderNumberExternalTest {

    @Test
    fun `randomly generated IDs match the form of external order numbers`() {
        val allMatch = (0 until 100)
            .map { OrderNumber.External.random() }
            .all { it.number.matches(Regex("^[0123456789]{10}$")) }

        assertThat(allMatch).isEqualTo(true)
    }
}
