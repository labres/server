package com.healthmetrix.labres.order

import com.healthmetrix.labres.LabResApiResponse
import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.persistence.OrderInformationRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
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

@ApiResponse(
    responseCode = "401",
    description = "API key invalid or missing",
    headers = [Header(name = "WWW-Authenticate", schema = Schema(type = "string"))]
)

@RestController
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
        description = "Should only be invoked for verified users (logged into account or verified email address)",
        tags = ["External Order Number API"],
        security = [SecurityRequirement(name = "OrdersApiToken")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "External Order Number Created",
                content = [
                    Content(schema = Schema(type = "object", implementation = CreateOrderResponse.Created::class))
                ]
            ),
            ApiResponse(
                responseCode = "401",
                description = "API key invalid or missing",
                headers = [Header(name = "WWW-Authenticate", schema = Schema(type = "string"))]
            )
        ]
    )
    fun postOrderNumber(@RequestBody(required = false) createOrderRequestBody: CreateOrderRequestBody?): ResponseEntity<CreateOrderResponse> {
        val (id, orderNumber) = createOrderUseCase(createOrderRequestBody?.notificationId)
        return CreateOrderResponse.Created(
            id,
            orderNumber.number
        ).asEntity()
    }

    @GetMapping("/v1/orders/{orderId}")
    @Operation(
        tags = ["External Order Number API"],
        security = [SecurityRequirement(name = "OrdersApiToken")]
    )
    fun getOrderNumber(@PathVariable orderId: String): ResponseEntity<StatusResponse> {
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
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        tags = ["External Order Number API"],
        security = [SecurityRequirement(name = "OrdersApiToken")]
    )
    fun updateOrder(
        @PathVariable
        orderId: String,
        @RequestBody
        updateOrderRequestBody: UpdateOrderRequestBody
    ): ResponseEntity<UpdateOrderResponse> = when (updateOrderUseCase(orderId, updateOrderRequestBody.notificationId)) {
        UpdateOrderUseCase.Result.SUCCESS -> UpdateOrderResponse.Updated
        UpdateOrderUseCase.Result.NOT_FOUND -> UpdateOrderResponse.NotFound
        UpdateOrderUseCase.Result.INVALID_ORDER_ID -> UpdateOrderResponse.NotFound
    }.asEntity()
}

data class CreateOrderRequestBody(val notificationId: String)

data class UpdateOrderRequestBody(val notificationId: String)

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
