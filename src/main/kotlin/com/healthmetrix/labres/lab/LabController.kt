package com.healthmetrix.labres.lab

import com.healthmetrix.labres.ApiResponse
import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.order.Status
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class LabController(
    private val extractResultUseCase: ExtractResultUseCase,
    private val updateResultUseCase: UpdateResultUseCase
) {

    @PutMapping(
        path = ["/v1/order/{externalOrderNumber}/result"],
        consumes = [MediaType.TEXT_PLAIN_VALUE]
    )
    fun obxResult(
        @PathVariable externalOrderNumber: String,
        @RequestBody obxMessage: String
    ): ResponseEntity<UpdateStatusResponse> {
        val result = extractResultUseCase(obxMessage) ?: return UpdateStatusResponse.InfoUnreadable.asEntity()

        return updateResultUseCase(externalOrderNumber, LabResult(result)).asEntity()
    }

    @PutMapping(
        path = ["/v1/order/{externalOrderNumber}/result"],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun jsonResult(
        @PathVariable externalOrderNumber: String,
        @RequestBody labResult: LabResult
    ): ResponseEntity<UpdateStatusResponse> = updateResultUseCase(externalOrderNumber, labResult).asEntity()
}

data class LabResult(val result: Result)

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
