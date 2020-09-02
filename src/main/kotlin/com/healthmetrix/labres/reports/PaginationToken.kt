package com.healthmetrix.labres.reports

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.healthmetrix.labres.decodeBase64
import com.healthmetrix.labres.logger
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

data class PaginationToken(
    val reportParameters: ReportParameters,
    val exclusiveStartKey: String
)

@Component
class PaginationTokenBase64Converter(
    private val objectMapper: ObjectMapper
) : Converter<String, PaginationToken> {
    override fun convert(b64: String): PaginationToken? {
        val json = b64.decodeBase64()
            ?: throw IllegalArgumentException("Failed deserializing paginationToken: not valid base64").also {
                logger.warn(it.message)
            }

        return try {
            objectMapper.readValue(json, PaginationToken::class.java)
        } catch (ex: JsonProcessingException) {
            val message = "Failed deserializing paginationToken: ${ex.message}"
            logger.warn(message)
            throw IllegalArgumentException(message, ex)
        }
    }
}
