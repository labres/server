package com.healthmetrix.labres.lab

import com.healthmetrix.labres.LabResApiResponse
import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Status
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class LabController(
    private val extractLabIdUseCase: ExtractLabIdUseCase,
    private val extractObxResultUseCase: ExtractObxResultUseCase,
    private val extractLdtResultUseCase: ExtractLdtResultUseCase,
    private val updateResultUseCase: UpdateResultUseCase
) {

    @PutMapping(
        path = ["/v1/results/json"],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun jsonResult(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        labIdHeader: String,
        @RequestBody
        result: JsonResult
    ): ResponseEntity<UpdateStatusResponse> {
        val orderNumber = OrderNumber.External.from(result.orderNumber)
            ?: return UpdateStatusResponse.OrderNotFound.asEntity()

        val labId = extractLabIdUseCase(labIdHeader) ?: return UpdateStatusResponse.OrderNotFound.asEntity()

        return updateResultUseCase(LabResult(orderNumber, labId, result.result)).asEntity()
    }

    @PutMapping(
        path = ["/v1/results/obx"],
        consumes = [MediaType.TEXT_PLAIN_VALUE]
    )
    fun obxResult(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        labIdHeader: String,
        @RequestBody
        obxMessage: String
    ): ResponseEntity<UpdateStatusResponse> {
        val labId = extractLabIdUseCase(labIdHeader)
            ?: return UpdateStatusResponse.OrderNotFound.asEntity()

        val labResult = extractObxResultUseCase(obxMessage, labId)
            ?: return UpdateStatusResponse.InfoUnreadable.asEntity()

        return updateResultUseCase(labResult).asEntity()
    }

    @PutMapping(
        path = ["/v1/results/ldt"],
        consumes = [MediaType.TEXT_PLAIN_VALUE]
    )
    fun ldtResult(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        labIdHeader: String,
        @RequestBody
        ldtMessage: String
    ): ResponseEntity<UpdateStatusResponse> {
        val labId = extractLabIdUseCase(labIdHeader)
            ?: return UpdateStatusResponse.OrderNotFound.asEntity()

        val labResult = extractLdtResultUseCase(ldtMessage, labId)
            ?: return UpdateStatusResponse.InfoUnreadable.asEntity()

        return updateResultUseCase(labResult).asEntity()
    }
}

data class JsonResult(
    val orderNumber: String,
    val result: Result
)

data class LabResult(val orderNumber: OrderNumber, val labId: String, val result: Result)

enum class Result {
    POSITIVE,
    WEAK_POSITIVE,
    NEGATIVE,
    IN_PROGRESS,
    INVALID;

    fun asStatus() = when (this) {
        POSITIVE -> Status.POSITIVE
        WEAK_POSITIVE -> Status.WEAK_POSITIVE
        NEGATIVE -> Status.NEGATIVE
        INVALID -> Status.INVALID
        IN_PROGRESS -> Status.IN_PROGRESS
    }

    companion object {
        fun from(s: String): Result? = try {
            valueOf(s)
        } catch (ex: Exception) {
            null
        }
    }
}

sealed class UpdateStatusResponse(httpStatus: HttpStatus, hasBody: Boolean = false) : LabResApiResponse(httpStatus, hasBody) {
    object Success : UpdateStatusResponse(HttpStatus.OK)
    object OrderNotFound : UpdateStatusResponse(HttpStatus.NOT_FOUND)
    object InfoUnreadable : UpdateStatusResponse(HttpStatus.BAD_REQUEST, true) {
        val message = "Unable to read status"
    }
}
