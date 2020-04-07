package com.healthmetrix.labres.order

import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController(
    private val orderInformationRepository: OrderInformationRepository
) {

    // TODO catch errors and return new ones based on the spec

    @PostMapping("/v1/order")
    fun postOrderNumber(): OrderNumber {
        val order = OrderInformation(
            OrderNumber.random(),
            Status.values().random(),
            updatedAt = null,
            hash = null
        )
        orderInformationRepository.save(order)
        return order.number
    }

    @GetMapping("/v1/order/{orderNumber}")
    fun getOrderNumber(@PathVariable orderNumber: String, @RequestParam hash: String): ResponseEntity<StatusResponse> {
        val orderInfo = orderInformationRepository.findById(orderNumber)

        return when {
            orderInfo == null -> StatusResponse.NotFound
            orderInfo.hash != hash -> StatusResponse.WrongHash
            else -> StatusResponse.Found(orderInfo.status)
        }.asEntity()
    }
}
