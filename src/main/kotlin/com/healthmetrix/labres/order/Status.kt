package com.healthmetrix.labres.order

import com.fasterxml.jackson.annotation.JsonInclude
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Found(
        @Schema(
            description = "Status enum to indicate if the lab result is in progress, sample material has been found invalid,  or if it has a positive or negative result"
        )
        val status: Status,

        @Schema(
            description = "Unix Epoch timestamp when the sample has been taken",
            nullable = true,
            required = false,
            example = "1596184744"
        )
        val sampledAt: Long?
    ) : StatusResponse(HttpStatus.OK)

    object NotFound : StatusResponse(HttpStatus.NOT_FOUND, false)
    object Forbidden : StatusResponse(HttpStatus.FORBIDDEN, false)
    data class BadRequest(val message: String) : StatusResponse(HttpStatus.BAD_REQUEST, true)
}
