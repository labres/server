package com.healthmetrix.labres.order

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController {

    // TODO catch errors and return new ones based on the spec

    @PostMapping("/v1/order")
    fun postOrderNumber(): OrderNumber {
        return OrderNumber.random()
    }

    @GetMapping("/v1/order/{orderNumber}")
    fun getOrderNumber(@PathVariable orderNumber: String, @RequestParam hash: String): StatusResponse {
        return StatusResponse(Status.POSITIVE)
    }
}
