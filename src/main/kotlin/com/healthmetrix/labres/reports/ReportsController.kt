package com.healthmetrix.labres.reports

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.healthmetrix.labres.GlobalErrorHandler
import com.healthmetrix.labres.LabResApiResponse
import com.healthmetrix.labres.REPORTS_API_TAG
import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.encodeBase64
import com.healthmetrix.labres.extractBasicAuthUser
import com.healthmetrix.labres.logger
import com.healthmetrix.labres.toHttpHeaders
import com.healthmetrix.labres.unify
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
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

const val REPORTS_API_BASE = "/v1/reports"
private const val LMS_AUTHORIZATION_REALM = "Basic realm=\"labres:reports:lms:read\""

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
class ReportsController(
    private val createLmsReportUseCase: CreateLmsReportUseCase,
    private val objectMapper: ObjectMapper
) {

    @GetMapping(
        path = ["$REPORTS_API_BASE/lms"],
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
                    Content(schema = Schema(type = "object", implementation = LmsReportResponse.Success::class))
                ]
            ),
            ApiResponse(
                responseCode = "403",
                description = "User is not allowed to query reports for given testSiteId",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Report parameters (event, sampledAfter or reportedAfter) have been changed and do not match startKey",
                content = [Content(schema = Schema(type = "object", implementation = LmsReportResponse.BadRequest::class))]
            )
        ]
    )
    fun generateLmsTicketReport(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        authorizationHeader: String?,
        @Parameter(
            description = "Report only tickets for a certain event identified by this string",
            required = true,
            allowEmptyValue = false,
            example = "sp-1"
        )
        @RequestParam
        event: String,
        @Parameter(
            description = "Filter tickets in report to only be included if having been reported by the lab after the given UNIX epoch timestamp in seconds",
            required = false,
            example = "1596184744"
        )
        @RequestParam(required = false)
        reportedAfter: Long? = null,
        @Parameter(
            description = "Filter tickets in report to only be included if having been sampled at the test site after the given UNIX epoch timestamp in seconds",
            required = false,
            example = "1596184744"
        )
        @RequestParam(required = false)
        sampledAfter: Long? = null,
        @Parameter(
            description = "Used for pagination to determine which page to return. Use nextKey on the LmsReport for this to retrieve the next page of a result. When null, the first page will be returned.",
            schema = Schema(type = "string", format = "Base64"),
            required = false,
            example = "eyJyZXBvcnRQYXJhbWV0ZXJzIjp7ImV2ZW50IjoiYW5vdGhlciIsInJlcG9ydGVkQWZ0ZXIiOm51bGwsInNhbXBsZWRBZnRlciI6bnVsbH0sImV4Y2x1c2l2ZVN0YXJ0S2V5IjoiNDcwNDI0NWQtZDhhOC00NzI5LTlhY2EtY2RlZDkwOTEzNGIzIn0="
        )
        @RequestParam(required = false, value = "startKey")
        paginationToken: PaginationToken? = null,
        @Parameter(
            description = "Limit the size of how many tickets should be included on a page of the returned report. If null, the limit of a response will be determined by how many tickets can fit in 1MB",
            required = false,
            example = "100"
        )
        @RequestParam(required = false)
        pageSize: Int? = null
    ): ResponseEntity<LmsReportResponse> {
        val reportClient = authorizationHeader?.extractBasicAuthUser()
            ?: return LmsReportResponse.Unauthorized.asEntity()

        val params = ReportParameters(
            event = event,
            reportedAfter = reportedAfter,
            sampledAfter = sampledAfter
        )

        return createLmsReportUseCase.invoke(
            client = reportClient,
            params = params,
            paginationToken = paginationToken,
            pageSize = pageSize
        ).onFailure { error ->
            logger.info(
                "[{}]: Error for client {} and event {}: {}",
                kv("method", "createLmsReport"),
                kv("client", reportClient),
                kv("event", event),
                kv("error", error.name)
            )
        }.mapError { error ->
            when (error) {
                CreateLmsReportUseCase.Error.FORBIDDEN -> LmsReportResponse.Forbidden
                CreateLmsReportUseCase.Error.BAD_REQUEST -> LmsReportResponse.BadRequest()
            }
        }.onSuccess {
            logger.debug(
                "[{}]: Success for client {} and event {}",
                kv("method", "createLmsReport"),
                kv("client", reportClient),
                kv("event", event)
            )
        }.map { report ->
            LmsReportResponse.Success(
                results = report.results,
                nextKey = report.paginationToken?.encodeBase64()
            )
        }.unify().asEntity()
    }

    private fun PaginationToken.encodeBase64() = this.let(objectMapper::writeValueAsString).encodeBase64()

    sealed class LmsReportResponse(
        httpStatus: HttpStatus,
        hasBody: Boolean = false,
        headers: HttpHeaders = HttpHeaders.EMPTY
    ) :
        LabResApiResponse(httpStatus, hasBody, headers) {

        data class Success(
            @ArraySchema(
                minItems = 0,
                schema = Schema(
                    type = "object",
                    description = "A single LmsTicket in the report. The test for the ticket resolved to a negative result",
                    implementation = LmsTicket::class
                )
            )
            val results: List<LmsTicket>,
            @Schema(
                type = "string",
                format = "base64",
                nullable = true,
                description = "NextKey to load the next page of the report. Should be used as parameter startKey for the next request",
                example = "eyJyZXBvcnRQYXJhbWV0ZXJzIjp7ImV2ZW50IjoiYW5vdGhlciIsInJlcG9ydGVkQWZ0ZXIiOm51bGwsInNhbXBsZWRBZnRlciI6bnVsbH0sImV4Y2x1c2l2ZVN0YXJ0S2V5IjoiNDcwNDI0NWQtZDhhOC00NzI5LTlhY2EtY2RlZDkwOTEzNGIzIn0="
            )
            val nextKey: String?
        ) : LmsReportResponse(
            httpStatus = HttpStatus.OK,
            hasBody = true
        )

        object Unauthorized : LmsReportResponse(
            httpStatus = HttpStatus.UNAUTHORIZED,
            headers = mapOf(
                HttpHeaders.WWW_AUTHENTICATE to listOf(
                    LMS_AUTHORIZATION_REALM
                )
            ).toHttpHeaders()
        )

        object Forbidden : LmsReportResponse(HttpStatus.FORBIDDEN)

        data class BadRequest(
            val message: String = "Report parameters (event, sampledAfter or reportedAfter) have been changed and do not match startKey"
        ) : LmsReportResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            hasBody = true
        )
    }
}
