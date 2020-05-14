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

    @Test
    fun `eons have an implicit issuer`() {
        val eon = OrderNumber.External.from("1234567890")
        assertThat(eon.issuerId).isEqualTo(EON_ISSUER_ID)
    }

    @Test
    fun `factory method should create an eon if the issuerId is null`() {
        assertThat(OrderNumber.from(null, "1234567890")).isInstanceOf(OrderNumber.External::class.java)
    }

    @Test
    fun `factory method should create an eon if the issuerId is "labres"`() {
        assertThat(OrderNumber.from("labres", "1234567890")).isInstanceOf(OrderNumber.External::class.java)
    }

    @Test
    fun `factory method should create an pon if the issuerId is not "labres" or null`() {
        assertThat(OrderNumber.from("something", "test")).isInstanceOf(OrderNumber.PreIssued::class.java)
    }
}
