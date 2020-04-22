package com.healthmetrix.labres

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

@ControllerAdvice
class GlobalErrorHandler {

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun onException(exception: Exception): ResponseEntity<InternalServerError> {
        val id = UUID.randomUUID()
        logger.error("Exception caught incidentId=$id", exception)
        return InternalServerError("$id").asEntity()
    }

    @Schema(description = "General error response body")
    data class InternalServerError(
        @Schema(
            description = "A summary of the error that occurred",
            example = "There was an error processing your request"
        )
        val message: String
    ) : LabResApiResponse(HttpStatus.INTERNAL_SERVER_ERROR)
}
