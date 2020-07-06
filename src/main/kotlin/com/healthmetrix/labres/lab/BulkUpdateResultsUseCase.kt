package com.healthmetrix.labres.lab

import org.springframework.stereotype.Service

@Service
class BulkUpdateResultsUseCase(
    private val updateResultUseCase: UpdateResultUseCase,
    private val metrics: LabMetrics
) {

    operator fun invoke(results: List<UpdateResultRequest>, labId: String, issuerId: String?) =
        results.mapNotNull { updateResultRequest ->
            val result = updateResultUseCase(updateResultRequest, labId, issuerId)

            metrics.countUpdateResults(result, labId, issuerId)

            when (result) {
                UpdateResult.INVALID_ORDER_NUMBER -> BulkUpdateError("Failed to parse orderNumber: ${updateResultRequest.orderNumber}")
                UpdateResult.ORDER_NOT_FOUND -> BulkUpdateError("Order for orderNumber ${updateResultRequest.orderNumber} not found")
                UpdateResult.SUCCESS -> {
                    metrics.countPersistedTestResults(updateResultRequest.result, labId, issuerId)
                    null
                }
            }
        }

    data class BulkUpdateError(val message: String)
}
