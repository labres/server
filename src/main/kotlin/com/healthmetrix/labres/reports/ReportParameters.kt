package com.healthmetrix.labres.reports

data class ReportParameters(
    val event: String,
    val reportedAfter: Long?,
    val sampledAfter: Long?
)
