package com.healthmetrix.labres.lab

import com.fasterxml.jackson.databind.ObjectMapper
import com.healthmetrix.labres.LabResTestApplication
import com.healthmetrix.labres.encodeBase64
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.persistence.OrderInformation
import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.core.Is
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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
    private lateinit var bulkUpdateResultsUseCase: BulkUpdateResultsUseCase

    private val labId = "test-lab"
    private val issuerId = "test-issuer"
    private val orderNumber = "0123456789"
    private val labIdHeader = "Basic ${"$labId:pass".encodeBase64()}"

    @Nested
    inner class JSON {

        @Test
        fun `uploading a valid json request body with an external order number returns 200`() {
            every { updateResultUseCase(any(), any()) } returns mockk()

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
        fun `uploading a valid json request body with an external order number calls the updateResultsUseCase`() {
            every { updateResultUseCase(any(), any()) } returns mockk<OrderInformation>()

            mockMvc.put("/v1/results/json") {
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
                    labResult = match {
                        it.labId == labId &&
                            it.orderNumber == OrderNumber.External.from(orderNumber) &&
                            it.result == Result.NEGATIVE
                    },
                    now = any()
                )
            }
        }

        @Test
        fun `uploading a valid json request body with a preissued order number returns 200`() {
            every { updateResultUseCase(any(), any()) } returns mockk()

            mockMvc.put("/v1/results/json") {
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
            every { updateResultUseCase(any(), any()) } returns mockk<OrderInformation>()

            mockMvc.put("/v1/results/json") {
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
                    labResult = match {
                        it.labId == labId &&
                            it.orderNumber == OrderNumber.PreIssued(issuerId, orderNumber) &&
                            it.result == Result.NEGATIVE
                    },
                    now = any()
                )
            }
        }

        @Test
        fun `upload a document to an unknown order number returns 404`() {
            every { updateResultUseCase(any(), any()) } returns null

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
        fun `upload a document to an invalid order number returns 400`() {
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
        fun `upload document with invalid auth header returns 401`(labHeaderValue: String) {
            mockMvc.put("/v1/results/json") {
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
        fun `uploading a document with the optional test type returns 200`() {
            every { updateResultUseCase(any(), any()) } returns mockk()

            mockMvc.put("/v1/results/json") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "orderNumber" to "0123456789",
                        "result" to Status.NEGATIVE,
                        "type" to "94500-6"
                    )
                )
            }.andExpect {
                status { isOk }
            }
        }

        @Test
        fun `uploading a document with the optional test type calls the updateStateUseCase`() {
            every { updateResultUseCase(any(), any()) } returns mockk()

            val testType = "94500-6"

            mockMvc.put("/v1/results/json") {
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
                    labResult = match {
                        it.labId == labId &&
                            it.orderNumber == OrderNumber.External.from(orderNumber) &&
                            it.result == Result.NEGATIVE &&
                            it.testType == testType
                    },
                    now = any()
                )
            }
        }

        @Test
        fun `bulk uploading status of a list of externally issued orders returns status 200`() {
            every { bulkUpdateResultsUseCase(any(), any(), any()) } returns listOf()

            val anotherOrderNumber = "9876543210"

            mockMvc.put("/v1/results/bulk") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    BulkUpdateStatusRequest(
                        listOf(
                            JsonResult(
                                orderNumber = orderNumber,
                                result = Result.NEGATIVE
                            ),
                            JsonResult(
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
                    BulkUpdateStatusRequest(
                        listOf(
                            JsonResult(
                                orderNumber = orderNumber,
                                result = Result.NEGATIVE
                            ),
                            JsonResult(
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
                param("issuerId", "leKevin")
                content = objectMapper.writeValueAsString(
                    BulkUpdateStatusRequest(
                        listOf(
                            JsonResult(
                                orderNumber = orderNumber,
                                result = Result.NEGATIVE
                            ),
                            JsonResult(
                                orderNumber = orderNumber + "1",
                                result = Result.NEGATIVE
                            ),
                            JsonResult(
                                orderNumber = orderNumber + "2",
                                result = Result.POSITIVE
                            ),
                            JsonResult(
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
                param("issuerId", "leKevin")
                content = objectMapper.writeValueAsString(
                    BulkUpdateStatusRequest(
                        listOf(
                            JsonResult(
                                orderNumber = orderNumber,
                                result = Result.NEGATIVE
                            ),
                            JsonResult(
                                orderNumber = orderNumber + "1",
                                result = Result.NEGATIVE
                            ),
                            JsonResult(
                                orderNumber = orderNumber + "2",
                                result = Result.POSITIVE
                            ),
                            JsonResult(
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
                    BulkUpdateStatusRequest(
                        listOf(
                            JsonResult(
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
            every { bulkUpdateResultsUseCase(any(), any(), any()) } returns listOf(BulkUploadError(message = "something broke"))

            mockMvc.put("/v1/results/bulk") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    BulkUpdateStatusRequest(
                        listOf(
                            JsonResult(
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
            every { bulkUpdateResultsUseCase(any(), any(), any()) } returns listOf(BulkUploadError(message = "something broke"))

            mockMvc.put("/v1/results/bulk") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    BulkUpdateStatusRequest(
                        listOf(
                            JsonResult(
                                orderNumber = "invalid",
                                result = Result.NEGATIVE
                            ),
                            JsonResult(
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
                BulkUploadError(message = "something broke"),
                BulkUploadError(message = "another thing broke")
            )

            mockMvc.put("/v1/results/bulk") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    BulkUpdateStatusRequest(
                        listOf(
                            JsonResult(
                                orderNumber = orderNumber,
                                result = Result.NEGATIVE
                            ),
                            JsonResult(
                                orderNumber = "9876543210",
                                result = Result.NEGATIVE
                            )
                        )
                    )
                )
            }.andExpect { jsonPath("$.bulkUploadErrors.length()", Is.`is`(2)) }
        }
    }

    @Nested
    inner class KEVB_CSV {

        private val kevbCsv = MediaType.valueOf(KEVB_CSV_VALUE)

        @Test
        fun `uploading a valid kevb csv request body with an external order number returns 200`() {
            every { updateResultUseCase(any(), any()) } returns mockk()

            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = kevbCsv
                content = "$orderNumber,${Status.NEGATIVE}"
            }.andExpect {
                status { isOk }
            }
        }

        @Test
        fun `uploading a valid kevb csv request body with an external order number calls the updateResultsUseCase`() {
            every { updateResultUseCase(any(), any()) } returns mockk<OrderInformation>()

            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = kevbCsv
                content = "$orderNumber,${Status.NEGATIVE}"
            }

            verify {
                updateResultUseCase.invoke(
                    labResult = match {
                        it.labId == labId &&
                            it.orderNumber == OrderNumber.External.from(orderNumber) &&
                            it.result == Result.NEGATIVE
                    },
                    now = any()
                )
            }
        }

        @Test
        fun `uploading a valid kevb csv request body with a preissued order number returns 200`() {
            every { updateResultUseCase(any(), any()) } returns mockk()

            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                param("issuerId", issuerId)
                contentType = kevbCsv
                content = "$orderNumber,${Status.NEGATIVE}"
            }.andExpect {
                status { isOk }
            }
        }

        @Test
        fun `uploading a valid kevb csv request body with a preissued order number calls the updateResultsUseCase`() {
            every { updateResultUseCase(any(), any()) } returns mockk<OrderInformation>()

            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                param("issuerId", issuerId)
                contentType = kevbCsv
                content = "$orderNumber,${Status.NEGATIVE}"
            }

            verify {
                updateResultUseCase.invoke(
                    labResult = match {
                        it.labId == labId &&
                            it.orderNumber == OrderNumber.PreIssued(issuerId, orderNumber) &&
                            it.result == Result.NEGATIVE
                    },
                    now = any()
                )
            }
        }

        @Test
        fun `upload a document to an unknown order number returns 404`() {
            every { updateResultUseCase(any(), any()) } returns null

            mockMvc.put("/v1/results") {
                contentType = kevbCsv
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                content = "$orderNumber,${Status.POSITIVE}"
            }.andExpect {
                status { isNotFound }
            }
        }

        @Test
        fun `upload a document to an invalid order number returns 400`() {
            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = kevbCsv
                content = "nonsense,${Status.POSITIVE}"
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
        fun `upload document with invalid auth header returns 401`(labHeaderValue: String) {
            mockMvc.put("/v1/results") {
                contentType = kevbCsv
                header(HttpHeaders.AUTHORIZATION, labHeaderValue)
                content = "$orderNumber,${Status.POSITIVE}"
            }.andExpect {
                status { isUnauthorized }
            }
        }

        @Test
        fun `uploading a document with the optional test type returns 200`() {
            every { updateResultUseCase(any(), any()) } returns mockk()

            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = kevbCsv
                content = "$orderNumber,${Status.NEGATIVE},94500-6"
            }.andExpect {
                status { isOk }
            }
        }

        @Test
        fun `uploading a document with the optional test type calls the updateStateUseCase`() {
            every { updateResultUseCase(any(), any()) } returns mockk()

            val testType = "94500-6"

            mockMvc.put("/v1/results") {
                header(HttpHeaders.AUTHORIZATION, labIdHeader)
                contentType = kevbCsv
                content = "$orderNumber,${Status.NEGATIVE},$testType"
            }

            verify {
                updateResultUseCase.invoke(
                    labResult = match {
                        it.labId == labId &&
                            it.orderNumber == OrderNumber.External.from(orderNumber) &&
                            it.result == Result.NEGATIVE &&
                            it.testType == testType
                    },
                    now = any()
                )
            }
        }
    }
}
