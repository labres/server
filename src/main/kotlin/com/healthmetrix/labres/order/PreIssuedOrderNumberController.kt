package com.healthmetrix.labres.order

import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.healthmetrix.labres.GlobalErrorHandler
import com.healthmetrix.labres.LabResApiResponse
import com.healthmetrix.labres.PRE_ISSUED_ORDER_NUMBER_API_TAG
import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.logger
import com.healthmetrix.labres.unify
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

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
    private val queryStatusUseCase: QueryStatusUseCase,
    private val metrics: OrderMetrics
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
        @PathVariable(value = "issuerId") rawIssuerId: String,
        @RequestBody rawRequest: RegisterOrderRequest
    ): ResponseEntity<RegisterOrderResponse> {
        val requestId = UUID.randomUUID()

        val (issuerId, request) = transform(rawIssuerId, rawRequest, requestId)

        logger.debug(
            "[{}] for issuerId {} with request $request",
            kv("method", "registerOrder"),
            kv("issuerId", issuerId),
            kv("orderNumber", request.orderNumber),
            kv("sample", request.sample),
            kv("testSiteId", request.testSiteId),
            kv("requestId", requestId)
        )

        return registerOrderUseCase(
            orderNumber = OrderNumber.from(issuerId, request.orderNumber),
            testSiteId = request.testSiteId,
            sample = request.sample,
            notificationUrl = request.notificationUrl
        ).onFailure { msg ->
            logger.warn(
                "[{}] Order already exists: $msg",
                kv("method", "registerOrder"),
                kv("issuerId", issuerId),
                kv("orderNumber", request.orderNumber),
                kv("sample", request.sample),
                kv("requestId", requestId)
            )
            metrics.countConflictOnRegisteringOrders(issuerId)
        }.mapError {
            RegisterOrderResponse.Conflict
        }.onSuccess {
            logger.debug(
                "[{}] Order successfully registered",
                kv("method", "registerOrder"),
                kv("issuerId", issuerId),
                kv("orderNumber", request.orderNumber),
                kv("sample", request.sample),
                kv("requestId", requestId)
            )
            metrics.countRegisteredOrders(issuerId)
        }.map { order ->
            RegisterOrderResponse.Created(order.id, order.orderNumber.number)
        }.unify().asEntity()
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
                content = [
                    Content(
                        schema = Schema(
                            type = "object",
                            implementation = StatusResponse.NotFound::class,
                            hidden = true
                        )
                    )
                ]
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
        val requestId = UUID.randomUUID()
        logger.debug(
            "[{}] for issuerId {} and orderId {}",
            kv("method", "getOrderNumber"),
            kv("issuerId", issuerId),
            kv("orderId", orderId),
            kv("requestId", requestId)
        )

        val id = try {
            UUID.fromString(orderId)
        } catch (ex: IllegalArgumentException) {
            val message = "Failed to parse orderId $orderId"
            logger.info(
                "[{}]: $message",
                kv("method", "getOrderNumber"),
                kv("issuerId", issuerId),
                kv("orderId", orderId),
                kv("requestId", requestId),
                ex
            )
            metrics.countErrorOnParsingOrderNumbersOnGet(issuerId)
            return StatusResponse.BadRequest(message).asEntity()
        }

        val result = queryStatusUseCase(id, issuerId)

        return if (result != null) {
            logger.debug(
                "[{}]: Found $result",
                kv("method", "getOrderNumber"),
                kv("issuerId", issuerId),
                kv("orderId", orderId),
                kv("requestId", requestId)
            )
            StatusResponse.Found(result).asEntity()
        } else {
            logger.info(
                "[{}]: Not found",
                kv("method", "getOrderNumber"),
                kv("issuerId", issuerId),
                kv("orderId", orderId),
                kv("requestId", requestId)
            )
            metrics.countOrderNotFoundOnGet(issuerId)
            StatusResponse.NotFound.asEntity()
        }
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
                    )
                ]
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
        updateOrderRequestBody: UpdateOrderRequestBody
    ): ResponseEntity<UpdateOrderResponse> {
        val requestId = UUID.randomUUID()
        logger.debug(
            "[{}]: Update order for issuerId {} and orderId {}: $updateOrderRequestBody",
            kv("method", "updateOrder"),
            kv("issuerId", issuerId),
            kv("orderId", orderId),
            kv("requestId", requestId)
        )

        val id = try {
            UUID.fromString(orderId)
        } catch (ex: IllegalArgumentException) {
            val message = "Failed to parse orderId $orderId"
            logger.info(
                "[{}]: $message",
                kv("method", "updateOrder"),
                kv("issuerId", issuerId),
                kv("orderId", orderId),
                kv("requestId", requestId),
                ex
            )
            metrics.countErrorOnParsingOrderNumbersOnUpdate(issuerId)
            return UpdateOrderResponse.BadRequest(message).asEntity()
        }

        val result = updateOrderUseCase(id, issuerId, updateOrderRequestBody.notificationUrl)
        logger.debug(
            "[{}]: $result",
            kv("method", "updateOrder"),
            kv("issuerId", issuerId),
            kv("orderId", orderId),
            kv("requestId", requestId)
        )

        return when (result) {
            UpdateOrderUseCase.Result.SUCCESS -> UpdateOrderResponse.Updated
            UpdateOrderUseCase.Result.NOT_FOUND -> UpdateOrderResponse.NotFound.also {
                metrics.countOrderNotFoundOnUpdate(
                    issuerId
                )
            }
        }.asEntity()
    }

    data class RegisterOrderRequest(
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
        val notificationUrl: String?,
        @Schema(
            description = "The sample type that is being used for the lab test.",
            nullable = true,
            required = false,
            defaultValue = "SALIVA",
            example = "BLOOD"
        )
        val sample: Sample = Sample.SALIVA
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

    // TODO move out in classes being invoked depending on issuerId
    fun transform(
        rawIssuerId: String,
        rawRequest: RegisterOrderRequest,
        requestId: UUID
    ): Pair<String, RegisterOrderRequest> {
        // BEGIN IOS ISSUERID QUICKFIX
        val iOsReplacedIssuerIdWithTestSite = rawIssuerId == "hpi" || rawIssuerId == "wmt"

        if (iOsReplacedIssuerIdWithTestSite) {
            logger.warn(
                "[{}] IOS ISSUERID BUG: incoming issuerId $rawIssuerId, incoming testSiteId ${rawRequest.testSiteId}",
                kv("method", "registerOrder"),
                kv("requestId", requestId)
            )
        }

        val request = if (iOsReplacedIssuerIdWithTestSite) {
            rawRequest.copy(
                testSiteId = rawIssuerId
            )
        } else {
            rawRequest
        }

        val issuerId = if (iOsReplacedIssuerIdWithTestSite) {
            "mvz"
        } else {
            rawIssuerId
        }
        // END IOS ISSUERID QUICKFIX

        // BEGIN TRUNCATE KEVB ORDER NUMBERS
        if (issuerId == "kevb" && request.orderNumber.length > 8) {
            logger.info(
                "[{}] Truncating analyt prefix for order {}",
                kv("method", "registerOrder"),
                kv("orderNumber", request.orderNumber),
                kv("issuerId", issuerId),
                kv("requestId", requestId),
                kv("issuer", "kevb")
            )
            return issuerId to request.copy(orderNumber = request.orderNumber.substring(0, 8))
        }
        // END TRUNCATE KEVB ORDER NUMBERS

        return issuerId to request
    }
}
