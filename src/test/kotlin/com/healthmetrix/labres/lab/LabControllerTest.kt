package com.healthmetrix.labres.lab

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.healthmetrix.labres.encodeBase64
import com.healthmetrix.labres.order.EON_ISSUER_ID
import com.healthmetrix.labres.order.Status
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.core.Is
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.UUID

class LabControllerTest {
    private val updateResultUseCase: UpdateResultUseCase = mockk()
    private val bulkUpdateResultsUseCase: BulkUpdateResultsUseCase = mockk()
    private val labRegistry: LabRegistry = mockk()
    private val metrics: LabMetrics = mockk(relaxUnitFun = true)

    private val underTest = LabController(updateResultUseCase, bulkUpdateResultsUseCase, labRegistry, metrics)

    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val jsonMessageConverter = MappingJackson2HttpMessageConverter(objectMapper)
    private val mockMvc = MockMvcBuilders
        .standaloneSetup(underTest)
        .setMessageConverters(jsonMessageConverter, KevbLabResultMessageConverter()) // rather mock?
        .build()

    private val labId = "test-lab"
    private val issuerId = "test-issuer"
    private val orderNumber = "0123456789"
    private val labIdHeader = "Basic ${"$labId:pass".encodeBase64()}"
    private val verificationSecret = UUID.randomUUID().toString()

    @BeforeEach
    internal fun setUp() {
        every { labRegistry.get(any()) } returns RegisteredLab(labId, listOf(issuerId, EON_ISSUER_ID))
        every { updateResultUseCase(any(), any(), any(), any()) } returns UpdateResult.SUCCESS
    }

    @Nested
    inner class UpdateJsonResult {

        @Test
        fun `uploading a valid json request body with an external order number returns 200`() {
            mockMvc.put("/v1/results") {
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
        fun `uploading a valid json request body with an external order number calls the updateResultsUseCase`() {
            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "orderNumber" to orderNumber,
                        "result" to Status.NEGATIVE
                    )
                )
            }

            verify {
                updateResultUseCase.invoke(
                    updateResultRequest = match {
                        it.orderNumber == orderNumber &&
                            it.result == Result.NEGATIVE &&
                            it.type == TestType.PCR
                    },
                    labId = labId,
                    issuerId = null,
                    now = any()
                )
            }
        }

