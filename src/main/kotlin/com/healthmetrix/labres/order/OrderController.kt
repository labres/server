package com.healthmetrix.labres.order

import com.healthmetrix.labres.ApiResponse
import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.persistence.OrderInformationRepository
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

    // TODO catch errors and return new ones based on the spec

    @PostMapping("/v1/orders")
    fun postOrderNumber(): ResponseEntity<CreateOrderResponse> {
        val orderInfo = createOrderUseCase()
        return CreateOrderResponse.Created(
            UUID.randomUUID(), // TODO Fix when implemented
            orderInfo.number.externalOrderNumber,
            "fake token" // TODO Fix when implemented
        ).asEntity()
    }

    @GetMapping("/v1/orders/{orderNumber}")
    fun getOrderNumber(@PathVariable orderNumber: String): ResponseEntity<StatusResponse> {
        val orderInfo = orderInformationRepository.findById(orderNumber)

        return when (orderInfo) {
            null -> StatusResponse.NotFound
            else -> StatusResponse.Found(orderInfo.status)
        }.asEntity()
    }
}

sealed class CreateOrderResponse(httpStatus: HttpStatus, hasBody: Boolean = true) : ApiResponse(httpStatus, hasBody) {
    data class Created(val id: UUID, val externalOrderNumber: String, val token: String) :
        CreateOrderResponse(HttpStatus.CREATED)
}
