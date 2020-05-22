package com.healthmetrix.labres.order

import com.healthmetrix.labres.GlobalErrorHandler
import com.healthmetrix.labres.LabResApiResponse
import com.healthmetrix.labres.PRE_ISSUED_ORDER_NUMBER_API_TAG
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
            description = "Unauthorized",
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
@SecurityRequirement(name = "OrdersApiToken")
@Tag(name = PRE_ISSUED_ORDER_NUMBER_API_TAG)
class PreIssuedOrderNumberController(
    private val registerOrderUseCase: RegisterOrderUseCase,
    private val updateOrderUseCase: UpdateOrderUseCase,
    private val queryStatusUseCase: QueryStatusUseCase
) {
    @PostMapping(
        path = ["/v1/issuers/{issuerId}/orders"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        summary = "Registers a laboratory order specified by an issuer id and a given out laboratory order number",
        description = "Should only be invoked for verified users (logged into account or verified email address)"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Pre issued laboratory order has been registered successfully",
                content = [
                    Content(schema = Schema(type = "object", implementation = RegisterOrderResponse.Created::class))
                ]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Combination of issuer and order number already exists",
                content = [
                    Content(schema = Schema(type = "object", implementation = RegisterOrderResponse.Conflict::class))
                ]
            )
        ]
    )
    fun registerOrder(
        @Parameter(
            description = "Identifier for the issuer giving out the order number. Has to be whitelisted for a JWT issuer.",
            required = true,
            schema = Schema(
                type = "string",
                description = "A short descriptive issuer name having at most 16 characters"
            )
        )
        @PathVariable issuerId: String,
        @RequestBody requestBody: RegisterOrderRequestBody
    ): ResponseEntity<RegisterOrderResponse> {
        val order = registerOrderUseCase(
            orderNumber = OrderNumber.from(issuerId, requestBody.orderNumber),
            testSiteId = requestBody.testSiteId,
            notificationUrl = requestBody.notificationUrl
        ) ?: return RegisterOrderResponse.Conflict.asEntity()

        return RegisterOrderResponse.Created(order.id, order.orderNumber.number).asEntity()
    }

    @GetMapping(path = ["/v1/issuers/{issuerId}/orders/{orderId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
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
                responseCode = "404",
                description = "No order for the given combination of issuerId and orderId found",
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
    fun getOrderNumber(
        @Parameter(
            description = "Identifier for the issuer giving out the order number. Has to be whitelisted for a JWT issuer.",
            required = true,
            schema = Schema(
                type = "string",
                description = "A short descriptive issuer name having at most 16 characters"
            )
        )
        @PathVariable issuerId: String,
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

        return (queryStatusUseCase(id, issuerId)
            ?.let(StatusResponse::Found)
            ?: StatusResponse.NotFound)
            .asEntity()
    }

    @PutMapping(
        path = ["/v1/issuers/{issuerId}/orders/{orderId}"],
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
            description = "Identifier for the issuer given out the order number. Has to be whitelisted for a JWT issuer.",
            required = true,
            schema = Schema(
                type = "string",
                description = "A short descriptive issuer name having at most 16 characters"
            )
        )
        @PathVariable
        issuerId: String,
        @Parameter(
            description = "UUID of an order that has been sent to a lab",
            required = true,
            schema = Schema(type = "string", format = "uuid", description = "A Version 4 UUID")
        )
        @PathVariable
        orderId: String,
        @RequestBody
        updateOrderRequestBody: ExternalOrderNumberController.UpdateOrderRequestBody
    ): ResponseEntity<UpdateOrderResponse> {
        val id = try {
            UUID.fromString(orderId)
        } catch (ex: IllegalArgumentException) {
            val message = "Failed to parse orderId $orderId"
            logger.info(message, ex)
            return UpdateOrderResponse.BadRequest(message).asEntity()
        }

        return when (updateOrderUseCase(id, issuerId, updateOrderRequestBody.notificationUrl)) {
            UpdateOrderUseCase.Result.SUCCESS -> UpdateOrderResponse.Updated
            UpdateOrderUseCase.Result.NOT_FOUND -> UpdateOrderResponse.NotFound
        }.asEntity()
    }

    data class RegisterOrderRequestBody(
        @Schema(
            type = "string",
            description = "Order number that has been issued by an issuer to identify a laboratory order. Must be unique for the given issuer id.",
            required = true
        )
        val orderNumber: String,
        @Schema(
            type = "string",
            description = "Optional identifier to specify the test site a test was being conducted at",
            required = false
        )
        val testSiteId: String?,
        @Schema(
            type = "string",
            description = "Notification URL sent from the client that can be used later to notify them that lab results have been uploaded. Must be a valid, complete URL including protocol for an existing HTTPS endpoint supporting POST requests.",
            example = "https://client.labres.de/notification",
            required = false
        )
        val notificationUrl: String?
    )

    sealed class RegisterOrderResponse(httpStatus: HttpStatus, hasBody: Boolean = true) :
        LabResApiResponse(httpStatus, hasBody) {

        data class Created(
            @Schema(
                type = "string",
                description = "A unique internal identifier for the order",
                example = "65e524cb-7494-4073-ad16-495fed0d79e4"
            )
            val id: UUID,
            @Schema(
                type = "string",
                description = "numeric 10-digit-long external order number",
                example = "1234567890"
            )
            val orderNumber: String
        ) : RegisterOrderResponse(HttpStatus.CREATED)

        object Conflict : RegisterOrderResponse(HttpStatus.CONFLICT)
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
