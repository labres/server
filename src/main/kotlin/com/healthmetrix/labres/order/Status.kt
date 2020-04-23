package com.healthmetrix.labres.order

import com.healthmetrix.labres.LabResApiResponse
import io.swagger.v3.oas.annotations.media.Schema
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
    data class Found(
        @Schema(
            description = "Status enum to indicate if the lab result is in progress, sample material has been found invalid,  or if it has a positive or negative result"
        )
        val status: Status
    ) : StatusResponse(HttpStatus.OK)

    object NotFound : StatusResponse(HttpStatus.NOT_FOUND, false)
}
