package com.healthmetrix.labres.order

import com.healthmetrix.labres.LabResApiResponse
import org.springframework.http.HttpStatus

enum class Status {
    POSITIVE,
    WEAK_POSITIVE,
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

sealed class StatusResponse(httpStatus: HttpStatus, hasBody: Boolean = true) : LabResApiResponse(httpStatus, hasBody) {
    data class Found(val status: Status) : StatusResponse(HttpStatus.OK)

    object NotFound : StatusResponse(HttpStatus.NOT_FOUND, false)
}
