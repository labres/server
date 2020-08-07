package com.healthmetrix.labres.order

import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.healthmetrix.labres.EXTERNAL_ORDER_NUMBER_API_TAG
import com.healthmetrix.labres.GlobalErrorHandler
import com.healthmetrix.labres.LabResApiResponse
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
import net.logstash.logback.argument.StructuredArguments
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "400",
            description = "Bad request",
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
@Tag(name = EXTERNAL_ORDER_NUMBER_API_TAG)
class ExternalOrderNumberController(
    private val issueExternalOrderNumber: IssueExternalOrderNumberUseCase,
    private val updateOrderUseCase: UpdateOrderUseCase,
    private val findOrderByIdUseCase: FindOrderByIdUseCase,
    private val findOrderByOrderNumberUseCase: FindOrderByOrderNumberUseCase,
    private val metrics: OrderMetrics
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
        request: IssueExternalOrderNumberRequestBody? // nullable for backwards compatibility
    ): ResponseEntity<IssueExternalOrderNumberResponse> {
        val requestId = UUID.randomUUID()

        logger.debug(
            "[{}] for issuerId labres with request $request",
            StructuredArguments.kv("method", "registerOrder"),
            StructuredArguments.kv("issuerId", EON_ISSUER_ID),
            StructuredArguments.kv("sample", request?.sample ?: Sample.SALIVA),
            StructuredArguments.kv("requestId", requestId)
        )

        return issueExternalOrderNumber(
            notificationUrl = request?.notificationUrl,
            sample = request?.sample ?: Sample.SALIVA,
            verificationSecret = request?.verificationSecret
        ).onFailure { msg ->
            logger.warn(
                "[{}] Order already exists: $msg",
                StructuredArguments.kv("method", "registerOrder"),
                StructuredArguments.kv("issuerId", EON_ISSUER_ID),
                StructuredArguments.kv("sample", request?.sample ?: Sample.SALIVA),
                StructuredArguments.kv("requestId", requestId)
            )
            metrics.countConflictOnRegisteringOrders(EON_ISSUER_ID, null)
        }.mapError {
            IssueExternalOrderNumberResponse.Conflict
        }.onSuccess {
            logger.debug(
                "[{}] Order successfully registered",
                StructuredArguments.kv("method", "registerOrder"),
                StructuredArguments.kv("issuerId", EON_ISSUER_ID),
                StructuredArguments.kv("orderNumber", it.orderNumber.number),
                StructuredArguments.kv("sample", request?.sample),
                StructuredArguments.kv("requestId", requestId)
            )
            metrics.countRegisteredOrders(EON_ISSUER_ID, null)
        }.map { order ->
            IssueExternalOrderNumberResponse.Created(order.id, order.orderNumber.number)
        }.unify().asEntity()
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
                responseCode = "404",
                description = "No order for the given id found",
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
    fun queryStatus(
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
            StructuredArguments.kv("method", "getOrderNumber"),
            StructuredArguments.kv("issuerId", EON_ISSUER_ID),
            StructuredArguments.kv("orderId", orderId),
            StructuredArguments.kv("requestId", requestId)
        )

        val id = try {
            UUID.fromString(orderId)
        } catch (ex: IllegalArgumentException) {
            val message = "Failed to parse orderId $orderId"
            logger.info(
                "[{}]: $message",
                StructuredArguments.kv("method", "getOrderNumber"),
                StructuredArguments.kv("issuerId", EON_ISSUER_ID),
                StructuredArguments.kv("orderId", orderId),
                StructuredArguments.kv("requestId", requestId),
                ex
            )
            metrics.countErrorOnParsingOrderIdOnGet(EON_ISSUER_ID)
            return StatusResponse.BadRequest(message).asEntity()
        }

        val result = findOrderByIdUseCase(id, null)

        return if (result != null) {
            logger.debug(
                "[{}]: Found $result",
                StructuredArguments.kv("method", "getOrderNumber"),
                StructuredArguments.kv("issuerId", EON_ISSUER_ID),
                StructuredArguments.kv("orderId", orderId),
                StructuredArguments.kv("requestId", requestId)
            )
            StatusResponse.Found(result.status, result.sampledAt).asEntity()
        } else {
            logger.info(
                "[{}]: Not found",
                StructuredArguments.kv("method", "getOrderNumber"),
                StructuredArguments.kv("issuerId", EON_ISSUER_ID),
                StructuredArguments.kv("orderId", orderId),
                StructuredArguments.kv("requestId", requestId)
            )
            metrics.countOrderNotFoundOnGet(EON_ISSUER_ID)
            StatusResponse.NotFound.asEntity()
        }
    }

    @GetMapping(path = ["/v1/orders"], produces = [MediaType.APPLICATION_JSON_VALUE])
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
                description = "No order for the given eon found",
                content = [
                    Content(
                        schema = Schema(
                            type = "object",
                            implementation = StatusResponse.NotFound::class,
                            hidden = true
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Verification secret was wrong for the given EON",
                content = [
                    Content(
                        schema = Schema(
                            type = "object",
                            implementation = StatusResponse.Forbidden::class,
                            hidden = true
                        )
                    )
                ]
            )
        ]
    )
    fun queryStatusByEon(
        @Parameter(
            description = "OrderNumber",
            required = true,
            schema = Schema(type = "string", format = "[0123456789]{10}", description = "OrderNumber in format [0123456789]{10}")
        )
        @RequestParam(required = true, name = "orderNumber")
        orderNumberAsString: String,

        @Parameter(
            description = "Verification secret that has been registered on issuing the order number",
            required = true,
            schema = Schema(
                nullable = false,
                required = true,
                example = "3ASsdfSA*SFDnj!f"
            )
        )
        @RequestParam(required = true)
        verificationSecret: String,

        @Parameter(
            description = "The sample type that is being used for the lab test.",
            required = false,
            schema = Schema(
                type = "string",
                defaultValue = "SALIVA",
                allowableValues = ["SALIVA", "BLOOD"],
                example = "SALIVA"
            )
        )
        @RequestParam(required = false, defaultValue = "SALIVA")
        sample: Sample = Sample.SALIVA
    ): ResponseEntity<StatusResponse> {
        val requestId = UUID.randomUUID()

        logger.debug(
            "[{}] for issuerId {}, orderNumber {} and sample {}",
            StructuredArguments.kv("method", "getResultByEon"),
            StructuredArguments.kv("issuerId", EON_ISSUER_ID),
            StructuredArguments.kv("orderNumber", orderNumberAsString),
            StructuredArguments.kv("sample", sample.toString()),
            StructuredArguments.kv("requestId", requestId)
        )

        val orderNumber = try {
            OrderNumber.from(null, orderNumberAsString)
        } catch (ex: IllegalArgumentException) {
            val message = "Failed to parse orderNumber $orderNumberAsString"
            logger.info(
                "[{}]: $message",
                StructuredArguments.kv("method", "getResultByEon"),
                StructuredArguments.kv("issuerId", EON_ISSUER_ID),
                StructuredArguments.kv("orderNumber", orderNumberAsString),
                StructuredArguments.kv("sample", sample),
                StructuredArguments.kv("requestId", requestId),
                ex
            )
            metrics.countErrorOnParsingOrderIdOnGet(EON_ISSUER_ID)
            return StatusResponse.BadRequest(message).asEntity()
        }

        return findOrderByOrderNumberUseCase(orderNumber, sample, verificationSecret)
            .onFailure { err ->
                when (err) {
                    FindOrderByOrderNumberUseCase.FindOrderError.NOT_FOUND -> {
                        logger.info(
                            "[{}]: Not found",
                            StructuredArguments.kv("method", "getResultByEon"),
                            StructuredArguments.kv("issuerId", EON_ISSUER_ID),
                            StructuredArguments.kv("orderNumber", orderNumberAsString),
                            StructuredArguments.kv("sample", sample),
                            StructuredArguments.kv("requestId", requestId)
                        )
                        metrics.countOrderNotFoundOnGetByEon()
                    }
                    FindOrderByOrderNumberUseCase.FindOrderError.FORBIDDEN -> {
                        logger.info(
                            "[{}]: Forbidden",
                            StructuredArguments.kv("method", "getResultByEon"),
                            StructuredArguments.kv("issuerId", EON_ISSUER_ID),
                            StructuredArguments.kv("orderNumber", orderNumberAsString),
                            StructuredArguments.kv("sample", sample),
                            StructuredArguments.kv("requestId", requestId)
                        )
                        metrics.countForbiddenOnGetByEon()
                    }
                }
            }
            .mapError { err ->
                when (err) {
                    FindOrderByOrderNumberUseCase.FindOrderError.FORBIDDEN -> StatusResponse.Forbidden
                    FindOrderByOrderNumberUseCase.FindOrderError.NOT_FOUND -> StatusResponse.NotFound
                }
            }
            .onSuccess { order ->
                logger.debug(
                    "[{}]: Found ${order.status}",
                    StructuredArguments.kv("method", "getResultByEon"),
                    StructuredArguments.kv("issuerId", EON_ISSUER_ID),
                    StructuredArguments.kv("orderNumber", order.orderNumber),
                    StructuredArguments.kv("sample", order.sample),
                    StructuredArguments.kv("requestId", requestId)
                )
            }
            .map { order -> StatusResponse.Found(order.status, order.sampledAt) }
            .unify().asEntity()
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
            StructuredArguments.kv("method", "updateOrder"),
            StructuredArguments.kv("issuerId", EON_ISSUER_ID),
            StructuredArguments.kv("orderId", orderId),
            StructuredArguments.kv("requestId", requestId)
        )

        val id = try {
            UUID.fromString(orderId)
        } catch (ex: IllegalArgumentException) {
            val message = "Failed to parse orderId $orderId"
            logger.info(
                "[{}]: $message",
                StructuredArguments.kv("method", "updateOrder"),
                StructuredArguments.kv("issuerId", EON_ISSUER_ID),
                StructuredArguments.kv("orderId", orderId),
                StructuredArguments.kv("requestId", requestId),
                ex
            )
            metrics.countErrorOnParsingOrderNumbersOnUpdate(EON_ISSUER_ID)
            return UpdateOrderResponse.BadRequest(message).asEntity()
        }

        val result = updateOrderUseCase(id, null, updateOrderRequestBody.notificationUrl)
        logger.debug(
            "[{}]: $result",
            StructuredArguments.kv("method", "updateOrder"),
            StructuredArguments.kv("issuerId", EON_ISSUER_ID),
            StructuredArguments.kv("orderId", orderId),
            StructuredArguments.kv("requestId", requestId)
        )

        return when (result) {
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
        val notificationUrl: String?,

        @Schema(
            description = "The sample type that is being used for the lab test.",
            nullable = true,
            required = false,
            defaultValue = "SALIVA",
            example = "BLOOD"
        )
        val sample: Sample = Sample.SALIVA,

        @Schema(
            description = "Verification secret that has to be presented when querying the status on an order by EON",
            nullable = true,
            required = false,
            defaultValue = "null",
            example = "3ASsdfSA*SFDnj!f"
        )
        val verificationSecret: String? = null
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

        object Conflict : IssueExternalOrderNumberResponse(HttpStatus.CONFLICT, false)
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
