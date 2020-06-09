package com.healthmetrix.labres.lab

import org.springframework.stereotype.Service

@Service
class BulkUpdateResultsUseCase(
    private val updateResultUseCase: UpdateResultUseCase
) {

    operator fun invoke(results: List<UpdateResultRequest>, labId: String, issuerId: String?) =
        results.mapNotNull { result ->
            when (updateResultUseCase(result, labId, issuerId)) {
                UpdateResult.INVALID_ORDER_NUMBER -> BulkUpdateError("Failed to parse orderNumber: ${result.orderNumber}")
                UpdateResult.ORDER_NOT_FOUND -> BulkUpdateError("Order for orderNumber ${result.orderNumber} not found")
                UpdateResult.SUCCESS -> null
            }
        }

    data class BulkUpdateError(val message: String)
}
