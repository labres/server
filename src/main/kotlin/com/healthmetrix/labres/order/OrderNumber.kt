package com.healthmetrix.labres.order

const val ALPHABET = "0123456789"
const val LENGTH = 10

sealed class OrderNumber {

    fun eon() = (this as External).number

    fun eonOrNull() = (this as? External)?.number

    data class External(val number: String) : OrderNumber() {
        companion object {
            fun random(): External = (0 until LENGTH).map {
                ALPHABET.random()
            }.joinToString("").let(::External)

            fun from(s: String?) = s?.let { num ->
                if (num.matches(Regex("[$ALPHABET]{$LENGTH}")))
                    External(num)
                else
                    null
            }
        }
    }
}
