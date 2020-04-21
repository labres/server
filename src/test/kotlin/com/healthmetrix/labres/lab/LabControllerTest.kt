package com.healthmetrix.labres.lab

import com.fasterxml.jackson.databind.ObjectMapper
import com.healthmetrix.labres.LabResTestApplication
import com.healthmetrix.labres.encodeBase64
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Status
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.put

@SpringBootTest(
    classes = [LabResTestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
class LabControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var updateResultUseCase: UpdateResultUseCase

    @MockkBean
    private lateinit var extractObxResultUseCase: ExtractObxResultUseCase

    @MockkBean
    private lateinit var extractLdtResultUseCase: ExtractLdtResultUseCase

    @MockkBean
    private lateinit var extractLabIdUseCase: ExtractLabIdUseCase

    private val labIdHeader = "Basic ${"user:pass".encodeBase64()}"

    @Nested
    inner class JSON {
        @Test
        fun `uploading a valid json request body with an external order number returns 200`() {
            every { extractLabIdUseCase(any()) } returns "labId"
            every { updateResultUseCase(any(), any()) } returns UpdateStatusResponse.Success

            mockMvc.put("/v1/results/json") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "orderNumber" to "0123456789",
                        "result" to Status.NEGATIVE
                    )
                )
            }.andExpect {
                status { isOk }
            }
        }

        @Test
        fun `upload a document to an unknown external order number returns 404`() {
            every { extractLabIdUseCase(any()) } returns "labId"
            every { updateResultUseCase(any(), any()) } returns UpdateStatusResponse.OrderNotFound

            mockMvc.put("/v1/results/json") {
                contentType = MediaType.APPLICATION_JSON
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "orderNumber" to "0123456789",
                        "result" to Status.POSITIVE
                    )
                )
            }.andExpect {
                status { isNotFound }
            }
        }

        @Test
        fun `upload a document to an invalid external order number returns 404`() {
            mockMvc.put("/v1/results/json") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "orderNumber" to "nonsense",
                        "result" to Status.POSITIVE
                    )
                )
            }.andExpect {
                status { isNotFound }
            }
        }

        @Test
        fun `upload document with invalid auth header returns 404`() {
            every { extractLabIdUseCase(any()) } returns null
            mockMvc.put("/v1/results/json") {
                contentType = MediaType.APPLICATION_JSON
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "orderNumber" to "0123456789",
                        "result" to Status.POSITIVE
                    )
                )
            }.andExpect {
                status { isNotFound }
            }
        }
    }

    @Nested
    inner class OBX {

        @Test
        fun `uploading a pretend OBX message returns 200`() {
            every { updateResultUseCase(any(), any()) } returns
                    UpdateStatusResponse.Success
            every { extractObxResultUseCase(any(), any()) } returns
                    LabResult(OrderNumber.External.random(), labIdHeader, Result.NEGATIVE)

            mockMvc.put("/v1/results/obx") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.TEXT_PLAIN
                content = "NEGATIVE"
            }.andExpect {
                status { isOk }
            }
        }

        @Test
        fun `uploading an invalid OBX message returns 500`() {
            every { extractObxResultUseCase(any(), any()) } returns null

            mockMvc.put("/v1/results/obx") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.TEXT_PLAIN
                content = "NOT OBX"
            }.andExpect {
                status { isBadRequest }
            }
        }
    }

    @Nested
    inner class LDT {
        @Test
        fun `uploading an invalid ldt document returns 500`() {
            every { extractLdtResultUseCase(any(), any()) } returns null

            mockMvc.put("/v1/results/ldt") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.TEXT_PLAIN
                content = "NOT LDT"
            }.andExpect {
                status { isBadRequest }
            }
        }
    }
}
