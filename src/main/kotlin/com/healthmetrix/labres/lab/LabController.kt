package com.healthmetrix.labres.lab

import com.healthmetrix.labres.GlobalErrorHandler
import com.healthmetrix.labres.LABORATORY_API_TAG
import com.healthmetrix.labres.LABORATORY_BULK_API_TAG
import com.healthmetrix.labres.LabResApiResponse
import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.decodeBase64
import com.healthmetrix.labres.logger
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.persistence.OrderInformation
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
const val KEVB_CSV_VALUE = "application/kevb+csv"

@RestController
@ApiResponses(
    value = [
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
                    schema = Schema(implementation = GlobalErrorHandler.Error::class, hidden = false)
                )
            ]
        )
    ]
)
@SecurityRequirement(name = "LabCredential")
class LabController(
    private val updateResultUseCase: UpdateResultUseCase,
    private val bulkUpdateResultsUseCase: BulkUpdateResultsUseCase
) {

    @PutMapping(
        path = [LAB_API_BASE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE]
    )
    @Operation(
        summary = "Upload lab result. Supported formats: JSON, HL7 ORU"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Result uploaded successfully",
                content = [
                    Content(schema = Schema(implementation = UpdateStatusResponse.Success::class, hidden = true))
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Order number is invalid and could not be parsed",
                content = [
                    Content(
                        schema = Schema(
                            implementation = UpdateStatusResponse.InvalidRequest::class,
                            hidden = false
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "No order for order number (and issuer if provided) found",
                content = [
                    Content(schema = Schema(implementation = UpdateStatusResponse.OrderNotFound::class, hidden = true))
                ]
            )
        ]
    )
    @Tag(name = LABORATORY_API_TAG)
    fun jsonResult(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        labIdHeader: String,
        @RequestParam(required = false)
        issuerId: String?,
        @RequestBody
        result: JsonResult
    ): ResponseEntity<UpdateStatusResponse> {
        // TODO: test issuer to be whitelisted for basic auth user
        val labId = extractLabIdFrom(labIdHeader) ?: return UpdateStatusResponse.Unauthorized.asEntity()

        val orderNumber = try {
            OrderNumber.from(issuerId, result.orderNumber)
        } catch (ex: IllegalArgumentException) {
            return UpdateStatusResponse.InvalidRequest(ex.message ?: "Failed to parse orderNumber").asEntity()
        }

        val labResult = LabResult(
            orderNumber = orderNumber,
            labId = labId,
            result = result.result,
            testType = result.type
        )

        return updateResultUseCase(labResult).asUpdateStatusResponseEntity()
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
                    Content(schema = Schema(implementation = UpdateStatusResponse.Success::class, hidden = true))
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Order number is invalid and could not be parsed",
                content = [
                    Content(
                        schema = Schema(
                            implementation = UpdateStatusResponse.InvalidRequest::class,
                            hidden = false
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "No order for order number (and issuer if provided) found",
                content = [
                    Content(schema = Schema(implementation = UpdateStatusResponse.OrderNotFound::class, hidden = true))
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
        result: JsonResult
    ): ResponseEntity<UpdateStatusResponse> = jsonResult(labIdHeader, issuerId, result)

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
                    Content(schema = Schema(implementation = BulkUpdateStatusResponse.Success::class))
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Error messages per result that couldn't be successfully updated.",
                content = [
                    Content(schema = Schema(implementation = BulkUpdateStatusResponse.PartialBadRequest::class))
                ]
            )
        ]
    )
    @Tag(name = LABORATORY_BULK_API_TAG)
    fun bulkUploadJsonResults(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        labIdHeader: String,
        @RequestParam(required = false)
        issuerId: String?,
        @RequestBody
        request: BulkUpdateStatusRequest
    ): ResponseEntity<BulkUpdateStatusResponse> {
        // TODO: test issuer to be whitelisted for basic auth user
        val labId = extractLabIdFrom(labIdHeader) ?: return BulkUpdateStatusResponse.Unauthorized.asEntity()

        val errors = bulkUpdateResultsUseCase(request.results, labId, issuerId)

        val response = if (errors.any())
            BulkUpdateStatusResponse.PartialBadRequest(errors)
        else
            BulkUpdateStatusResponse.Success(request.results.size)

        return response.asEntity()
    }

    // TODO: collapse to one method and inject custom Spring HttpMessageConverter for application/kevb+csv
    @PutMapping(
        path = [LAB_API_BASE],
        consumes = [KEVB_CSV_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE]
    )
    @Operation(
        summary = "Upload lab result"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Result uploaded successfully",
                content = [
                    Content(schema = Schema(implementation = UpdateStatusResponse.Success::class, hidden = true))
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = [
                    Content(schema = Schema(implementation = UpdateStatusResponse.InfoUnreadable::class, hidden = true))
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "No order for order number found",
                content = [
                    Content(schema = Schema(implementation = UpdateStatusResponse.OrderNotFound::class, hidden = true))
                ]
            )
        ]
    )
    @Tag(name = LABORATORY_API_TAG)
    fun uploadCsvLabResult(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        labIdHeader: String,
        @RequestParam(required = false)
        issuerId: String?,
        @RequestBody
        @Schema(
            description = "CSV string with orderNumber, lab result and test type",
            example = "1234567890,NEGATIVE,94531-1",
            format = "string"
        )
        csv: String
    ): ResponseEntity<UpdateStatusResponse> {
        // TODO: remove after testing with KEVB
        logger.info("CSV message: received message with issuerId $issuerId and message:\n$csv")

        val labId = extractLabIdFrom(labIdHeader)
            ?: return UpdateStatusResponse.Unauthorized.asEntity()

        val csvParts = csv.trim().split(",")

        if (csvParts.size < 2)
            return UpdateStatusResponse.InvalidRequest(message = "CSV string contains less than 2 values").asEntity()

        val orderNumber = try {
            OrderNumber.from(issuerId, csvParts[0])
        } catch (ex: IllegalArgumentException) {
            return UpdateStatusResponse.InvalidRequest(message = ex.message ?: "Failed to parse orderNumber").asEntity()
        }

        val result = Result.from(csvParts[1])
            ?: return UpdateStatusResponse.InvalidRequest(message = "Failed to parse result").asEntity()

        val labResult = LabResult(
            orderNumber = orderNumber,
            labId = labId,
            result = result,
            testType = csvParts.getOrNull(2)
        )

        return updateResultUseCase(labResult).asUpdateStatusResponseEntity()
    }

    private fun extractLabIdFrom(labIdHeader: String): String? {
        val (prefix, encoded) = labIdHeader.split(" ").also {
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

data class JsonResult(
    @Schema(
        description = "The external order number",
        example = "1234567890"
    )
    val orderNumber: String,
    @Schema(
        description = "The kind of test used to generate the given result as a LOINC code.",
        example = "94500-6"
    )
    val type: String? = null,
    @Schema(description = "The test result")
    val result: Result
)

private const val WWW_AUTHENTICATE_VALUE = "Basic realm=\"labres:labresults:write\""

data class BulkUpdateStatusRequest(val results: List<JsonResult>)

sealed class UpdateStatusResponse(
    httpStatus: HttpStatus,
    hasBody: Boolean = false,
    headers: HttpHeaders = HttpHeaders.EMPTY
) :
    LabResApiResponse(httpStatus, hasBody, headers) {

    object Success : UpdateStatusResponse(HttpStatus.OK)
    object OrderNotFound : UpdateStatusResponse(HttpStatus.NOT_FOUND)
    data class InvalidRequest(val message: String) : UpdateStatusResponse(HttpStatus.BAD_REQUEST, hasBody = true)
    object Unauthorized : UpdateStatusResponse(
        HttpStatus.UNAUTHORIZED, headers = mapOf(
            HttpHeaders.WWW_AUTHENTICATE to listOf(
                WWW_AUTHENTICATE_VALUE
            )
        ).toHttpHeaders()
    )

    object Forbidden : UpdateStatusResponse(HttpStatus.FORBIDDEN)

    object InfoUnreadable : UpdateStatusResponse(HttpStatus.BAD_REQUEST, true) {
        val message = "Unable to read status"
    }
}

sealed class BulkUpdateStatusResponse(
    httpStatus: HttpStatus,
    hasBody: Boolean = false,
    headers: HttpHeaders = HttpHeaders.EMPTY
) :
    LabResApiResponse(httpStatus, hasBody, headers) {

    data class Success(val processedRows: Int) : BulkUpdateStatusResponse(HttpStatus.OK, true)
    data class PartialBadRequest(val bulkUploadErrors: List<BulkUploadError>) :
        BulkUpdateStatusResponse(HttpStatus.BAD_REQUEST, hasBody = true)

    object Unauthorized : BulkUpdateStatusResponse(
        HttpStatus.UNAUTHORIZED, headers = mapOf(
            HttpHeaders.WWW_AUTHENTICATE to listOf(
                WWW_AUTHENTICATE_VALUE
            )
        ).toHttpHeaders()
    )
}

data class BulkUploadError(val message: String)

private fun Map<String, List<String>>.toHttpHeaders() = HttpHeaders(LinkedMultiValueMap(this))

private fun OrderInformation?.asUpdateStatusResponseEntity() = (this?.let { UpdateStatusResponse.Success }
    ?: UpdateStatusResponse.OrderNotFound).asEntity()