        @Test
        fun `uploading a valid json request body with a preissued order number returns 200`() {
            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                param("issuerId", issuerId)
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
        fun `uploading a valid json request body with a preissued order number calls the updateResultsUseCase`() {
            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                param("issuerId", issuerId)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "orderNumber" to orderNumber,
                        "result" to Status.NEGATIVE
                    )
                )
            }

            verify {
                updateResultUseCase.invoke(
                    updateResultRequest = match {
                        it.orderNumber == orderNumber &&
                            it.result == Result.NEGATIVE
                    },
                    labId = labId,
                    issuerId = issuerId,
                    now = any()
                )
            }
        }

        @Test
        fun `upload a result to an unknown order number returns 404`() {
            every {
                updateResultUseCase(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns UpdateResult.ORDER_NOT_FOUND

            mockMvc.put("/v1/results") {
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
        fun `upload a result to an invalid order number returns 400`() {
            every {
                updateResultUseCase(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns UpdateResult.INVALID_ORDER_NUMBER

            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "orderNumber" to orderNumber,
                        "result" to Status.POSITIVE
                    )
                )
            }.andExpect {
                status { isBadRequest }
            }
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "Wrong dXNlcjpwYXNz",
                "Basic wrong",
                "Basic dXNlcjpwYXNz dXNlcjpwYXNz",
                "Basic dXNlcjpwYXNzOndyb25n"
            ]
        )
        fun `upload result with invalid auth header returns 401`(labHeaderValue: String) {
            mockMvc.put("/v1/results") {
                contentType = MediaType.APPLICATION_JSON
                header(HttpHeaders.AUTHORIZATION, labHeaderValue)
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "orderNumber" to "0123456789",
                        "result" to Status.POSITIVE
                    )
                )
            }.andExpect {
                status { isUnauthorized }
            }
        }

        @Test
        fun `upload result returns 401 when lab has not been registered`() {
            every { labRegistry.get(any()) } returns null

            mockMvc.put("/v1/results") {
                contentType = MediaType.APPLICATION_JSON
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "orderNumber" to "0123456789",
                        "result" to Status.POSITIVE
                    )
                )
            }.andExpect {
                status { isUnauthorized }
            }
        }

        @Test
        fun `upload result returns 403 when registered lab is not allowed to upload for the given issuer`() {
            every { labRegistry.get(any()) } returns RegisteredLab(labId, emptyList())

            mockMvc.put("/v1/results") {
                contentType = MediaType.APPLICATION_JSON
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "orderNumber" to "0123456789",
                        "result" to Status.POSITIVE
                    )
                )
            }.andExpect {
                status { isForbidden }
            }
        }

        @Test
        fun `uploading a document with the optional test type returns 200`() {
            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "orderNumber" to "0123456789",
                        "result" to Status.NEGATIVE,
                        "type" to TestType.ANTIBODY.toString()
                    )
                )
            }.andExpect {
                status { isOk }
            }
        }

        @Test
        fun `uploading a document with the optional test type as loinc code for kevb returns 200`() {
            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "orderNumber" to "0123456789",
                        "result" to Status.NEGATIVE,
                        "type" to PCR_LOINC
                    )
                )
            }.andExpect {
                status { isOk }
            }
        }

        @Test
        fun `uploading a document with the optional test type calls the updateStateUseCase`() {
            val testType = TestType.ANTIBODY

            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "orderNumber" to orderNumber,
                        "result" to Status.NEGATIVE,
                        "type" to testType
                    )
                )
            }

            verify {
                updateResultUseCase.invoke(
                    updateResultRequest = match {
                        it.orderNumber == orderNumber &&
                            it.result == Result.NEGATIVE &&
                            it.type == testType
                    },
                    labId = labId,
                    issuerId = null,
                    now = any()
                )
            }
        }

        @Test
        fun `uploading a document with optional value sampledAt returns 200`() {
            val sampledAt = 1596186947L

            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "orderNumber" to "0123456789",
                        "result" to Status.NEGATIVE,
                        "sampledAt" to sampledAt
                    )
                )
            }

            verify {
                updateResultUseCase.invoke(
                    updateResultRequest = match {
                        it.orderNumber == orderNumber &&
                            it.result == Result.NEGATIVE &&
                            it.sampledAt == sampledAt
                    },
                    labId = labId,
                    issuerId = null,
                    now = any()
                )
            }
        }

        @Test
        fun `uploading a document with optional value sampledAt persists it`() {
            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "orderNumber" to "0123456789",
                        "result" to Status.NEGATIVE,
                        "sampledAt" to 1596186947L
                    )
                )
            }.andExpect {
                status { isOk }
            }
        }

        @Test
        fun `uploading a document with optional value verificationSecret returns 200`() {
            val sampledAt = 1596186947L

            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "orderNumber" to "0123456789",
                        "result" to Status.NEGATIVE,
                        "verificationSecret" to verificationSecret
                    )
                )
            }

            verify {
                updateResultUseCase.invoke(
                    updateResultRequest = match {
                        it.orderNumber == orderNumber &&
                            it.result == Result.NEGATIVE &&
                            it.verificationSecret == verificationSecret
                    },
                    labId = labId,
                    issuerId = null,
                    now = any()
                )
            }
        }

        @Test
        fun `uploading a document with optional value verificationSecret persists it`() {
            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "orderNumber" to "0123456789",
                        "result" to Status.NEGATIVE,
                        "verificationSecret" to verificationSecret
                    )
                )
            }.andExpect {
                status { isOk }
            }
        }
    }

    @Nested
    inner class UpdateKevbCsvResult {

        @Test
        fun `uploading a valid csv request body with an external order number returns 200`() {
            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = APPLICATION_KEVB_CSV
                content = "$orderNumber,${Status.NEGATIVE}"
            }.andExpect {
                status { isOk }
            }
        }

        @Test
        fun `uploading a valid csv request body with an external order number calls the updateResultsUseCase`() {
            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = APPLICATION_KEVB_CSV
                content = "$orderNumber,${Status.NEGATIVE}"
            }

            verify {
                updateResultUseCase.invoke(
                    updateResultRequest = match {
                        it.orderNumber == orderNumber &&
                            it.result == Result.NEGATIVE
                    },
                    labId = labId,
                    issuerId = null,
                    now = any()
                )
            }
        }

        @Test
        fun `uploading a valid csv request body with a preissued order number returns 200`() {
            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                param("issuerId", issuerId)
                contentType = APPLICATION_KEVB_CSV
                content = "0123456789,${Status.POSITIVE}"
            }.andExpect {
                status { isOk }
            }
        }

        @Test
        fun `uploading a valid csv request body with a preissued order number calls the updateResultsUseCase`() {
            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                param("issuerId", issuerId)
                contentType = APPLICATION_KEVB_CSV
                content = "$orderNumber,${Status.POSITIVE}"
            }

            verify {
                updateResultUseCase.invoke(
                    updateResultRequest = match {
                        it.orderNumber == orderNumber &&
                            it.result == Result.POSITIVE &&
                            it.type == TestType.PCR
                    },
                    labId = labId,
                    issuerId = issuerId,
                    now = any()
                )
            }
        }

        @Test
        fun `upload a result to an unknown order number returns 404`() {
            every {
                updateResultUseCase(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns UpdateResult.ORDER_NOT_FOUND

            mockMvc.put("/v1/results") {
                contentType = APPLICATION_KEVB_CSV
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                content = "0123456789,${Status.POSITIVE}"
            }.andExpect {
                status { isNotFound }
            }
        }

        @Test
        fun `upload a result to an invalid order number returns 400`() {
            every {
                updateResultUseCase(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns UpdateResult.INVALID_ORDER_NUMBER

            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = APPLICATION_KEVB_CSV
                content = "$orderNumber,${Status.POSITIVE}"
            }.andExpect {
                status { isBadRequest }
            }
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "Wrong dXNlcjpwYXNz",
                "Basic wrong",
                "Basic dXNlcjpwYXNz dXNlcjpwYXNz",
                "Basic dXNlcjpwYXNzOndyb25n"
            ]
        )
        fun `upload result with invalid auth header returns 401`(labHeaderValue: String) {
            mockMvc.put("/v1/results") {
                contentType = APPLICATION_KEVB_CSV
                header(HttpHeaders.AUTHORIZATION, labHeaderValue)
                content = "0123456789,${Status.POSITIVE}"
            }.andExpect {
                status { isUnauthorized }
            }
        }

        @Test
        fun `upload result returns 401 when lab has not been registered`() {
            every { labRegistry.get(any()) } returns null

            mockMvc.put("/v1/results") {
                contentType = APPLICATION_KEVB_CSV
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                content = "0123456789,${Status.POSITIVE}"
            }.andExpect {
                status { isUnauthorized }
            }
        }

        @Test
        fun `upload result returns 403 when registered lab is not allowed to upload for the given issuer`() {
            every { labRegistry.get(any()) } returns RegisteredLab(labId, emptyList())

            mockMvc.put("/v1/results") {
                contentType = APPLICATION_KEVB_CSV
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                content = "0123456789,${Status.POSITIVE}"
            }.andExpect {
                status { isForbidden }
            }
        }

        @Test
        fun `uploading a result with the optional test type returns 200`() {
            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = APPLICATION_KEVB_CSV
                content = "0123456789,${Status.NEGATIVE},${TestType.ANTIBODY}"
            }.andExpect {
                status { isOk }
            }
        }

        @Test
        fun `uploading a result with the optional test type as loinc code for kevb returns 200`() {
            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = APPLICATION_KEVB_CSV
                content = "0123456789,${Status.NEGATIVE},94531-1"
            }.andExpect {
                status { isOk }
            }
        }

        @Test
        fun `uploading a result with the optional test type calls the updateStateUseCase`() {
            val testType = TestType.PCR

            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = APPLICATION_KEVB_CSV
                content = "$orderNumber,${Status.NEGATIVE},$testType"
            }

            verify {
                updateResultUseCase.invoke(
                    updateResultRequest = match {
                        it.orderNumber == orderNumber &&
                            it.result == Result.NEGATIVE &&
                            it.type == testType
                    },
                    labId = labId,
                    issuerId = null,
                    now = any()
                )
            }
        }
    }

    @Nested
    inner class BulkUpdateResults {

        @Test
        fun `bulk uploading status of a list of externally issued orders returns status 200`() {
            every { bulkUpdateResultsUseCase(any(), any(), any()) } returns listOf()

            val anotherOrderNumber = "9876543210"

            mockMvc.put("/v1/results/bulk") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    BulkUpdateResultRequest(
                        listOf(
                            UpdateResultRequest(
                                orderNumber = orderNumber,
                                result = Result.NEGATIVE
                            ),
                            UpdateResultRequest(
                                orderNumber = anotherOrderNumber,
                                result = Result.NEGATIVE
                            )
                        )
                    )
                )
            }.andExpect {
                status { isOk }
            }
        }

        @Test
        fun `bulk uploading status of a list of externally issued orders returns the number of processed rows`() {
            every { bulkUpdateResultsUseCase(any(), any(), any()) } returns listOf()

            mockMvc.put("/v1/results/bulk") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    BulkUpdateResultRequest(
                        listOf(
                            UpdateResultRequest(
                                orderNumber = orderNumber,
                                result = Result.NEGATIVE
                            ),
                            UpdateResultRequest(
                                orderNumber = "9876543210",
                                result = Result.NEGATIVE
                            )
                        )
                    )
                )
            }.andExpect {
                jsonPath("$.processedRows", Is.`is`(2))
            }
        }

        @Test
        fun `bulk uploading status of a list of pre issued orders returns the number of processed rows`() {
            every { bulkUpdateResultsUseCase(any(), any(), any()) } returns listOf()

            mockMvc.put("/v1/results/bulk") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                param("issuerId", issuerId)
                content = objectMapper.writeValueAsString(
                    BulkUpdateResultRequest(
                        listOf(
                            UpdateResultRequest(
                                orderNumber = orderNumber,
                                result = Result.NEGATIVE
                            ),
                            UpdateResultRequest(
                                orderNumber = orderNumber + "1",
                                result = Result.NEGATIVE
                            ),
                            UpdateResultRequest(
                                orderNumber = orderNumber + "2",
                                result = Result.POSITIVE
                            ),
                            UpdateResultRequest(
                                orderNumber = orderNumber + "3",
                                result = Result.POSITIVE
                            )
                        )
                    )
                )
            }.andExpect {
                jsonPath("$.processedRows", Is.`is`(4))
            }
        }

        @Test
        fun `bulk uploading status of a list of pre issued orders calls bulkUpdateResultsUseCase`() {
            clearMocks(bulkUpdateResultsUseCase)
            every { bulkUpdateResultsUseCase(any(), any(), any()) } returns listOf()

            mockMvc.put("/v1/results/bulk") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                param("issuerId", issuerId)
                content = objectMapper.writeValueAsString(
                    BulkUpdateResultRequest(
                        listOf(
                            UpdateResultRequest(
                                orderNumber = orderNumber,
                                result = Result.NEGATIVE
                            ),
                            UpdateResultRequest(
                                orderNumber = orderNumber + "1",
                                result = Result.NEGATIVE
                            ),
                            UpdateResultRequest(
                                orderNumber = orderNumber + "2",
                                result = Result.POSITIVE
                            ),
                            UpdateResultRequest(
                                orderNumber = orderNumber + "3",
                                result = Result.POSITIVE
                            )
                        )
                    )
                )
            }

            verify(exactly = 1) { bulkUpdateResultsUseCase(any(), any(), any()) }
        }

        @Test
        fun `bulk uploading status of a list of pre issued orders returns status 401 for an invalid labId header`() {
            mockMvc.put("/v1/results/bulk") {
                header(HttpHeaders.AUTHORIZATION, "wrong")
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    BulkUpdateResultRequest(
                        listOf(
                            UpdateResultRequest(
                                orderNumber = "invalid",
                                result = Result.NEGATIVE
                            )
                        )
                    )
                )
            }.andExpect { status { isUnauthorized } }
        }

        @Test
        fun `bulk uploading status of a list of pre issued orders returns status 400 for an invalid order number`() {
            every { bulkUpdateResultsUseCase(any(), any(), any()) } returns listOf(
                BulkUpdateResultsUseCase.BulkUpdateError(
                    message = "something broke"
                )
            )

            mockMvc.put("/v1/results/bulk") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    BulkUpdateResultRequest(
                        listOf(
                            UpdateResultRequest(
                                orderNumber = "invalid",
                                result = Result.NEGATIVE
                            )
                        )
                    )
                )
            }.andExpect { status { isBadRequest } }
        }

        @Test
        fun `bulk uploading status of a list of externally issued orders returns an error for an invalid order number`() {
            every { bulkUpdateResultsUseCase(any(), any(), any()) } returns listOf(
                BulkUpdateResultsUseCase.BulkUpdateError(
                    message = "something broke"
                )
            )

            mockMvc.put("/v1/results/bulk") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    BulkUpdateResultRequest(
                        listOf(
                            UpdateResultRequest(
                                orderNumber = "invalid",
                                result = Result.NEGATIVE
                            ),
                            UpdateResultRequest(
                                orderNumber = orderNumber,
                                result = Result.NEGATIVE
                            )
                        )
                    )
                )
            }.andExpect { jsonPath("$.bulkUploadErrors.length()", Is.`is`(1)) }
        }

        @Test
        fun `bulk uploading status of a list of orders returns multiple errors`() {
            every { bulkUpdateResultsUseCase(any(), any(), any()) } returns listOf(
                BulkUpdateResultsUseCase.BulkUpdateError(message = "something broke"),
                BulkUpdateResultsUseCase.BulkUpdateError(message = "another thing broke")
            )

            mockMvc.put("/v1/results/bulk") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    BulkUpdateResultRequest(
                        listOf(
                            UpdateResultRequest(
                                orderNumber = orderNumber,
                                result = Result.NEGATIVE
                            ),
                            UpdateResultRequest(
                                orderNumber = "9876543210",
                                result = Result.NEGATIVE
                            )
                        )
                    )
                )
            }.andExpect { jsonPath("$.bulkUploadErrors.length()", Is.`is`(2)) }
        }
    }
}
