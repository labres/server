package com.healthmetrix.labres.lab

import com.healthmetrix.labres.order.Status
import org.springframework.stereotype.Component

@Component
class ExtractInfoUseCase {
    operator fun invoke(ldt: String): Info? {
        // TODO
        return (Status.from(ldt))?.let { status ->
            val fakeHash = (0 until 32).map {
                ('0'..'f').random()
            }.joinToString("")

            Info(status, fakeHash)
        }
    }

    data class Info(val status: Status, val hash: String)
}
