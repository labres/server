package com.healthmetrix.labres.reports

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("labres.registry")
@ConstructorBinding
class ReportClientAuthenticator(
    private val reportClients: List<RegisteredClient>
) {
    fun getWhitelistedTestSites(client: String) =
        reportClients.firstOrNull { it.userName == client && it.testSiteIds.isNotEmpty() }?.testSiteIds
}

data class RegisteredClient(
    val userName: String,
    val testSiteIds: List<String>
)
