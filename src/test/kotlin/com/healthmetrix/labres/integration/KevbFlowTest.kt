package com.healthmetrix.labres.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.healthmetrix.labres.LabResApplication
import com.healthmetrix.labres.encodeBase64
import com.healthmetrix.labres.lab.APPLICATION_KEVB_CSV
import com.healthmetrix.labres.lab.Result
import com.healthmetrix.labres.lab.TestType
import com.healthmetrix.labres.notifications.Notification
import com.healthmetrix.labres.notifications.Notifier
import com.healthmetrix.labres.order.PreIssuedOrderNumberController
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
import java.util.UUID

@SpringBootTest(
    classes = [LabResApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
class KevbFlowTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var repository: OrderInformationRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @SpykBean
    private lateinit var fcmNotifier: Notifier<Notification.FcmNotification>

    @BeforeEach
    internal fun setUp() {
        (repository as InMemoryOrderInformationRepository).clear()
        clearMocks(fcmNotifier)
    }

    private val labId = "test_lab"
    private val labIdHeader = "$labId:pass".encodeBase64()
    private val issuerId = "test_issuer"
    private val fcmToken = "test"
    private val notificationUrl = "fcm://labres@$fcmToken"
    private val orderNumber = "1234567890"

    @Nested
    inner class Saliva {

        private val sample = Sample.SALIVA
        private val testType = TestType.PCR

        @Test
        fun `an order can be registered`() {
            val registeredResponse = registerOrder(sample = sample)

            val result = repository.findById(registeredResponse.id)

            assertThat(result).isNotNull
            assertThat(result!!).matches { it.status == Status.IN_PROGRESS && it.sample == sample }
        }

        @Test
        fun `an order with analyt can be registered`() {
            val registeredResponse = registerOrder(orderNumber + "16", sample)

            val result = repository.findById(registeredResponse.id)

            assertThat(result).isNotNull
            assertThat(result!!).matches { it.status == Status.IN_PROGRESS && it.sample == sample }
        }

        @Test
        fun `a lab result can be successfully created, fetched, updated and a result can be uploaded`() {
            // register
            val registeredResponse = registerOrder(sample = sample, notificationUrl = notificationUrl)
            val orderId = registeredResponse.id

            // query result while IN_PROGRESS
            var queryResult = mockMvc.get("/v1/issuers/$issuerId/orders/$orderId")
                .andExpect { jsonPath("$.sampledAt") { doesNotExist() } }
                .andReturn().responseBody<StatusResponse.Found>()

            assertThat(queryResult.status).isEqualTo(Status.IN_PROGRESS)

            // update result
            uploadResult(testType)
            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.status).isEqualTo(Status.POSITIVE)

            verify(exactly = 1) { fcmNotifier.send(match { it.token == fcmToken }) }

            // query result
            queryResult = mockMvc.get("/v1/issuers/$issuerId/orders/$orderId")
                .andExpect { jsonPath("$.sampledAt") { doesNotExist() } }
                .andReturn().responseBody<StatusResponse.Found>()

            assertThat(queryResult.status).isEqualTo(Status.POSITIVE)
        }

        @Test
        fun `the notificationUrl can be updated`() {
            // register
            val registeredResponse = registerOrder(sample = sample)
            val orderId = registeredResponse.id

            var orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.notificationUrls).isEmpty()

            // set notificationUrl
            setNotificationUrlFor(orderId)

            orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.notificationUrls).containsExactly(notificationUrl)
        }

        @Test
        fun `updating results sets labId and testType`() {
            val createResponse = registerOrder(sample = sample)

            val orderId = createResponse.id
            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.labId).isNull()
            assertThat(orderInformation.testType).isNull()

            uploadResult(testType)

            val updatedOrderInformation = repository.findById(orderId)
            assertThat(updatedOrderInformation).isNotNull
            assertThat(updatedOrderInformation!!).matches {
                it.labId == labId && it.testType == testType && it.sampledAt == null
            }
        }
    }

    @Nested
    inner class Blood {

        private val sample = Sample.BLOOD
        private val testType = TestType.ANTIBODY

        @Test
        fun `an order can be registered`() {
            val registeredResponse = registerOrder(sample = sample)

            val result = repository.findById(registeredResponse.id)

            assertThat(result).isNotNull
            assertThat(result!!).matches { it.status == Status.IN_PROGRESS && it.sample == sample }
        }

        @Test
        fun `an order with analyt can be registered`() {
            val registeredResponse = registerOrder(orderNumber + "16", sample)

            val result = repository.findById(registeredResponse.id)

            assertThat(result).isNotNull
            assertThat(result!!).matches { it.status == Status.IN_PROGRESS && it.sample == sample }
        }

        @Test
        fun `a lab result can be successfully created, fetched, updated and a result can be uploaded`() {
            // register
            val registeredResponse = registerOrder(sample = sample, notificationUrl = notificationUrl)
            val orderId = registeredResponse.id

            // query result while IN_PROGRESS
            var queryResult = mockMvc.get("/v1/issuers/$issuerId/orders/$orderId")
                .andExpect { jsonPath("$.sampledAt") { doesNotExist() } }
                .andReturn().responseBody<StatusResponse.Found>()

            assertThat(queryResult.status).isEqualTo(Status.IN_PROGRESS)

            // update result
            uploadResult(testType)
            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.status).isEqualTo(Status.POSITIVE)

            verify(exactly = 1) { fcmNotifier.send(match { it.token == fcmToken }) }

            // query result
            queryResult = mockMvc.get("/v1/issuers/$issuerId/orders/$orderId")
                .andExpect { jsonPath("$.sampledAt") { doesNotExist() } }
                .andReturn().responseBody<StatusResponse.Found>()

            assertThat(queryResult.status).isEqualTo(Status.POSITIVE)
        }

        @Test
        fun `the notificationUrl can be updated`() {
            // register
            val registeredResponse = registerOrder(sample = sample)
            val orderId = registeredResponse.id

            var orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.notificationUrls).isEmpty()

            // set notificationUrl
            setNotificationUrlFor(orderId)

            orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.notificationUrls).containsExactly(notificationUrl)
        }

        @Test
        fun `updating results sets labId and testType`() {
            val createResponse = registerOrder(sample = sample)

            val orderId = createResponse.id
            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.labId).isNull()
            assertThat(orderInformation.testType).isNull()

            uploadResult(testType)

            val updatedOrderInformation = repository.findById(orderId)
            assertThat(updatedOrderInformation).isNotNull
            assertThat(updatedOrderInformation!!).matches {
                it.labId == labId && it.testType == testType && it.sampledAt == null
            }
        }
    }

    private fun registerOrder(orderNumber: String = this.orderNumber, sample: Sample, notificationUrl: String? = null): PreIssuedOrderNumberController.RegisterOrderResponse.Created {
        return mockMvc.post("/v1/issuers/$issuerId/orders") {
            val body = mutableMapOf(
                "orderNumber" to orderNumber,
                "sample" to sample
            )

            if (notificationUrl != null)
                body["notificationUrl"] = notificationUrl

            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsBytes(body)
        }.andReturn().responseBody()
    }

    private fun setNotificationUrlFor(id: UUID) =
        mockMvc.put("/v1/issuers/$issuerId/orders/$id") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsBytes(mapOf("notificationUrl" to notificationUrl))
        }.andExpect { status { isOk } }

    private fun uploadResult(testType: TestType) =
        mockMvc.put("/v1/results") {
            contentType = APPLICATION_KEVB_CSV
            headers { setBasicAuth(labIdHeader) }
            param("issuerId", issuerId)
            content = "$orderNumber,${Result.POSITIVE},$testType"
        }.andExpect { status { isOk } }

    private inline fun <reified T> MvcResult.responseBody(): T {
        return objectMapper.readValue(response.contentAsString)
    }
}
