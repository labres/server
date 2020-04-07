package com.healthmetrix.labres.order

const val ALPHABET = "0123456789abcdefghikmnpqrstuvwxyz"

data class OrderNumber(val externalOrderNumber: String) {
    companion object {
        fun random(): OrderNumber = (0 until 8).map {
            ALPHABET.random()
        }.joinToString("").let(::OrderNumber)

        fun from(s: String?) = s?.let { num ->
            if (num.matches(Regex("[$ALPHABET]{8}")))
                OrderNumber(num)
            else
                null
        }
    }
}
