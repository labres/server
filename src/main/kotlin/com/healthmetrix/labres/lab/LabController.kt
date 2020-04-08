package com.healthmetrix.labres.lab

import com.healthmetrix.labres.ApiResponse
import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import java.time.Instant
import java.util.Date
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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

    @PutMapping(
        path = ["/v1/order/{externalOrderNumber}/result"],
        consumes = [MediaType.TEXT_PLAIN_VALUE]
    )
    fun ldtResult(
        @PathVariable externalOrderNumber: String,
        @RequestBody ldt: String
    ): ResponseEntity<UpdateStatusResponse> {
        val orderInfo = OrderNumber.from(externalOrderNumber)?.let {
            orderInformationRepository.findById(it.externalOrderNumber)
        } ?: return UpdateStatusResponse.OrderNotFound.asEntity()

        val ldtInfo = extractInfoUseCase(ldt.trim())
            ?: return UpdateStatusResponse.InfoUnreadable.asEntity()

        orderInformationRepository.save(
            OrderInformation(
                number = orderInfo.number,
                status = ldtInfo.status,
                hash = ldtInfo.hash,
                updatedAt = Date.from(Instant.now())
            )
        )

        return UpdateStatusResponse.Success.asEntity()
    }

    @PutMapping(
        path = ["/v1/order/{externalOrderNumber}/result"],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun jsonResult(
        @PathVariable externalOrderNumber: String,
        @RequestBody labResult: LabResult
    ): ResponseEntity<UpdateStatusResponse> {
        val orderInfo = OrderNumber.from(externalOrderNumber)?.let {
            orderInformationRepository.findById(it.externalOrderNumber)
        } ?: return UpdateStatusResponse.OrderNotFound.asEntity()

        orderInformationRepository.save(
            OrderInformation(
                number = orderInfo.number,
                status = labResult.result.asStatus(),
                hash = labResult.hash,
                updatedAt = Date.from(Instant.now())
            )
        )

        return UpdateStatusResponse.Success.asEntity()
    }
}

data class LabResult(val result: Result, val hash: String)

enum class Result {
    POSITIVE,
    NEGATIVE,
    INVALID;

    fun asStatus() = when (this) {
        POSITIVE -> Status.POSITIVE
        NEGATIVE -> Status.NEGATIVE
        INVALID -> Status.INVALID
    }
}

sealed class UpdateStatusResponse(httpStatus: HttpStatus, hasBody: Boolean = false) : ApiResponse(httpStatus, hasBody) {
    object Success : UpdateStatusResponse(HttpStatus.OK)
    object OrderNotFound : UpdateStatusResponse(HttpStatus.NOT_FOUND)
    object InfoUnreadable : UpdateStatusResponse(HttpStatus.INTERNAL_SERVER_ERROR, true) {
        val message = "Unable to read status"
    }
}
