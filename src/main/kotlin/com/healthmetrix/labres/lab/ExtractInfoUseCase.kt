package com.healthmetrix.labres.lab

import com.healthmetrix.labres.order.Status
import org.springframework.stereotype.Component

@Component
class ExtractInfoUseCase {
    operator fun invoke(ldt: String): Info? {
        // TODO
        return Status.from(ldt)?.let(::Info)
    }

    data class Info(val status: Status)
}
