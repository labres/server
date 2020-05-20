package com.healthmetrix.labres.order

import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class QueryStatusUseCase(private val repository: OrderInformationRepository) {

    operator fun invoke(id: UUID, issuerId: String?) = repository
        .findById(id)
        ?.takeIf { it.isIssuedBy(issuerId) }
        ?.let(OrderInformation::status)

    private fun OrderInformation?.isIssuedBy(issuerId: String?) =
        this?.orderNumber?.issuerId == (issuerId ?: EON_ISSUER_ID)
}
