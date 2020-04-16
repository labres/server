package com.healthmetrix.labres.lab

import com.healthmetrix.labres.ApiResponse
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
        path = ["/v1/orders/result/json"],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun jsonResult(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        labIdHeader: String,
        @RequestBody
        jsonResult: JsonResult
    ): ResponseEntity<UpdateStatusResponse> {
        val orderNumber = OrderNumber.Internal.from(extractLabIdUseCase(labIdHeader), jsonResult.internalOrderNumber)
            ?: OrderNumber.External.from(jsonResult.externalOrderNumber)
            ?: return UpdateStatusResponse.OrderNotFound.asEntity()

        return updateResultUseCase(LabResult(orderNumber, jsonResult.result)).asEntity()
    }

    @PutMapping(
        path = ["/v1/orders/result/obx"],
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
        path = ["/v1/orders/result/ldt"],
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
    val externalOrderNumber: String? = null,
    val internalOrderNumber: String? = null,
    val result: Result
)

data class LabResult(val orderNumber: OrderNumber, val result: Result)

enum class Result {
    POSITIVE,
    WEAK_POSITIVE,
    NEGATIVE,
    INVALID;

    fun asStatus() = when (this) {
        POSITIVE -> Status.POSITIVE
        WEAK_POSITIVE -> Status.WEAK_POSITIVE
        NEGATIVE -> Status.NEGATIVE
        INVALID -> Status.INVALID
    }

    companion object {
        fun from(s: String): Result? = try {
            valueOf(s)
        } catch (ex: Exception) {
            null
        }
    }
}

sealed class UpdateStatusResponse(httpStatus: HttpStatus, hasBody: Boolean = false) : ApiResponse(httpStatus, hasBody) {
    object Success : UpdateStatusResponse(HttpStatus.OK)
    object OrderNotFound : UpdateStatusResponse(HttpStatus.NOT_FOUND)
    object InfoUnreadable : UpdateStatusResponse(HttpStatus.INTERNAL_SERVER_ERROR, true) {
        val message = "Unable to read status"
    }
}
