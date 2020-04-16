package com.healthmetrix.labres.order

import com.healthmetrix.labres.ApiResponse
import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.persistence.OrderInformationRepository
import java.lang.IllegalArgumentException
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController(
    private val createOrderUseCase: CreateOrderUseCase,
    private val orderInformationRepository: OrderInformationRepository
) {
    @PostMapping("/v1/orders")
    fun postOrderNumber(): ResponseEntity<CreateOrderResponse> {
        val (id, eon) = createOrderUseCase()
        return CreateOrderResponse.Created(
            id,
            eon.number,
            "fake token" // TODO Fix when implemented in future ticket
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

sealed class CreateOrderResponse(httpStatus: HttpStatus, hasBody: Boolean = true) : ApiResponse(httpStatus, hasBody) {
    data class Created(
        val id: UUID,
        val externalOrderNumber: String,
        val token: String
    ) : CreateOrderResponse(HttpStatus.CREATED)
}
