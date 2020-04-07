package com.healthmetrix.labres.order

import com.healthmetrix.labres.ApiResponse
import org.springframework.http.HttpStatus

enum class Status {
    POSITIVE,
    NEGATIVE,
    INVALID,
    IN_PROGRESS
}

sealed class StatusResponse(httpStatus: HttpStatus, hasBody: Boolean = true) : ApiResponse(httpStatus, hasBody) {
    data class Found(val status: Status) : StatusResponse(HttpStatus.OK)

    object NotFound : StatusResponse(HttpStatus.NOT_FOUND, false)
}
