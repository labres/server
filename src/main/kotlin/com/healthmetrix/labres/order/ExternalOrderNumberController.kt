package com.healthmetrix.labres.order

import com.healthmetrix.labres.EXTERNAL_ORDER_NUMBER_API_TAG
import com.healthmetrix.labres.LabResApiResponse
import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.logger
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            headers = [Header(name = "WWW-Authenticate", schema = Schema(type = "string"))],
            content = [Content()]
        )
    ]
)
@SecurityRequirement(name = "OrdersApiToken")
@Tag(name = EXTERNAL_ORDER_NUMBER_API_TAG)
class ExternalOrderNumberController(
    private val issueExternalOrderNumber: IssueExternalOrderNumberUseCase,
    private val updateOrderUseCase: UpdateOrderUseCase,
    private val queryStatusUseCase: QueryStatusUseCase
) {
    @PostMapping(
        path = ["/v1/orders"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        summary = "Issues a new globally unique External Order Number (EON) and registers a lab order for it. The number of issuing EONs is limited to 3 per subject.",
        description = "Should only be invoked for verified users (logged into account or verified email address)"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "External Order Number Created",
                content = [
                    Content(
                        schema = Schema(
                            type = "object",
                            implementation = IssueExternalOrderNumberResponse.Created::class
                        )
                    )
                ]
            )
        ]
    )
    fun issueExternalOrderNumber(
        @RequestBody(required = false)
        requestBody: IssueExternalOrderNumberRequestBody?
    ): ResponseEntity<IssueExternalOrderNumberResponse> {
        val (id, orderNumber) = issueExternalOrderNumber(requestBody?.notificationUrl)
        return IssueExternalOrderNumberResponse.Created(id, orderNumber.number).asEntity()
    }

    @GetMapping(path = ["/v1/orders/{orderId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Returns the current status of a given lab order",
        description = "Should only be invoked for verified users (logged into account or verified email address)"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "returns current status of the lab order",
                content = [
                    Content(schema = Schema(type = "object", implementation = StatusResponse.Found::class))
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Order id could not be parsed as valid UUID",
                content = [Content(
                    schema = Schema(
                        type = "object",
                        implementation = StatusResponse.BadRequest::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "No order for the given id found",
                content = [Content(
                    schema = Schema(
                        type = "object",
                        implementation = StatusResponse.NotFound::class,
                        hidden = true
                    )
                )]
            )
        ]
    )
    fun queryStatus(
        @Parameter(
            description = "UUID of an order that has been sent to a lab",
            required = true,
            schema = Schema(type = "string", format = "uuid", description = "A Version 4 UUID")
        )
        @PathVariable orderId: String
    ): ResponseEntity<StatusResponse> {
        val id = try {
            UUID.fromString(orderId)
        } catch (ex: IllegalArgumentException) {
            val message = "Failed to parse orderId $orderId"
            logger.info(message, ex)
            return StatusResponse.BadRequest(message).asEntity()
        }

        return (queryStatusUseCase(id, null)
            ?.let(StatusResponse::Found)
            ?: StatusResponse.NotFound)
            .asEntity()
    }

    @PutMapping(
        path = ["/v1/orders/{orderId}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        summary = "Updates an order with the notification url",
        description = "Should only be invoked for verified users (logged into account or verified email address)"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successful update",
                content = [
                    Content(
                        schema = Schema(
                            type = "object",
                            implementation = UpdateOrderResponse.Updated::class,
                            hidden = true
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "OrderId could not be parsed as a UUID",
                content = [
                    Content(
                        schema = Schema(
                            type = "object",
                            implementation = UpdateOrderResponse.BadRequest::class
                        )
                    )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "no order for the given id found",
                content = [
                    Content(
                        schema = Schema(
                            type = "object",
                            implementation = UpdateOrderResponse.NotFound::class,
                            hidden = true
                        )
                    )]
            )
        ]
    )
    fun updateOrder(
        @Parameter(
            description = "UUID of an order that has been sent to a lab",
            required = true,
            schema = Schema(type = "string", format = "uuid", description = "A Version 4 UUID")
        )
        @PathVariable
        orderId: String,
        @RequestBody
        updateOrderRequestBody: UpdateOrderRequestBody
    ): ResponseEntity<UpdateOrderResponse> {
        val id = try {
            UUID.fromString(orderId)
        } catch (ex: IllegalArgumentException) {
            val message = "Failed to parse orderId $orderId"
            logger.info(message, ex)
            return UpdateOrderResponse.BadRequest(message).asEntity()
        }

        return when (updateOrderUseCase(id, null, updateOrderRequestBody.notificationUrl)) {
            UpdateOrderUseCase.Result.SUCCESS -> UpdateOrderResponse.Updated
            UpdateOrderUseCase.Result.NOT_FOUND -> UpdateOrderResponse.NotFound
        }.asEntity()
    }

    data class IssueExternalOrderNumberRequestBody(
        @Schema(
            type = "string",
            description = "Notification URL sent from the client that can be used later to notify them that lab results have been uploaded. Must be a valid, complete URL including protocol for an existing HTTPS endpoint supporting POST requests.",
            example = "https://client.labres.de/notification",
            required = true
        )
        val notificationUrl: String
    )

    sealed class IssueExternalOrderNumberResponse(httpStatus: HttpStatus, hasBody: Boolean = true) :
        LabResApiResponse(httpStatus, hasBody) {

        data class Created(
            @Schema(
                description = "A unique internal identifier for the order",
                example = "65e524cb-7494-4073-ad16-495fed0d79e4"
            )
            val id: UUID,
            @Schema(
                description = "numeric 10-digit-long external order number",
                example = "1234567890"
            )
            val orderNumber: String
        ) : IssueExternalOrderNumberResponse(HttpStatus.CREATED)
    }

    data class UpdateOrderRequestBody(
        @Schema(
            type = "string",
            description = "Notification URL sent from the client that can be used later to notify them that lab results have been uploaded. Must be a valid, complete URL including protocol for an existing HTTPS endpoint supporting POST requests.",
            required = true
        )
        val notificationUrl: String
    )

    sealed class UpdateOrderResponse(httpStatus: HttpStatus, hasBody: Boolean = false) :
        LabResApiResponse(httpStatus, hasBody) {
        object Updated : UpdateOrderResponse(HttpStatus.OK)
        object NotFound : UpdateOrderResponse(HttpStatus.NOT_FOUND)
        data class BadRequest(val message: String) : UpdateOrderResponse(HttpStatus.BAD_REQUEST)
    }
}
