package com.healthmetrix.labres

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapBoth
import org.springframework.http.HttpHeaders
import org.springframework.util.LinkedMultiValueMap
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

fun String.extractBasicAuthUser(): String? {
    val (prefix, encoded) = this.split(" ").also {
        if (it.size != 2) return null
    }

    if (prefix != "Basic")
        return null

    val decoded = encoded.decodeBase64()
        ?.split(":")
        ?: return null

    if (decoded.size != 2)
        return null

    return decoded.first()
}

fun Map<String, List<String>>.toHttpHeaders() = HttpHeaders(LinkedMultiValueMap(this))
