package com.healthmetrix.labres

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

abstract class ApiResponse(
    @JsonIgnore
    val httpStatus: HttpStatus,

    @JsonIgnore
    val hasBody: Boolean = true,

    @JsonIgnore
    val headers: HttpHeaders = HttpHeaders()
)

fun <T : ApiResponse> T.asEntity(): ResponseEntity<T> {
    return ResponseEntity
        .status(this.httpStatus)
        .headers(headers)
        .body(if (hasBody) this else null)
}
