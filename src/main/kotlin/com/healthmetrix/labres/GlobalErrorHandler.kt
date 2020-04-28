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
    fun onException(exception: Exception) = handle(exception)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun onMethodNotSupported(exception: HttpRequestMethodNotSupportedException) =
        handle(exception, exception.message ?: "Request method not supported")

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun onContentTypeNotSupported(exception: HttpMediaTypeNotSupportedException) =
        handle(exception, exception.message ?: "Content type not supported")

    @ExceptionHandler(MissingRequestHeaderException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun onMissingRequestHeader(exception: MissingRequestHeaderException) =
        handle(exception, "Missing request header '${exception.headerName}'")

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun onHttpMessageNotReadable(exception: HttpMessageNotReadableException) =
        handle(exception, "Malformed request body. Cause: ${exception.cause?.javaClass?.simpleName ?: "unknown"}")

    private fun handle(exception: Exception, msg: String? = null): ResponseEntity<InternalServerError> {
        val id = UUID.randomUUID()
        logger.error("Exception caught incidentId=$id", exception)
        return InternalServerError(msg ?: id.toString()).asEntity()
    }

    @Schema(description = "General error response body")
    data class InternalServerError(
        @Schema(
            description = "A summary of the error that occurred, or an incident ID if the error is unexpected",
            example = "Malformed request body. Cause: JsonParseException"
        )
        val message: String
    ) : LabResApiResponse(HttpStatus.INTERNAL_SERVER_ERROR)
}
