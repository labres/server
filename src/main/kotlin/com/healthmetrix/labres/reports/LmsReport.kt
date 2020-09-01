package com.healthmetrix.labres.reports

data class LmsReport(
    val paginationToken: PaginationToken?,
    val results: List<LmsTicket>
)
