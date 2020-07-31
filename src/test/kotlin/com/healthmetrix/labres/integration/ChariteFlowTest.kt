package com.healthmetrix.labres.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.healthmetrix.labres.LabResApplication
import com.healthmetrix.labres.encodeBase64
import com.healthmetrix.labres.lab.Result
import com.healthmetrix.labres.lab.TestType
import com.healthmetrix.labres.lab.UpdateResultRequest
import com.healthmetrix.labres.notifications.Notification
import com.healthmetrix.labres.notifications.Notifier
import com.healthmetrix.labres.order.ExternalOrderNumberController.IssueExternalOrderNumberResponse
import com.healthmetrix.labres.order.Sample
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.order.StatusResponse
import com.healthmetrix.labres.persistence.InMemoryOrderInformationRepository
import com.healthmetrix.labres.persistence.OrderInformationRepository
import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
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
class ChariteFlowTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var repository: OrderInformationRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @SpykBean
    private lateinit var httpNotifier: Notifier<Notification.HttpNotification>

    @BeforeEach
    internal fun setUp() {
        (repository as InMemoryOrderInformationRepository).clear()
        clearMocks(httpNotifier)
    }

    private val labId = "test_lab"
    private val labIdHeader = "$labId:pass".encodeBase64()
    private val notificationUrl = "http://notify.test"
    private val sampledAt = 1596186947L

    @Nested
    inner class SalivaImplicit {
        private val sample = Sample.SALIVA
        private val testType = TestType.PCR

        @Test
        fun `an orderInformation can be created with a notification url`() {
            val registeredResponse = registerOrderWithNotificationUrl(notificationUrl, null)

            val result = repository.findById(registeredResponse.id)

            assertThat(result).isNotNull
            assertThat(result!!).matches { order ->
                order.notificationUrls == listOf(notificationUrl) &&
                    order.status == Status.IN_PROGRESS &&
                    order.sample == sample
            }
        }

        @Test
        fun `a lab result can be successfully created, fetched and a result can be uploaded`() {
            // register
            val registeredResponse = registerOrderWithNotificationUrl(notificationUrl, null)
            val orderId = registeredResponse.id
            val orderNumber = registeredResponse.orderNumber

            // query result while IN_PROGRESS
            var queryResult = mockMvc.get("/v1/orders/$orderId")
                .andExpect { jsonPath("$.sampledAt") { doesNotExist() } }
                .andReturn().responseBody<StatusResponse.Found>()

            assertThat(queryResult.status).isEqualTo(Status.IN_PROGRESS)

            // update result
            updateResultFor(orderNumber, null, sampledAt)

            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.status).isEqualTo(Status.POSITIVE)

            verify(exactly = 1) { httpNotifier.send(match { it.url == notificationUrl }) }

            // query result
            queryResult = mockMvc.get("/v1/orders/$orderId")
                .andReturn().responseBody<StatusResponse.Found>()

            assertThat(queryResult.sampledAt).isEqualTo(sampledAt)
            assertThat(queryResult.status).isEqualTo(Status.POSITIVE)
        }

        @Test
        fun `updating results sets labId, sampledAt and testType`() {
            val createResponse = registerOrderWithNotificationUrl("http://before.test", null)

            val orderId = createResponse.id
            val orderNumber = createResponse.orderNumber
            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.labId).isNull()
            assertThat(orderInformation.testType).isNull()
            assertThat(orderInformation.sampledAt).isNull()

            updateResultFor(orderNumber, null, sampledAt)

            val updatedOrderInformation = repository.findById(orderId)
            assertThat(updatedOrderInformation).isNotNull
            assertThat(updatedOrderInformation!!).matches {
                it.labId == labId && it.testType == testType && it.sampledAt == sampledAt
            }
        }
    }

    @Nested
    inner class Saliva {
        private val sample = Sample.SALIVA
        private val testType = TestType.PCR

        @Test
        fun `an orderInformation can be created with a notification url`() {
            val registeredResponse = registerOrderWithNotificationUrl(notificationUrl, sample)

            val result = repository.findById(registeredResponse.id)

            assertThat(result).isNotNull
            assertThat(result!!).matches { order ->
                order.notificationUrls == listOf(notificationUrl) &&
                    order.status == Status.IN_PROGRESS &&
                    order.sample == sample
            }
        }

        @Test
        fun `a lab result can be successfully created, fetched and a result can be uploaded`() {
            // register
            val registeredResponse = registerOrderWithNotificationUrl(notificationUrl, sample)
            val orderId = registeredResponse.id
            val orderNumber = registeredResponse.orderNumber

            // query result while IN_PROGRESS
            var queryResult = mockMvc.get("/v1/orders/$orderId")
                .andExpect { jsonPath("$.sampledAt") { doesNotExist() } }
                .andReturn().responseBody<StatusResponse.Found>()

            assertThat(queryResult.status).isEqualTo(Status.IN_PROGRESS)

            // update result
            updateResultFor(orderNumber, testType, sampledAt)

            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.status).isEqualTo(Status.POSITIVE)

            verify(exactly = 1) { httpNotifier.send(match { it.url == notificationUrl }) }

            // query result
            queryResult = mockMvc.get("/v1/orders/$orderId")
                .andReturn().responseBody<StatusResponse.Found>()

            assertThat(queryResult.sampledAt).isEqualTo(sampledAt)
            assertThat(queryResult.status).isEqualTo(Status.POSITIVE)
        }

        @Test
        fun `updating results sets labId and testType`() {
            val createResponse = registerOrderWithNotificationUrl("http://before.test", sample)

            val orderId = createResponse.id
            val orderNumber = createResponse.orderNumber
            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.labId).isNull()
            assertThat(orderInformation.testType).isNull()
            assertThat(orderInformation.sampledAt).isNull()

            updateResultFor(orderNumber, testType, sampledAt)

            val updatedOrderInformation = repository.findById(orderId)
            assertThat(updatedOrderInformation).isNotNull
            assertThat(updatedOrderInformation!!).matches {
                it.labId == labId && it.testType == testType && it.sampledAt == sampledAt
            }
        }
    }

    @Nested
    inner class Blood {
        private val sample = Sample.BLOOD
        private val testType = TestType.ANTIBODY

        @Test
        fun `an orderInformation can be created with a notification url`() {
            val registeredResponse = registerOrderWithNotificationUrl(notificationUrl, sample)

            val result = repository.findById(registeredResponse.id)

            assertThat(result).isNotNull
            assertThat(result!!).matches { order ->
                order.notificationUrls == listOf(notificationUrl) &&
                    order.status == Status.IN_PROGRESS &&
                    order.sample == sample
            }
        }

        @Test
        fun `a lab result can be successfully created, fetched and a result can be uploaded`() {
            // register
            val registeredResponse = registerOrderWithNotificationUrl(notificationUrl, sample)
            val orderId = registeredResponse.id
            val orderNumber = registeredResponse.orderNumber

            // query result while IN_PROGRESS
            var queryResult = mockMvc.get("/v1/orders/$orderId")
                .andExpect { jsonPath("$.sampledAt") { doesNotExist() } }
                .andReturn().responseBody<StatusResponse.Found>()

            assertThat(queryResult.status).isEqualTo(Status.IN_PROGRESS)

            // update result
            updateResultFor(orderNumber, testType, sampledAt)

            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.status).isEqualTo(Status.POSITIVE)

            verify(exactly = 1) { httpNotifier.send(match { it.url == notificationUrl }) }

            // query result
            queryResult = mockMvc.get("/v1/orders/$orderId")
                .andReturn().responseBody<StatusResponse.Found>()

            assertThat(queryResult.sampledAt).isEqualTo(sampledAt)
            assertThat(queryResult.status).isEqualTo(Status.POSITIVE)
        }

        @Test
        fun `updating results sets labId and testType`() {
            val createResponse = registerOrderWithNotificationUrl("http://before.test", sample)

            val orderId = createResponse.id
            val orderNumber = createResponse.orderNumber
            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.labId).isNull()
            assertThat(orderInformation.testType).isNull()
            assertThat(orderInformation.sampledAt).isNull()

            updateResultFor(orderNumber, testType, sampledAt)

            val updatedOrderInformation = repository.findById(orderId)
            assertThat(updatedOrderInformation).isNotNull
            assertThat(updatedOrderInformation!!).matches {
                it.labId == labId && it.testType == testType && sampledAt == sampledAt
            }
        }
    }

    private fun registerOrderWithNotificationUrl(url: String? = notificationUrl, sample: Sample?) = mockMvc.post("/v1/orders") {
        contentType = MediaType.APPLICATION_JSON
        val body = mutableMapOf(
            "notificationUrl" to url
        )

        if (sample != null)
            body["sample"] = sample.toString()

        content = objectMapper.writeValueAsBytes(body)
    }.andReturn().responseBody<IssueExternalOrderNumberResponse.Created>()

    private fun updateResultFor(orderNumber: String, testType: TestType?, sampledAt: Long? = null) =
        mockMvc.put("/v1/results") {
            contentType = MediaType.APPLICATION_JSON
            headers { setBasicAuth(labIdHeader) }
            var body = UpdateResultRequest(
                orderNumber = orderNumber,
                result = Result.POSITIVE
            )

            if (testType != null)
                body = body.copy(type = testType)

            if (sampledAt != null)
                body = body.copy(sampledAt = sampledAt)

            content = objectMapper.writeValueAsBytes(body)
        }.andExpect { status { isOk } }

    private inline fun <reified T> MvcResult.responseBody(): T {
        return objectMapper.readValue(response.contentAsString)
    }
}
