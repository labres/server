package com.healthmetrix.labres.reports

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.healthmetrix.labres.encodeBase64
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException
import java.util.UUID

internal class PaginationTokenBase64ConverterTest {
    private val objectMapper: ObjectMapper = mockk()

    private val underTest = PaginationTokenBase64Converter(objectMapper)

    private val b64 = "I'm a pagination token".encodeBase64()
    private val paginationToken = PaginationToken(
        exclusiveStartKey = UUID.randomUUID().toString(),
        reportParameters = ReportParameters(
            event = "event",
            sampledAfter = null,
            reportedAfter = null
        )
    )

    @Test
    fun `it should convert b64 to pagination token`() {
        every { objectMapper.readValue(any<String>(), PaginationToken::class.java) } returns paginationToken

        val res = underTest.convert(b64)

        assertThat(res).isEqualTo(paginationToken)
    }

    @Test
    fun `it should throw IllegalArgumentException on invalid b64`() {
        every { objectMapper.readValue(any<String>(), PaginationToken::class.java) } returns paginationToken

        assertThrows<IllegalArgumentException> {
            underTest.convert("! InValid !")
        }
    }

    @Test
    fun `it should throw IllegalArgumentException when json can't be deserialized`() {
        every {
            objectMapper.readValue(any<String>(), PaginationToken::class.java)
        } throws mockk<JsonProcessingException>() {
            every { message } returns "test"
        }

        assertThrows<IllegalArgumentException> {
            underTest.convert(b64)
        }
    }
}
