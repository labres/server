package com.healthmetrix.labres.order

import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.persistence.OrderInformationRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController(
    private val createOrderUseCase: CreateOrderUseCase,
    private val orderInformationRepository: OrderInformationRepository
) {

    @PostMapping("/v1/order")
    @ResponseStatus(HttpStatus.CREATED)
    fun postOrderNumber(): OrderNumber = createOrderUseCase().number

    @GetMapping("/v1/order/{orderNumber}")
    fun getOrderNumber(@PathVariable orderNumber: String): ResponseEntity<StatusResponse> {
        val orderInfo = orderInformationRepository.findById(orderNumber)

        return when (orderInfo) {
            null -> StatusResponse.NotFound
            else -> StatusResponse.Found(orderInfo.status)
        }.asEntity()
    }
}
