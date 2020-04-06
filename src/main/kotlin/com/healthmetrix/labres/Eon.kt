package com.healthmetrix.labres

const val ALPHABET = "0123456789abcdefghikmnpqrstuvwxyz"

data class Eon(val value: String) {
    companion object {
        fun random() = (0..8).map {
            ALPHABET.random()
        }.joinToString("").let(::Eon)
    }
}
