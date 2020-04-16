package com.healthmetrix.labres.lab

import org.springframework.stereotype.Component

@Component
class ExtractLabIdUseCase {
    operator fun invoke(header: String): String? {
        return "labId" // TODO once requirements clarified
    }
}
