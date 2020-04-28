package com.healthmetrix.labres.order

import com.healthmetrix.labres.EXTERNAL_ORDER_NUMBER_API_TAG
import com.healthmetrix.labres.LabResApiResponse
import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.persistence.OrderInformationRepository
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
class OrderController(
    private val createOrderUseCase: CreateOrderUseCase,
    private val orderInformationRepository: OrderInformationRepository,
    private val updateOrderUseCase: UpdateOrderUseCase
) {
    @PostMapping(
        path = ["/v1/orders"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        summary = "Issues a new globally unique External Order Number (EON). The number of issuing  EONs is limited to 3 per subject.",
        description = "Should only be invoked for verified users (logged into account or verified email address)"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "External Order Number Created",
                content = [
                    Content(schema = Schema(type = "object", implementation = CreateOrderResponse.Created::class))
                ]
            )
        ]
    )
    fun postOrderNumber(
        @RequestBody(required = false) // TODO: springdoc-openapi does not correctly infer this. see https://github.com/springdoc/springdoc-openapi/issues/603
        createOrderRequestBody: CreateOrderRequestBody?
    ): ResponseEntity<CreateOrderResponse> {
        val (id, orderNumber) = createOrderUseCase(createOrderRequestBody?.notificationUrl)
        return CreateOrderResponse.Created(
            id,
            orderNumber.number
        ).asEntity()
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
            description = "UUID of an order that has been sent to a lab",
            required = true,
            schema = Schema(type = "string", format = "uuid", description = "A Version 4 UUID")
        )
        @PathVariable orderId: String
    ): ResponseEntity<StatusResponse> {
        val id = try {
            UUID.fromString(orderId)
        } catch (ex: IllegalArgumentException) {
            null
        }

        val orderInfo = id?.let(orderInformationRepository::findById)

        return when (orderInfo) {
            null -> StatusResponse.NotFound
            else -> StatusResponse.Found(orderInfo.status)
        }.asEntity()
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
    ): ResponseEntity<UpdateOrderResponse> = when (updateOrderUseCase(orderId, updateOrderRequestBody.notificationUrl)) {
        UpdateOrderUseCase.Result.SUCCESS -> UpdateOrderResponse.Updated
        UpdateOrderUseCase.Result.NOT_FOUND -> UpdateOrderResponse.NotFound
        UpdateOrderUseCase.Result.INVALID_ORDER_ID -> UpdateOrderResponse.NotFound
    }.asEntity()
}

data class CreateOrderRequestBody(
    @Schema(
        type = "string",
        description = "Notification URL sent from Data4Life that can be used later to notify them that lab results have been uploaded. Must be a valid, complete URL including protocol for an existing HTTPS endpoint supporting POST requests.",
        required = true
    )
    val notificationUrl: String
)

data class UpdateOrderRequestBody(
    @Schema(
        type = "string",
        description = "Notification URL sent from Data4Life that can be used later to notify them that lab results have been uploaded. Must be a valid, complete URL including protocol for an existing HTTPS endpoint supporting POST requests.",
        required = true
    )
    val notificationUrl: String
)

sealed class UpdateOrderResponse(httpStatus: HttpStatus) : LabResApiResponse(httpStatus, false) {
    object Updated : UpdateOrderResponse(HttpStatus.OK)
    object NotFound : UpdateOrderResponse(HttpStatus.NOT_FOUND)
}

sealed class CreateOrderResponse(httpStatus: HttpStatus, hasBody: Boolean = true) :
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
    ) : CreateOrderResponse(HttpStatus.CREATED)
}
