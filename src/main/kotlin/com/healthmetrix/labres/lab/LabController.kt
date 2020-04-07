package com.healthmetrix.labres.lab

import com.healthmetrix.labres.ApiResponse
import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import java.time.Instant
import java.util.Date
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class LabController(
    private val extractInfoUseCase: ExtractInfoUseCase,
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
        val info = extractInfoUseCase(ldt)

        if (orderInfo == null)
            return UpdateStatusResponse.OrderNotFound.asEntity()

        if (info == null)
            return UpdateStatusResponse.InfoUnreadable.asEntity()

        orderInformationRepository.save(
            OrderInformation(
                number = orderInfo.number,
                status = info.first,
                hash = info.second,
                updatedAt = Date.from(Instant.now())
            )
        )

        return UpdateStatusResponse.Success.asEntity()
    }
}

sealed class UpdateStatusResponse(httpStatus: HttpStatus) : ApiResponse(httpStatus, false) {
    object Success : UpdateStatusResponse(HttpStatus.OK)
    object OrderNotFound : UpdateStatusResponse(HttpStatus.NOT_FOUND)
    object InfoUnreadable : UpdateStatusResponse(HttpStatus.INTERNAL_SERVER_ERROR) {
        val message = "Unable to read status"
    }
}
