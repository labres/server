package com.healthmetrix.labres.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.healthmetrix.labres.LabResApplication
import com.healthmetrix.labres.encodeBase64
import com.healthmetrix.labres.lab.Result
import com.healthmetrix.labres.lab.TestType
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
            val registeredResponse = registerOrderWithNotificationUrl(notificationUrl, null)

            val orderId = registeredResponse.id
            mockMvc.get("/v1/orders/$orderId")
                .andReturn().responseBody<StatusResponse.Found>()

            val orderNumber = registeredResponse.orderNumber
            updateResultFor(orderNumber, null)

            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.status).isEqualTo(Status.POSITIVE)

            verify(exactly = 1) { httpNotifier.send(match { it.url == notificationUrl }) }
        }

        @Test
        fun `updating results sets labId and testType`() {
            val createResponse = registerOrderWithNotificationUrl("http://before.test", null)

            val orderId = createResponse.id
            val orderNumber = createResponse.orderNumber
            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.labId).isNull()
            assertThat(orderInformation.testType).isNull()

            updateResultFor(orderNumber, null)

            val updatedOrderInformation = repository.findById(orderId)
            assertThat(updatedOrderInformation).isNotNull
            assertThat(updatedOrderInformation!!).matches {
                it.labId == labId && it.testType == testType
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
            val registeredResponse = registerOrderWithNotificationUrl(notificationUrl, sample)

            val orderId = registeredResponse.id
            mockMvc.get("/v1/orders/$orderId")
                .andReturn().responseBody<StatusResponse.Found>()

            val orderNumber = registeredResponse.orderNumber
            updateResultFor(orderNumber, testType)

            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.status).isEqualTo(Status.POSITIVE)

            verify(exactly = 1) { httpNotifier.send(match { it.url == notificationUrl }) }
        }

        @Test
        fun `updating results sets labId and testType`() {
            val createResponse = registerOrderWithNotificationUrl("http://before.test", sample)

            val orderId = createResponse.id
            val orderNumber = createResponse.orderNumber
            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.labId).isNull()
            assertThat(orderInformation.testType).isNull()

            updateResultFor(orderNumber, testType)

            val updatedOrderInformation = repository.findById(orderId)
            assertThat(updatedOrderInformation).isNotNull
            assertThat(updatedOrderInformation!!).matches {
                it.labId == labId && it.testType == testType
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
            val registeredResponse = registerOrderWithNotificationUrl(notificationUrl, sample)

            val orderId = registeredResponse.id
            mockMvc.get("/v1/orders/$orderId")
                .andReturn().responseBody<StatusResponse.Found>()

            val orderNumber = registeredResponse.orderNumber
            updateResultFor(orderNumber, testType)

            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.status).isEqualTo(Status.POSITIVE)

            verify(exactly = 1) { httpNotifier.send(match { it.url == notificationUrl }) }
        }

        @Test
        fun `updating results sets labId and testType`() {
            val createResponse = registerOrderWithNotificationUrl("http://before.test", sample)

            val orderId = createResponse.id
            val orderNumber = createResponse.orderNumber
            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.labId).isNull()
            assertThat(orderInformation.testType).isNull()

            updateResultFor(orderNumber, testType)

            val updatedOrderInformation = repository.findById(orderId)
            assertThat(updatedOrderInformation).isNotNull
            assertThat(updatedOrderInformation!!).matches {
                it.labId == labId && it.testType == testType
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

    private fun updateResultFor(orderNumber: String, testType: TestType?) =
        mockMvc.put("/v1/results") {
            contentType = MediaType.APPLICATION_JSON
            headers { setBasicAuth(labIdHeader) }
            val body = mutableMapOf(
                "orderNumber" to orderNumber,
                "result" to Result.POSITIVE.toString()
            )

            if (testType != null)
                body["type"] = testType.toString()

            content = objectMapper.writeValueAsBytes(body)
        }.andExpect { status { isOk } }

    private inline fun <reified T> MvcResult.responseBody(): T {
        return objectMapper.readValue(response.contentAsString)
    }
}
