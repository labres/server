package com.healthmetrix.labres.reports

import io.swagger.v3.oas.annotations.media.Schema

data class LmsTicket(
    @Schema(
        type = "object",
        implementation = LmsTicketIdentifier::class,
        description = "The identifier for the given LmsTicket"
    )
    val data: LmsTicketIdentifier,
    @Schema(
        description = "Unix Epoch timestamp when the test result has been reported by the lab",
        nullable = true,
        required = false,
        example = "1596184744"
    )
    val reportedAt: Long,
    @Schema(
        description = "Unix Epoch timestamp when the lab test order registration has been received in the system",
        nullable = true,
        required = false,
        example = "1596184744"
    )
    val issuedAt: Long,
    @Schema(
        description = "Unix Epoch timestamp when the sample has been taken at the given test site",
        nullable = true,
        required = false,
        example = "1596184744"
    )
    val sampledAt: Long?
)

data class LmsTicketIdentifier(
    @Schema(
        type = "string",
        description = "The identifier of the event that lab test has been issued for",
        example = "sp-1"
    )
    val event: String,
    @Schema(
        type = "string",
        description = "The actual identifier of the personalised ticket",
        example = "43cd3c91-f619-4b87-8421-dce1a4d3912d"
    )
    val ticket: String
)
