package com.healthmetrix.labres.lab

import com.healthmetrix.labres.order.Status
import org.springframework.stereotype.Component

@Component
class ExtractStatusUseCase {
    operator fun invoke(ldt: String): Status? {
        // TODO
        return (Status.values().toList() + listOf(null)).random()
    }
}
