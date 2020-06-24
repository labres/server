package com.healthmetrix.labres.lab

import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.stereotype.Service
import com.healthmetrix.labres.logger as hmxLogger

val APPLICATION_KEVB_CSV = MediaType("application", "kevb+csv")
const val APPLICATION_KEVB_CSV_VALUE = "application/kevb+csv"

@Service
class KevbLabResultMessageConverter : AbstractHttpMessageConverter<UpdateResultRequest>(
    APPLICATION_KEVB_CSV
) {

    override fun supports(clazz: Class<*>) = clazz == UpdateResultRequest::class.java

    override fun writeInternal(t: UpdateResultRequest, outputMessage: HttpOutputMessage) =
        throw NotImplementedError("Serialization of LabResults not supported")

    override fun readInternal(clazz: Class<out UpdateResultRequest>, inputMessage: HttpInputMessage): UpdateResultRequest {
        val csv = inputMessage.body
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        val csvParts = csv.trim().split(",")

        if (csvParts.size < 2)
            throw HttpMessageNotReadableException("CSV string $csv contains less than 2 values", inputMessage).also {
                hmxLogger.warn(it.message)
            }

        val orderNumber = csvParts[0]

        val resultString = csvParts[1]
        val result = Result.from(resultString)
            ?: throw HttpMessageNotReadableException("Failed to parse result $resultString", inputMessage).also {
                hmxLogger.warn(
                    it.message,
                    kv("orderNumber", orderNumber)
                )
            }

        val testTypeString = csvParts.getOrNull(2)
        val testType = TestType.from(testTypeString)
            ?: throw HttpMessageNotReadableException("Failed to parse testType $testTypeString", inputMessage).also {
                hmxLogger.warn(
                    it.message,
                    kv("orderNumber", orderNumber)
                )
            }

        return UpdateResultRequest(orderNumber, result, testType)
    }
}
