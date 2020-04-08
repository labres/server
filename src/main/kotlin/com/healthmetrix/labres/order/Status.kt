package com.healthmetrix.labres.order

import com.healthmetrix.labres.ApiResponse
import org.springframework.http.HttpStatus

enum class Status {
    POSITIVE,
    NEGATIVE,
    INVALID,
    IN_PROGRESS;

    companion object {
        fun from(string: String): Status? = try {
            valueOf(string)
        } catch (ex: Exception) {
            null
        }
    }
}

sealed class StatusResponse(httpStatus: HttpStatus, hasBody: Boolean = true) : ApiResponse(httpStatus, hasBody) {
    data class Found(val status: Status) : StatusResponse(HttpStatus.OK)

    object NotFound : StatusResponse(HttpStatus.NOT_FOUND, false)

    object WrongHash : StatusResponse(HttpStatus.FORBIDDEN, false)
}
