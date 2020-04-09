package com.healthmetrix.labres.lab

import org.springframework.stereotype.Component

@Component
class ExtractInfoUseCase {
    operator fun invoke(ldt: String): LabResult? {
        // TODO
        return Result.from(ldt)?.let(::LabResult)
    }
}
