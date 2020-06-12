package com.healthmetrix.labres

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

@ControllerAdvice
class GlobalErrorHandler {

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun onException(exception: Exception): ResponseEntity<Error> {
        val id = UUID.randomUUID()
        logger.error("Exception caught incidentId=$id", exception)
        return Error.InternalServerError(id).asEntity()
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun onMethodNotSupported(exception: HttpRequestMethodNotSupportedException) =
        badRequest(exception, exception.message ?: "Request method not supported")

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun onContentTypeNotSupported(exception: HttpMediaTypeNotSupportedException) =
        badRequest(exception, exception.message ?: "Content type not supported")

    @ExceptionHandler(MissingRequestHeaderException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun onMissingRequestHeader(exception: MissingRequestHeaderException) =
        badRequest(exception, "Missing request header '${exception.headerName}'")

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun onHttpMessageNotReadable(exception: HttpMessageNotReadableException) =
        badRequest(exception, "Malformed request body. Cause: ${exception.message ?: "unknown"}")

    private fun badRequest(exception: Exception, msg: String? = null): ResponseEntity<Error.BadRequest> {
        val id = UUID.randomUUID()
        logger.error("Exception caught incidentId=$id", exception)
        return Error.BadRequest(msg ?: id.toString()).asEntity()
    }

    sealed class Error(status: HttpStatus) : LabResApiResponse(status) {
        @Schema(description = "General error response body")
        class InternalServerError(incidentId: UUID) : Error(HttpStatus.INTERNAL_SERVER_ERROR) {
            @Schema(description = "An incident UUID for an unexpected error")
            val message = incidentId.toString()
        }

        @Schema(description = "General error response body")
        data class BadRequest(
            @Schema(
                description = "A summary of the error that occurred",
                example = "Malformed request body. Cause: JsonParseException"
            )
            val message: String
        ) : Error(HttpStatus.BAD_REQUEST)
    }
}
