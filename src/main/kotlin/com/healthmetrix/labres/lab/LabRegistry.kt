package com.healthmetrix.labres.lab

import com.healthmetrix.labres.order.EON_ISSUER_ID
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("labres.registry")
@ConstructorBinding
class LabRegistry(
    private val labs: List<RegisteredLab>
) {
    fun get(labId: String) = labs.firstOrNull { labId == it.id }
}

class RegisteredLab(
    val id: String,
    private val issuers: List<String>
) {
    fun canUpdateResultFor(issuerId: String?) =
        issuers.contains(issuerId ?: EON_ISSUER_ID)
}
