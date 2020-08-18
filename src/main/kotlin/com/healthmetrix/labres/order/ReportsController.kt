package com.healthmetrix.labres.order

import com.fasterxml.jackson.annotation.JsonProperty
import com.healthmetrix.labres.GlobalErrorHandler
import com.healthmetrix.labres.REPORTS_API_TAG
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

const val REPORTS_API_BASE = "/v1/reports"

@RestController
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = [
                Content(
                    schema = Schema(
                        type = "object",
                        implementation = GlobalErrorHandler.Error.BadRequest::class
                    )
                )
            ]
        ),
        ApiResponse(
            responseCode = "401",
            description = "API key invalid or missing",
            headers = [Header(name = "WWW-Authenticate", schema = Schema(type = "string"))],
            content = [Content()]
        ),
        ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = [
                Content(
                    schema = Schema(
                        type = "object",
                        implementation = GlobalErrorHandler.Error::class
                    )
                )
            ]
        )
    ]
)
@SecurityRequirement(name = "ReportsApiToken")
@Tag(name = REPORTS_API_TAG)
class ReportsController {

    @GetMapping(
        path = ["$REPORTS_API_BASE/lms"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        summary = "Returns a report including all ticket numbers for negative test results of a test site matching the given filters"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Returns the report with all LmsTickets for the given event with negative test results taken at the given test sites",
                content = [
                    Content(schema = Schema(type = "object", implementation = LmsReport::class))
                ]
            ),
            ApiResponse(
                responseCode = "403",
                description = "User is not allowed to query reports for given testSiteId",
                content = [Content()]
            )
        ]
    )
    fun generateLmsTicketReport(
        @Parameter(
            description = "Report only tickets with tests having been issued at one of the given test sites. The basic auth user has to be whitelisted for all test sites queried.",
            required = true,
            allowEmptyValue = false,
            array = ArraySchema(
                schema = Schema(
                    description = "Identifier for a test site",
                    type = "string",
                    example = "lms-test-site-1"
                )
            )
        )
        @RequestParam
        testSiteIds: List<String>,
        @Parameter(
            description = "Report only tickets for a certain event identified by this string",
            required = true,
            allowEmptyValue = false,
            example = "sp-1"
        )
        @RequestParam
        event: String,
        @Parameter(
            description = "Filter tickets in report to only be included if having been reported by the lab after the given UNIX epoch timestamp",
            required = false,
            example = "1596184744"
        )
        @RequestParam(required = false)
        reportedAfter: Long? = null,
        @Parameter(
            description = "Filter tickets in report to only be included if having been sampled at the test site after the given UNIX epoch timestamp",
            required = false,
            example = "1596184744"
        )
        @RequestParam(required = false)
        sampledAfter: Long? = null,
        @Parameter(
            description = "Used for pagination to determine which page to return. Use nextKey on the LmsReport for this to retrieve the next page of a result. When null, the first page will be returned.",
            required = false,
            example = "4c69efc0-88de-4b2b-9d53-34ec3e9bc26f"
        )
        @RequestParam(required = false, value = "startKey")
        lastKeyEvaluated: String? = null,
        @Parameter(
            description = "Limit the size of how many tickets should be included on a page of the returned report. If null, the limit of a response will be determined by how many tickets can fit in 1MB",
            required = false,
            example = "100"
        )
        @RequestParam(required = false)
        pageSize: Int? = null
    ): ResponseEntity<LmsReport> {
        TODO()
    }
}

data class LmsReport(
    @Schema(
        type = "string",
        nullable = true,
        description = "NextKey to load the next page of the report. Should be used as parameter startKey for the next request",
        example = "4c69efc0-88de-4b2b-9d53-34ec3e9bc26f"
    )
    @JsonProperty("nextKey")
    val lastKeyEvaluated: String?,
    @ArraySchema(
        minItems = 0,
        schema = Schema(
            type = "object",
            description = "A single LmsTicket in the report. The test for the ticket resolved to a negative result",
            implementation = LmsTicket::class
        )
    )
    val results: List<LmsTicket>
)

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
