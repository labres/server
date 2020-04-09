package com.healthmetrix.labres.order

const val ALPHABET = "0123456789"
const val LENGTH = 10

data class OrderNumber(val externalOrderNumber: String) {
    companion object {
        fun random(): OrderNumber = (0 until LENGTH).map {
            ALPHABET.random()
        }.joinToString("").let(::OrderNumber)

        fun from(s: String?) = s?.let { num ->
            if (num.matches(Regex("[$ALPHABET]{$LENGTH}")))
                OrderNumber(num)
            else
                null
        }
    }
}
