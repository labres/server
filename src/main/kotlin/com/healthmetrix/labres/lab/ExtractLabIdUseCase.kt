package com.healthmetrix.labres.lab

import com.healthmetrix.labres.decodeBase64
import org.springframework.stereotype.Component

@Component
class ExtractLabIdUseCase {
    operator fun invoke(header: String): String? {
        val (basic, encoded) = header.split(" ").also {
            if (it.size != 2) return null
        }

        if (basic != "Basic")
            return null

        val decoded = try {
            encoded.decodeBase64()?.split(":")
        } catch (ex: IllegalArgumentException) {
            null
        } ?: return null

        if (decoded.size != 2)
            return null

        return decoded.first()
    }
}
