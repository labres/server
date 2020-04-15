package com.healthmetrix.labres

import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalErrorHandler {

    @ExceptionHandler(Exception::class)
    fun onException(exception: Exception): ResponseEntity<InternalServerError> {
        val id = UUID.randomUUID()
        logger.error("Exception caught incidentId=$id", exception)
        return InternalServerError("$id").asEntity()
    }

    data class InternalServerError(val message: String) : ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR)
}
