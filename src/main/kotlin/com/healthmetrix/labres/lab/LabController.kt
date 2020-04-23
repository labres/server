package com.healthmetrix.labres.lab

import com.healthmetrix.labres.LABORATORY_API_TAG
import com.healthmetrix.labres.LabResApiResponse
import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Status
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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

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
    private val extractLabIdUseCase: ExtractLabIdUseCase,
    private val extractObxResultUseCase: ExtractObxResultUseCase,
    private val extractLdtResultUseCase: ExtractLdtResultUseCase,
    private val updateResultUseCase: UpdateResultUseCase
) {

    @PutMapping(
        path = ["/v1/results/json"],
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
                responseCode = "404",
                description = "No order for order number found",
                content = [
                    Content(schema = Schema(implementation = UpdateStatusResponse.OrderNotFound::class, hidden = true))
                ]
            )
        ]
    )
    fun jsonResult(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        labIdHeader: String,
        @RequestBody
        result: JsonResult
    ): ResponseEntity<UpdateStatusResponse> {
        val orderNumber = OrderNumber.External.from(result.orderNumber)
            ?: return UpdateStatusResponse.OrderNotFound.asEntity()

        val labId = extractLabIdUseCase(labIdHeader) ?: return UpdateStatusResponse.OrderNotFound.asEntity()

        val labResult = LabResult(
            orderNumber = orderNumber,
            labId = labId,
            result = result.result,
            testType = result.type
        )

        return updateResultUseCase(labResult).asEntity()
    }

    @PutMapping(
        path = ["/v1/results/obx"],
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
        @RequestBody
        @Schema(
            description = "An OBX Segment of an HL7 ORU R1 message (see https://wiki.hl7.de/index.php?title=Segment_OBX for details).",
            example = "OBX|3|ST|21300^2019-nCoronav.-RNA Sonst (PCR)|0061749799|Positiv|||N|||S|||20200406101220|Extern|||||||||Extern",
            format = "string"
        )
        obxMessage: String
    ): ResponseEntity<UpdateStatusResponse> {
        val labId = extractLabIdUseCase(labIdHeader)
            ?: return UpdateStatusResponse.OrderNotFound.asEntity()

        val labResult = extractObxResultUseCase(obxMessage, labId)
            ?: return UpdateStatusResponse.InfoUnreadable.asEntity()

        return updateResultUseCase(labResult).asEntity()
    }

    @PutMapping(
        path = ["/v1/results/ldt"],
        consumes = [MediaType.TEXT_PLAIN_VALUE]
    )
    fun ldtResult(
        @RequestHeader(HttpHeaders.AUTHORIZATION)
        labIdHeader: String,
        @RequestBody
        ldtMessage: String
    ): ResponseEntity<UpdateStatusResponse> {
        val labId = extractLabIdUseCase(labIdHeader)
            ?: return UpdateStatusResponse.OrderNotFound.asEntity()

        val labResult = extractLdtResultUseCase(ldtMessage, labId)
            ?: return UpdateStatusResponse.InfoUnreadable.asEntity()

        return updateResultUseCase(labResult).asEntity()
    }
}

data class JsonResult(
    @Schema(description = "The external order number")
    val orderNumber: String,
    val type: String? = null,
    @Schema(description = "The test result")
    val result: Result
)

data class LabResult(
    val orderNumber: OrderNumber,
    val labId: String,
    val result: Result,
    val testType: String?
)

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

sealed class UpdateStatusResponse(httpStatus: HttpStatus, hasBody: Boolean = false) :
    LabResApiResponse(httpStatus, hasBody) {
    object Success : UpdateStatusResponse(HttpStatus.OK)
    object OrderNotFound : UpdateStatusResponse(HttpStatus.NOT_FOUND)
    object InfoUnreadable : UpdateStatusResponse(HttpStatus.BAD_REQUEST, true) {
        val message = "Unable to read status"
    }
}
