package com.healthmetrix.labres.order

data class StatusResponse(val status: Status)

enum class Status {
    POSITIVE,
    NEGATIVE,
    INVALID,
    IN_PROGRESS
}
