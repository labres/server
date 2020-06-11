package com.healthmetrix.labres.lab

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class TestTypeTest {

    @Nested
    inner class Companion {

        @ParameterizedTest
        @EnumSource(TestType::class)
        fun `from should return correct TestType`(testType: TestType) {
            assertThat(TestType.from(testType.toString())).isEqualTo(testType)
        }

        @Test
        fun `from should return PCR for 94531-1`() {
            assertThat(TestType.from(PCR_LOINC)).isEqualTo(TestType.PCR)
        }

        @Test
        fun `from should default to PCR for null as input`() {
            assertThat(TestType.from(null)).isEqualTo(TestType.PCR)
        }

        @Test
        fun `from should return null when invalid input was given`() {
            assertThat(TestType.from("invalid")).isNull()
        }
    }

    @Nested
    inner class TestTypeJsonDeserializerTest {
        private val jsonParser: JsonParser = mockk() {
            every { valueAsString } returns "pcr"
        }
        private val context: DeserializationContext = mockk()

        private val underTest = TestTypeJsonDeserializer()

        @BeforeEach
        internal fun setUp() {
            mockkObject(TestType.Companion)
        }

        @AfterEach
        internal fun tearDown() {
            clearMocks(TestType.Companion)
        }

        @Test
        fun `deserialize should deserialize correctly`() {
            every { TestType.from(any()) } returns TestType.PCR

            val result = underTest.deserialize(jsonParser, context)

            assertThat(result).isEqualTo(TestType.PCR)
        }

        @Test
        fun `deserialize should throw an IllegalArgumentException for wrong input`() {
            every { TestType.from(any()) } returns null

            assertThrows<IllegalArgumentException> { underTest.deserialize(jsonParser, context) }
        }
    }
}
