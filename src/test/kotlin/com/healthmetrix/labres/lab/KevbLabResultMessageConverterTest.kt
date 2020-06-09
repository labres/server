package com.healthmetrix.labres.lab

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpInputMessage
import org.springframework.http.converter.HttpMessageNotReadableException

internal class KevbLabResultMessageConverterTest {

    val orderNumber = "1234567890"
    val result = Result.POSITIVE
    val testType = "multiple_choice"

    val underTest = KevbLabResultMessageConverter()
    val inputMessage: HttpInputMessage = mockk()

    @Test
    fun `readInternal should return updateResultRequest`() {
        every { inputMessage.body } returns "$orderNumber,$result".byteInputStream()

        val res = underTest.read(UpdateResultRequest::class.java, inputMessage)

        assertThat(res).isEqualTo(UpdateResultRequest(orderNumber, result, null))
    }

    @Test
    fun `readInternal should return updateResultRequest with testType`() {
        every { inputMessage.body } returns "$orderNumber,$result,$testType".byteInputStream()

        val res = underTest.read(UpdateResultRequest::class.java, inputMessage)

        assertThat(res).isEqualTo(UpdateResultRequest(orderNumber, result, testType))
    }

    @Test
    fun `readInternal should throw HttpMessageNotReadableException when csv input has no comma`() {
        every { inputMessage.body } returns "$orderNumber$result".byteInputStream()

        assertThrows(HttpMessageNotReadableException::class.java) {
            underTest.read(UpdateResultRequest::class.java, inputMessage)
        }
    }

    @Test
    fun `readInternal should throw HttpMessageNotReadableException when the result is not interpretable`() {
        every { inputMessage.body } returns "$orderNumber,wrong".byteInputStream()

        assertThrows(HttpMessageNotReadableException::class.java) {
            underTest.read(UpdateResultRequest::class.java, inputMessage)
        }
    }
}
