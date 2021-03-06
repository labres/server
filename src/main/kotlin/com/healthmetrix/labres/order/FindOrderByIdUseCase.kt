package com.healthmetrix.labres.order

import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FindOrderByIdUseCase(private val repository: OrderInformationRepository) {

    operator fun invoke(id: UUID, issuerId: String?) = repository
        .findById(id)
        ?.takeIf { it.isIssuedBy(issuerId) }

    private fun OrderInformation?.isIssuedBy(issuerId: String?) =
        this?.orderNumber?.issuerId == (issuerId ?: EON_ISSUER_ID)
}
