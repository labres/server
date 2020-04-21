package com.healthmetrix.labres.lab

import com.healthmetrix.labres.order.OrderNumber
import org.springframework.stereotype.Component

const val RESULT_INDEX = 5
const val OBX_MESSAGE_SEPARATOR = "|"

/**
 * A typical OBX Message looks like the following:
 *
 * OBX|3|ST|21300^2019-nCoronav.-RNA Sonst (PCR)|0061749799|Positiv|||N|||S|||20200406101220|Extern|||||||||Extern
 *
 * The lab result status can be found at `OBX-5`, where `OBX-1` is `3` in this example.
 */
@Component
class ExtractObxResultUseCase {
    operator fun invoke(message: String, labId: String): LabResult? {
        /**
         * The following mappings are a WIP, as we still don't know
         * the exact strings they might send over in the result
         * part of the OBX segment
         */
        return when (parseStatus(message)) {
            "Positiv" -> Result.POSITIVE
            "Nicht nachweisbar" -> Result.NEGATIVE
            "Schwach positiv" -> Result.WEAK_POSITIVE
            "Prozessfehler" -> Result.INVALID
            "InArbeit" -> Result.IN_PROGRESS
            else -> null
        }?.let { LabResult(OrderNumber.External("TODO"), labId, it) } // TODO when requirements clarified
    }

    private fun parseStatus(obxSegment: String): String {
        return obxSegment.split(OBX_MESSAGE_SEPARATOR).getOrNull(RESULT_INDEX) ?: ""
    }
}
