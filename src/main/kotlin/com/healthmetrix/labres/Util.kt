package com.healthmetrix.labres

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapBoth
import java.lang.IllegalArgumentException
import java.util.Base64

fun String.encodeBase64(): String =
    Base64.getEncoder().encodeToString(this.toByteArray())

fun String.decodeBase64(): String? {
    return try {
        Base64.getDecoder().decode(this).toString(Charsets.UTF_8)
    } catch (ex: IllegalArgumentException) {
        null
    }
}

fun <T> Result<T, T>.unify(): T {
    return mapBoth({ it }, { it })
}
