package com.healthmetrix.labres

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController {

    @PostMapping("/v1/order")
    fun postOrderNumber(): OrderNumber {
        return OrderNumber.random()
    }
}

const val ALPHABET = "0123456789abcdefghikmnpqrstuvwxyz"

data class OrderNumber(val externalOrderNumber: String) {
    companion object {
        fun random(): OrderNumber = (0 until 8).map {
            ALPHABET.random()
        }.joinToString("").let(::OrderNumber)
    }
}
