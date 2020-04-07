package com.healthmetrix.labres.lab

import com.healthmetrix.labres.order.Status
import org.springframework.stereotype.Component

@Component
class ExtractInfoUseCase {
    operator fun invoke(ldt: String): Pair<Status, String>? {
        // TODO
        return (Status.from(ldt))?.let {
            it to (0 until 32).map {
                ('0'..'f').random()
            }.joinToString("")
        }
    }
}
