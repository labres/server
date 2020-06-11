package com.healthmetrix.labres.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.healthmetrix.labres.LabResApplication
import com.healthmetrix.labres.encodeBase64
import com.healthmetrix.labres.lab.APPLICATION_KEVB_CSV
import com.healthmetrix.labres.lab.BulkUpdateResultRequest
import com.healthmetrix.labres.lab.KevbLabResultMessageConverter
import com.healthmetrix.labres.lab.Result
import com.healthmetrix.labres.lab.UpdateResultRequest
import com.healthmetrix.labres.order.ExternalOrderNumberController.IssueExternalOrderNumberResponse
import com.healthmetrix.labres.order.PreIssuedOrderNumberController
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.order.StatusResponse
import com.healthmetrix.labres.persistence.InMemoryOrderInformationRepository
import com.healthmetrix.labres.persistence.OrderInformationRepository
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.core.Is
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest(
    classes = [LabResApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
class LabResultsTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var repository: OrderInformationRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var kevbLabResultMessageConverter: KevbLabResultMessageConverter

    @BeforeEach
    internal fun setUp() {
        (repository as InMemoryOrderInformationRepository).clear()
    }

    private val labName = "test_lab"
    private val labIdHeader = "$labName:pass".encodeBase64()
    private val orderNumberString = "1234567890"
    private val issuerId = "test_issuer"
    private val testSiteId = "testSiteA"
    private val notificationUrl = "http://callme.test"

    @Nested
    inner class EON {
        @Test
        fun `an orderInformation has IN_PROGRESS when first created`() {
            val createResponse = mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
            }.andReturn().responseBody<IssueExternalOrderNumberResponse.Created>()

            val orderInformation = repository.findById(createResponse.id)!!
            assertThat(orderInformation.status).isEqualTo(Status.IN_PROGRESS)
        }

        @Test
        fun `an orderInformation can be created with a notification url`() {
            val createResponse = mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "notificationUrl" to "abc123"
                    )
                )
            }.andReturn().responseBody<IssueExternalOrderNumberResponse.Created>()
            val orderInformation = repository.findById(createResponse.id)!!
            assertThat(orderInformation.notificationUrl).isEqualTo("abc123")
        }

        @Test
        fun `a lab result can be successfully created, fetched, and updated`() {
            val createResponse = mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
            }.andReturn().responseBody<IssueExternalOrderNumberResponse.Created>()

            val orderId = createResponse.id
            mockMvc.get("/v1/orders/$orderId")
                .andReturn().responseBody<StatusResponse.Found>()

            val orderNumber = createResponse.orderNumber
            mockMvc.put("/v1/results") {
                contentType = MediaType.APPLICATION_JSON
                headers { setBasicAuth(labIdHeader) }
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "orderNumber" to orderNumber,
                        "result" to "POSITIVE"
                    )
                )
            }.andExpect { status { isOk } }

            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.status).isEqualTo(Status.POSITIVE)
        }

        @Test
        fun `updating results works also with kevb+csv as content type`() {
            val createResponse = mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "notificationUrl" to "beforeId"
                    )
                )
            }.andReturn().responseBody<IssueExternalOrderNumberResponse.Created>()

            val orderId = createResponse.id
            val orderNumber = createResponse.orderNumber
            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.labId).isNull()

            mockMvc.put("/v1/results") {
                contentType = APPLICATION_KEVB_CSV
                headers { setBasicAuth(labIdHeader) }
                content = "$orderNumber,${Result.POSITIVE}"
            }

            val updatedOrderInformation = repository.findById(orderId)!!
            assertThat(updatedOrderInformation.labId).isEqualTo(labName)
        }

        @Test
        fun `updating results sets the lab id`() {
            val createResponse = mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "notificationUrl" to "beforeId"
                    )
                )
            }.andReturn().responseBody<IssueExternalOrderNumberResponse.Created>()

            val orderId = createResponse.id
            val orderNumber = createResponse.orderNumber
            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.labId).isNull()

            mockMvc.put("/v1/results") {
                contentType = MediaType.APPLICATION_JSON
                headers { setBasicAuth(labIdHeader) }
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "orderNumber" to orderNumber,
                        "result" to "POSITIVE"
                    )
                )
            }

            val updatedOrderInformation = repository.findById(orderId)!!
            assertThat(updatedOrderInformation.labId).isEqualTo(labName)
        }

        @Test
        fun `bulk upload works`() {
            val responses = (0 until 3).map {
                mockMvc.post("/v1/orders") {
                    contentType = MediaType.APPLICATION_JSON
                }.andReturn().responseBody<IssueExternalOrderNumberResponse.Created>()
            }

            mockMvc.put("/v1/results/bulk") {
                contentType = MediaType.APPLICATION_JSON
                headers { setBasicAuth(labIdHeader) }
                content = objectMapper.writeValueAsBytes(
                    BulkUpdateResultRequest(
                        responses.map { res ->
                            UpdateResultRequest(
                                orderNumber = res.orderNumber,
                                result = Result.POSITIVE
                            )
                        }
                    )
                )
            }.andExpect {
                jsonPath("$.processedRows", Is.`is`(3))
            }

            responses.forEach { res ->
                val orderInfo = repository.findById(res.id)
                assertThat(orderInfo?.status).isEqualTo(Status.POSITIVE)
            }
        }
    }

    @Nested
    inner class PON {
        @Test
        fun `an orderInformation has IN_PROGRESS when first registered`() {
            val request = PreIssuedOrderNumberController.RegisterOrderRequest(
                orderNumber = orderNumberString,
                testSiteId = null,
                notificationUrl = null
            )

            val response = mockMvc.post("/v1/issuers/$issuerId/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andReturn().responseBody<PreIssuedOrderNumberController.RegisterOrderResponse.Created>()

            val orderInformation = repository.findById(response.id)!!
            assertThat(orderInformation.status).isEqualTo(Status.IN_PROGRESS)
        }

        @Test
        fun `an orderInformation can be created with notification url and testSiteId`() {
            val request = PreIssuedOrderNumberController.RegisterOrderRequest(
                orderNumber = orderNumberString,
                testSiteId = testSiteId,
                notificationUrl = notificationUrl
            )

            val createResponse = mockMvc.post("/v1/issuers/$issuerId/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsBytes(request)
            }.andReturn().responseBody<PreIssuedOrderNumberController.RegisterOrderResponse.Created>()

            val orderInformation = repository.findById(createResponse.id)!!
            assertThat(orderInformation.notificationUrl).isEqualTo(notificationUrl)
            assertThat(orderInformation.testSiteId).isEqualTo(testSiteId)
        }

        @Test
        fun `a lab result can be successfully created, fetched, and updated`() {
            val request = PreIssuedOrderNumberController.RegisterOrderRequest(
                orderNumber = orderNumberString,
                testSiteId = testSiteId,
                notificationUrl = notificationUrl
            )

            val response = mockMvc.post("/v1/issuers/$issuerId/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andReturn().responseBody<PreIssuedOrderNumberController.RegisterOrderResponse.Created>()

            val orderId = response.id
            mockMvc.get("/v1/issuers/$issuerId/orders/$orderId")
                .andReturn().responseBody<StatusResponse.Found>()

            val orderNumber = response.orderNumber
            mockMvc.put("/v1/results") {
                contentType = MediaType.APPLICATION_JSON
                headers { setBasicAuth(labIdHeader) }
                param("issuerId", issuerId)
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "orderNumber" to orderNumber,
                        "result" to "POSITIVE"
                    )
                )
            }

            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.status).isEqualTo(Status.POSITIVE)
        }

        @Test
        fun `updating results works also with kevb+csv as content type`() {
            val createResponse = mockMvc
                .post("/v1/issuers/$issuerId/orders") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsBytes(
                        mapOf(
                            "orderNumber" to orderNumberString,
                            "notificationUrl" to "beforeId"
                        )
                    )
                }.andReturn().responseBody<IssueExternalOrderNumberResponse.Created>()

            val orderId = createResponse.id
            val orderNumber = createResponse.orderNumber
            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.labId).isNull()

            mockMvc.put("/v1/results") {
                contentType = APPLICATION_KEVB_CSV
                headers { setBasicAuth(labIdHeader) }
                param("issuerId", issuerId)
                content = "$orderNumber,${Result.POSITIVE}"
            }

            val updatedOrderInformation = repository.findById(orderId)!!
            assertThat(updatedOrderInformation.labId).isEqualTo(labName)
        }

        @Test
        fun `updating results sets the lab id`() {
            val createResponse = mockMvc
                .post("/v1/issuers/$issuerId/orders") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsBytes(
                        mapOf(
                            "orderNumber" to orderNumberString
                        )
                    )
                }.andReturn().responseBody<IssueExternalOrderNumberResponse.Created>()

            val orderId = createResponse.id
            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.labId).isNull()

            mockMvc.put("/v1/results") {
                contentType = MediaType.APPLICATION_JSON
                headers { setBasicAuth(labIdHeader) }
                param("issuerId", issuerId)
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "orderNumber" to orderNumberString,
                        "result" to "POSITIVE"
                    )
                )
            }

            val updatedOrderInformation = repository.findById(orderId)!!
            assertThat(updatedOrderInformation.labId).isEqualTo(labName)
        }

        @Test
        fun `bulk upload works`() {
            val request1 = PreIssuedOrderNumberController.RegisterOrderRequest(
                orderNumber = orderNumberString,
                testSiteId = testSiteId,
                notificationUrl = notificationUrl
            )

            val request2 = PreIssuedOrderNumberController.RegisterOrderRequest(
                orderNumber = "orderNumber2",
                testSiteId = testSiteId,
                notificationUrl = notificationUrl
            )

            val request3 = PreIssuedOrderNumberController.RegisterOrderRequest(
                orderNumber = "orderNumber3",
                testSiteId = testSiteId,
                notificationUrl = notificationUrl
            )

            val responses = listOf(request1, request2, request3).map { req ->
                mockMvc.post("/v1/issuers/$issuerId/orders") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(req)
                }.andReturn().responseBody<PreIssuedOrderNumberController.RegisterOrderResponse.Created>()
            }

            mockMvc.put("/v1/results/bulk") {
                contentType = MediaType.APPLICATION_JSON
                headers { setBasicAuth(labIdHeader) }
                param("issuerId", issuerId)
                content = objectMapper.writeValueAsBytes(
                    BulkUpdateResultRequest(
                        responses.map { res ->
                            UpdateResultRequest(
                                orderNumber = res.orderNumber,
                                result = Result.POSITIVE
                            )
                        }
                    )
                )
            }.andExpect {
                jsonPath("$.processedRows", Is.`is`(3))
            }

            responses.forEach { res ->
                val orderInfo = repository.findById(res.id)
                assertThat(orderInfo?.status).isEqualTo(Status.POSITIVE)
            }
        }
    }

    private inline fun <reified T> MvcResult.responseBody(): T {
        return objectMapper.readValue(response.contentAsString)
    }
}
