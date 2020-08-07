package com.healthmetrix.labres.order

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import org.springframework.stereotype.Service

@Service
class FindOrderByOrderNumberUseCase(private val repository: OrderInformationRepository) {

    operator fun invoke(orderNumber: OrderNumber, sample: Sample, verificationSecret: String): Result<OrderInformation, FindOrderError> {
        val orderInformation = repository.findByOrderNumberAndSample(orderNumber, sample)
            .maxBy { it.issuedAt } ?: return Err(FindOrderError.NOT_FOUND)

        if (orderInformation.verificationSecret == null || orderInformation.verificationSecret != verificationSecret) {
            return Err(FindOrderError.FORBIDDEN)
        }

        return Ok(orderInformation)
    }

    enum class FindOrderError {
        NOT_FOUND,
        FORBIDDEN
    }
}
