package com.healthmetrix.labres.lab

import com.healthmetrix.labres.GlobalErrorHandler
import com.healthmetrix.labres.LABORATORY_API_TAG
import com.healthmetrix.labres.LABORATORY_BULK_API_TAG
import com.healthmetrix.labres.LabResApiResponse
import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.decodeBase64
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

const val LAB_API_BASE = "/v1/results"
private const val AUTHORIZATION_REALM = "Basic realm=\"labres:labresults:write\""

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
                )]
        ),
        ApiResponse(
            responseCode = "401",
            description = "API key invalid or missing",
            headers = [Header(name = "WWW-Authenticate", schema = Schema(type = "string"))],
            content = [Content()]
        ),
        ApiResponse(
            responseCode = "403",
            description = "LabId is not allowed to upload results for given issuerId",
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
@SecurityRequirement(name = "LabCredential")
class LabController(
    private val updateResultUseCase: UpdateResultUseCase,
    private val bulkUpdateResultsUseCase: BulkUpdateResultsUseCase,
    private val labRegistry: LabRegistry
) {

    @PutMapping(
        path = [LAB_API_BASE],
        consumes = [
            MediaType.APPLICATION_JSON_VALUE,
            APPLICATION_KEVB_CSV_VALUE
        ],
        produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE]
    )
    @Operation(
        summary = "Upload lab result. Supported formats: JSON, kevb+csv"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Result uploaded successfully",
                content = [
                    Content(schema = Schema(implementation = UpdateResultResponse.Success::class, hidden = true))
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "No order for order number (and issuer if provided) found",
                content = [
                    Content(schema = Schema(implementation = UpdateResultResponse.OrderNotFound::class, hidden = true))
                ]
            )
        ]
    )
    @Tag(name = LABORATORY_API_TAG)
    fun jsonResult(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        authorizationHeader: String,
        @RequestParam(required = false)
        issuerId: String?,
        @RequestBody
        request: UpdateResultRequest
    ): ResponseEntity<UpdateResultResponse> {
        val lab = extractLabIdFrom(authorizationHeader)
            ?.let(labRegistry::get)
            ?: return UpdateResultResponse.Unauthorized.asEntity()

        if (!lab.canUpdateResultFor(issuerId))
            return UpdateResultResponse.Forbidden.asEntity()

        return when (updateResultUseCase(request, lab.id, issuerId)) {
            UpdateResult.INVALID_ORDER_NUMBER -> UpdateResultResponse.InvalidRequest("Failed to parse orderNumber: ${request.orderNumber}")
            UpdateResult.ORDER_NOT_FOUND -> UpdateResultResponse.OrderNotFound
            UpdateResult.SUCCESS -> UpdateResultResponse.Success
        }.asEntity()
    }

    @PutMapping(
        path = ["$LAB_API_BASE/json"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE]
    )
    @Operation(
        summary = "Upload lab result via JSON"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Result uploaded successfully",
                content = [
                    Content(schema = Schema(implementation = UpdateResultResponse.Success::class, hidden = true))
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "No order for order number (and issuer if provided) found",
                content = [
                    Content(schema = Schema(implementation = UpdateResultResponse.OrderNotFound::class, hidden = true))
                ]
            )
        ]
    )
    @Tag(name = LABORATORY_API_TAG)
    fun jsonResultLegacy(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        labIdHeader: String,
        @RequestParam(required = false)
        issuerId: String?,
        @RequestBody
        request: UpdateResultRequest
    ): ResponseEntity<UpdateResultResponse> = jsonResult(labIdHeader, issuerId, request)

    @PutMapping(
        path = ["$LAB_API_BASE/bulk"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE]
    )
    @Operation(
        summary = "Upload multiple lab results at once via JSON"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Number of processed results that all have been successfully updated",
                content = [
                    Content(schema = Schema(implementation = BulkUpdateResultResponse.Success::class))
                ]
            )
        ]
    )
    @Tag(name = LABORATORY_BULK_API_TAG)
    fun bulkUploadJsonResults(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        authorizationHeader: String,
        @RequestParam(required = false)
        issuerId: String?,
        @RequestBody
        request: BulkUpdateResultRequest
    ): ResponseEntity<BulkUpdateResultResponse> {
        val lab = extractLabIdFrom(authorizationHeader)
            ?.let(labRegistry::get)
            ?: return BulkUpdateResultResponse.Unauthorized.asEntity()

        if (!lab.canUpdateResultFor(issuerId))
            return BulkUpdateResultResponse.Forbidden.asEntity()

        return bulkUpdateResultsUseCase(request.results, lab.id, issuerId).let { errors ->
            if (errors.any())
                BulkUpdateResultResponse.PartialBadRequest(errors)
            else
                BulkUpdateResultResponse.Success(request.results.size)
        }.asEntity()
    }

    private fun extractLabIdFrom(header: String): String? {
        val (prefix, encoded) = header.split(" ").also {
            if (it.size != 2) return null
        }

        if (prefix != "Basic")
            return null

        val decoded = encoded.decodeBase64()
            ?.split(":")
            ?: return null

        if (decoded.size != 2)
            return null

        return decoded.first()
    }
}

data class UpdateResultRequest(
    @Schema(
        description = "The external order number",
        example = "1234567890"
    )
    val orderNumber: String,
    @Schema(description = "The test result")
    val result: Result,
    @Schema(
        description = "The kind of test used to generate the given result.",
        nullable = true,
        required = false,
        defaultValue = "PCR",
        allowableValues = [PCR_LOINC],
        example = "NGS"
    )
    val type: TestType = TestType.PCR
)

sealed class UpdateResultResponse(
    httpStatus: HttpStatus,
    hasBody: Boolean = false,
    headers: HttpHeaders = HttpHeaders.EMPTY
) :
    LabResApiResponse(httpStatus, hasBody, headers) {

    object Success : UpdateResultResponse(HttpStatus.OK)
    object OrderNotFound : UpdateResultResponse(HttpStatus.NOT_FOUND)
    data class InvalidRequest(val message: String) : UpdateResultResponse(HttpStatus.BAD_REQUEST, hasBody = true)
    object Unauthorized : UpdateResultResponse(
        HttpStatus.UNAUTHORIZED, headers = mapOf(
            HttpHeaders.WWW_AUTHENTICATE to listOf(
                AUTHORIZATION_REALM
            )
        ).toHttpHeaders()
    )

    object Forbidden : UpdateResultResponse(HttpStatus.FORBIDDEN)
}

data class BulkUpdateResultRequest(val results: List<UpdateResultRequest>)

sealed class BulkUpdateResultResponse(
    httpStatus: HttpStatus,
    hasBody: Boolean = false,
    headers: HttpHeaders = HttpHeaders.EMPTY
) :
    LabResApiResponse(httpStatus, hasBody, headers) {

    data class Success(val processedRows: Int) : BulkUpdateResultResponse(HttpStatus.OK, true)
    data class PartialBadRequest(val bulkUploadErrors: List<BulkUpdateResultsUseCase.BulkUpdateError>) :
        BulkUpdateResultResponse(HttpStatus.BAD_REQUEST, hasBody = true)

    object Unauthorized : BulkUpdateResultResponse(
        HttpStatus.UNAUTHORIZED, headers = mapOf(
            HttpHeaders.WWW_AUTHENTICATE to listOf(
                AUTHORIZATION_REALM
            )
        ).toHttpHeaders()
    )

    object Forbidden : BulkUpdateResultResponse(HttpStatus.FORBIDDEN)
}

private fun Map<String, List<String>>.toHttpHeaders() = HttpHeaders(LinkedMultiValueMap(this))
