package com.healthmetrix.labres.reports

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ReportAuthenticatorTest {

    private val userName = "hmx"
    private val testSiteIds = listOf("test-site-1", "test-site-2")
    private val reportClients = listOf(
        RegisteredClient(
            userName = userName,
            testSiteIds = testSiteIds
        ),
        RegisteredClient(
            userName = "hmx-2",
            testSiteIds = listOf()
        )
    )
    private val underTest = ReportClientAuthenticator(reportClients)

    @Test
    fun `it should return test site ids for a registered client`() {
        val res = underTest.getWhitelistedTestSites(userName)

        assertThat(res).isEqualTo(testSiteIds)
    }

    @Test
    fun `it should return null for a registered client with not test site ids`() {
        val res = underTest.getWhitelistedTestSites("hmx-2")

        assertThat(res).isNull()
    }

    @Test
    fun `it should return null for an unregistered client`() {
        val res = underTest.getWhitelistedTestSites("hmx-3")

        assertThat(res).isNull()
    }
}
