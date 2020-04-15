package com.healthmetrix.labres.order

const val ALPHABET = "0123456789"
const val LENGTH = 10

sealed class OrderNumber {

    fun eon() = when (this) {
        is External -> this.number
        is Internal -> null
    }

    fun labId() = when (this) {
        is External -> null
        is Internal -> this.labId
    }

    fun ion() = when (this) {
        is External -> null
        is Internal -> this.number
    }

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

    data class Internal(val labId: String, val number: String) : OrderNumber() {
        companion object {
            fun from(labId: String?, number: String?) = when {
                labId == null || number == null -> null
                else -> Internal(labId, number)
            }
        }
    }
}
