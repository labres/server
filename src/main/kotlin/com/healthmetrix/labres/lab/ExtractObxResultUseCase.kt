package com.healthmetrix.labres.lab

import com.healthmetrix.labres.order.OrderNumber
import org.springframework.stereotype.Component

const val RESULT_INDEX = 5
const val TEST_TYPE_INDEX = 1000 // TODO when specified
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
    operator fun invoke(message: String, labId: String): LabResult? = with(Obx(message)) {
        result?.let { r -> LabResult(orderNumber, labId, r, testType) }
    }

    private class Obx(message: String) {
        private val fields = message.split(OBX_MESSAGE_SEPARATOR)

        /**
         * The following mappings are a WIP, as we still don't know
         * the exact strings they might send over in the result
         * part of the OBX segment
         */
        val result = when (fields.getOrNull(RESULT_INDEX) ?: "") {
            "Positiv" -> Result.POSITIVE
            "Nicht nachweisbar" -> Result.NEGATIVE
            "Schwach positiv" -> Result.WEAK_POSITIVE
            "Prozessfehler" -> Result.INVALID
            "InArbeit" -> Result.IN_PROGRESS
            else -> null
        }

        val testType = fields.getOrNull(TEST_TYPE_INDEX)

        val orderNumber = OrderNumber.External("TODO") // TODO when specified
    }
}
