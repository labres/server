package com.healthmetrix.labres.order

import com.healthmetrix.labres.ApiResponse
import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.persistence.OrderInformationRepository
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController(
    private val createOrderUseCase: CreateOrderUseCase,
    private val orderInformationRepository: OrderInformationRepository
) {
    @PostMapping(
        path = ["/v1/orders"],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun postOrderNumber(@RequestBody(required = false) createOrderRequestBody: CreateOrderRequestBody?): ResponseEntity<CreateOrderResponse> {
        val (id, orderNumber) = createOrderUseCase(createOrderRequestBody?.notificationId)
        return CreateOrderResponse.Created(
            id,
            orderNumber.number
        ).asEntity()
    }

    @GetMapping("/v1/orders/{orderId}")
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
}

data class CreateOrderRequestBody(val notificationId: String)

sealed class CreateOrderResponse(httpStatus: HttpStatus, hasBody: Boolean = true) : ApiResponse(httpStatus, hasBody) {
    data class Created(
        val id: UUID,
        val orderNumber: String
    ) : CreateOrderResponse(HttpStatus.CREATED)
}
