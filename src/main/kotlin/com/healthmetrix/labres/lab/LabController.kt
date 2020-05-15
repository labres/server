package com.healthmetrix.labres.lab

import com.healthmetrix.labres.LABORATORY_API_TAG
import com.healthmetrix.labres.LabResApiResponse
import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.decodeBase64
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

@RestController
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "401",
            description = "API key invalid or missing",
            headers = [Header(name = "WWW-Authenticate", schema = Schema(type = "string"))],
            content = [Content()]
        )
    ]
)
@SecurityRequirement(name = "LabCredential")
@Tag(name = LABORATORY_API_TAG)
class LabController(
    private val extractObxResultUseCase: ExtractObxResultUseCase,
    private val extractLdtResultUseCase: ExtractLdtResultUseCase,
    private val updateResultUseCase: UpdateResultUseCase
) {

    @PutMapping(
        path = [LAB_API_BASE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
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
                    Content(schema = Schema(implementation = UpdateStatusResponse.OrderNumberInvalid::class, hidden = false))
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

        val labResult = try {
            parseJsonResult(result, labId, issuerId)
        } catch (ex: IllegalArgumentException) {
            return UpdateStatusResponse.OrderNumberInvalid(ex.message ?: "Failed to parse orderNumber").asEntity()
        }

        return updateResultUseCase(labResult).asUpdateStatusResponseEntity()
    }

    @PutMapping(
        path = ["$LAB_API_BASE/json"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
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
                    Content(schema = Schema(implementation = UpdateStatusResponse.OrderNumberInvalid::class, hidden = false))
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
        produces = [MediaType.APPLICATION_JSON_VALUE]
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

        val errors = mutableListOf<BulkUploadError>()

        request.results.forEach { result ->
            val labResult = try {
                parseJsonResult(result, labId, issuerId)
            } catch (ex: IllegalArgumentException) {
                errors.add(BulkUploadError(ex.message ?: "Failed to parse orderNumber"))
                null
            }

            if (labResult != null)
                updateResultUseCase(labResult)
                    ?: errors.add(BulkUploadError("Order for orderNumber ${result.orderNumber} not found"))
        }

        val response = if (errors.any())
            BulkUpdateStatusResponse.PartialBadRequest(errors)
        else
            BulkUpdateStatusResponse.Success(request.results.size)

        return response.asEntity()
    }

    @PutMapping(
        path = ["$LAB_API_BASE/obx"],
        consumes = [MediaType.TEXT_PLAIN_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        summary = "Upload lab result via OBX text message"
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
                description = "Invalid OBX Message. See https://wiki.hl7.de/index.php?title=Segment_OBX for a description of acceptable fields",
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
    fun obxResult(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        labIdHeader: String,
        @RequestParam(required = false)
        issuerId: String?,
        @RequestBody
        @Schema(
            description = "An OBX Segment of an HL7 ORU R1 message (see https://wiki.hl7.de/index.php?title=Segment_OBX for details).",
            example = "OBX|3|ST|21300^2019-nCoronav.-RNA Sonst (PCR)|0061749799|Positiv|||N|||S|||20200406101220|Extern|||||||||Extern",
            format = "string"
        )
        obxMessage: String
    ): ResponseEntity<UpdateStatusResponse> {
        val labId = extractLabIdFrom(labIdHeader) ?: return UpdateStatusResponse.OrderNotFound.asEntity()

        val labResult = extractObxResultUseCase(obxMessage, labId, issuerId)
            ?: return UpdateStatusResponse.InfoUnreadable.asEntity()

        return updateResultUseCase(labResult).asUpdateStatusResponseEntity()
    }

    @PutMapping(
        path = ["$LAB_API_BASE/ldt"],
        consumes = [MediaType.TEXT_PLAIN_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        summary = "Upload lab result via LDT Document"
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
                description = "Invalid LDT Document",
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
    fun ldtResult(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        labIdHeader: String,
        @RequestParam(name = "issuer", required = false)
        issuerId: String?,
        @RequestBody
        @Schema(description = "An LDT Document")
        ldtMessage: String
    ): ResponseEntity<UpdateStatusResponse> {
        val labId = extractLabIdFrom(labIdHeader) ?: return UpdateStatusResponse.OrderNotFound.asEntity()

        val labResult = extractLdtResultUseCase(ldtMessage, labId)
            ?: return UpdateStatusResponse.InfoUnreadable.asEntity()

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

    private fun parseJsonResult(result: JsonResult, labId: String, issuerId: String?): LabResult {
        val orderNumber = OrderNumber.from(issuerId, result.orderNumber)

        return LabResult(
            orderNumber = orderNumber,
            labId = labId,
            result = result.result,
            testType = result.type
        )
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
    data class OrderNumberInvalid(val message: String) : UpdateStatusResponse(HttpStatus.BAD_REQUEST, hasBody = true)
    object Unauthorized : UpdateStatusResponse(
        HttpStatus.UNAUTHORIZED, headers = mapOf(
            HttpHeaders.WWW_AUTHENTICATE to listOf(
                WWW_AUTHENTICATE_VALUE
            )
        ).toHttpHeaders()
    )

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
