package com.healthmetrix.labres.lab

import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Status

data class LabResult(
    val orderNumber: OrderNumber,
    val labId: String,
    val result: Result,
    val testType: String?
) {
    val isEmptyLabResult = result == Result.IN_PROGRESS
}

enum class Result {
    POSITIVE,
    WEAK_POSITIVE,
    NEGATIVE,
    IN_PROGRESS,
    INVALID;

    fun asStatus() = when (this) {
        POSITIVE -> Status.POSITIVE
        WEAK_POSITIVE -> Status.WEAK_POSITIVE
        NEGATIVE -> Status.NEGATIVE
        INVALID -> Status.INVALID
        IN_PROGRESS -> Status.IN_PROGRESS
    }

    companion object {
        fun from(s: String): Result? = try {
            valueOf(s)
        } catch (ex: Exception) {
            null
        }
    }
}
