package com.healthmetrix.labres.lab

import com.healthmetrix.labres.order.EON_ISSUER_ID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RegisteredLabTest {

    @Test
    fun `canUpdateResultFor should return true for EON`() {
        val lab = RegisteredLab("lab", listOf(EON_ISSUER_ID))

        assertThat(lab.canUpdateResultFor(null)).isTrue()
    }

    @Test
    fun `canUpdateResultFor should return false for EON`() {
        val lab = RegisteredLab("lab", listOf("any"))

        assertThat(lab.canUpdateResultFor(null)).isFalse()
    }

    @Test
    fun `canUpdateResultFor should return true for issuer`() {
        val testIssuer = "issuer"
        val lab = RegisteredLab("lab", listOf(testIssuer))

        assertThat(lab.canUpdateResultFor(testIssuer)).isTrue()
    }

    @Test
    fun `canUpdateResultFor should return false for issuer`() {
        val testIssuer = "issuer"
        val lab = RegisteredLab("lab", listOf("any"))

        assertThat(lab.canUpdateResultFor(testIssuer)).isFalse()
    }
}
