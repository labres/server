package com.healthmetrix.labres.lab

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

const val PCR_LOINC = "94531-1"

@JsonDeserialize(using = TestTypeJsonDeserializer::class)
enum class TestType {
    PCR,
    NGS,
    ANTIBODY;

    companion object {
        fun from(s: String?): TestType? {
            // default type
            if (s == null)
                return PCR

            // workaround for not breaking the existing KEVB integration
            if (s == PCR_LOINC)
                return PCR

            return try {
                valueOf(s.toUpperCase())
            } catch (ex: IllegalArgumentException) {
                null
            }
        }
    }
}

class TestTypeJsonDeserializer : StdDeserializer<TestType>(TestType::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TestType {
        val jsonString: String? = p.valueAsString

        return TestType.from(jsonString)
            ?: throw IllegalArgumentException(
                "Error occurred parsing $jsonString to TestType. Allowed values are ${TestType.values()
                    .joinToString(", ")}, $PCR_LOINC"
            )
    }
}
