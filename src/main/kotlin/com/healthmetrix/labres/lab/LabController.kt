package com.healthmetrix.labres.lab

import com.healthmetrix.labres.ApiResponse
import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.persistence.OrderInformationRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class LabController(
    private val extractStatusUseCase: ExtractStatusUseCase,
    private val orderInformationRepository: OrderInformationRepository
) {

    @PutMapping("/v1/order/{externalOrderNumber}/result")
    fun putLabResult(
        @PathVariable externalOrderNumber: String,
        @RequestBody ldt: String
    ): ResponseEntity<UpdateStatusResponse> {
        val orderInfo = OrderNumber.from(externalOrderNumber)?.let {
            orderInformationRepository.findById(it.externalOrderNumber)
        }
        val status = extractStatusUseCase(ldt)

        if (orderInfo == null)
            return UpdateStatusResponse.OrderNotFound.asEntity()

        if (status == null)
            return UpdateStatusResponse.StatusUnreadable.asEntity()

        orderInformationRepository.save(orderInfo.copy(status = status))

        return UpdateStatusResponse.Success.asEntity()
    }
}

sealed class UpdateStatusResponse(httpStatus: HttpStatus) : ApiResponse(httpStatus, false) {
    object Success : UpdateStatusResponse(HttpStatus.OK)
    object OrderNotFound : UpdateStatusResponse(HttpStatus.NOT_FOUND)
    object StatusUnreadable : UpdateStatusResponse(HttpStatus.INTERNAL_SERVER_ERROR) {
        val message = "Unable to read status"
    }
}
