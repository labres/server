package com.healthmetrix.labres.order

import com.healthmetrix.labres.logger
import java.lang.IllegalArgumentException

const val EON_ALPHABET = "0123456789"
const val EON_LENGTH = 10
const val EON_PATTERN = "[$EON_ALPHABET]{$EON_LENGTH}"
const val EON_ISSUER_ID = "labres"

sealed class OrderNumber {

    abstract val number: String
    abstract val issuerId: String

    class External private constructor(override val number: String) : OrderNumber() {
        override val issuerId = EON_ISSUER_ID

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as External

            if (number != other.number) return false

            return true
        }

        override fun hashCode(): Int {
            return number.hashCode()
        }

        override fun toString(): String {
            return "External(number='$number')"
        }

        companion object {
            fun from(number: String): External = if (number.matches(Regex(EON_PATTERN)))
                External(number)
            else throw IllegalArgumentException("OrderNumber $number does not match $EON_PATTERN").also {
                    logger.info(it.message)
                }

            fun random(): External = (0 until EON_LENGTH).map {
                EON_ALPHABET.random()
            }.joinToString("").let(::External)
        }
    }

    data class PreIssued(override val issuerId: String, override val number: String) : OrderNumber()

    companion object {
        fun from(issuerId: String?, number: String) = when (issuerId) {
            null -> External.from(number)
            EON_ISSUER_ID -> External.from(number)
            else -> PreIssued(issuerId, number)
        }
    }
}
