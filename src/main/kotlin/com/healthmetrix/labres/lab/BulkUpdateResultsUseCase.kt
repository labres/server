package com.healthmetrix.labres.lab

import com.healthmetrix.labres.order.OrderNumber
import org.springframework.stereotype.Service

@Service
class BulkUpdateResultsUseCase(
    private val updateResultUseCase: UpdateResultUseCase
) {

    operator fun invoke(results: List<JsonResult>, labId: String, issuerId: String?): List<BulkUploadError> {

        val errors = mutableListOf<BulkUploadError>()

        results.forEach { result ->
            val labResult = try {
                mapJsonResult(result, labId, issuerId)
            } catch (ex: IllegalArgumentException) {
                errors.add(BulkUploadError(ex.message ?: "Failed to parse orderNumber"))
                null
            }

            if (labResult != null)
                updateResultUseCase(labResult)
                    ?: errors.add(BulkUploadError("Order for orderNumber ${result.orderNumber} not found"))
        }

        return errors
    }

    private fun mapJsonResult(result: JsonResult, labId: String, issuerId: String?): LabResult {
        val orderNumber = OrderNumber.from(issuerId, result.orderNumber)

        return LabResult(
            orderNumber = orderNumber,
            labId = labId,
            result = result.result,
            testType = result.type
        )
    }
}
