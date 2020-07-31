package com.healthmetrix.labres.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.healthmetrix.labres.LabResApplication
import com.healthmetrix.labres.encodeBase64
import com.healthmetrix.labres.lab.Result
import com.healthmetrix.labres.lab.TestType
import com.healthmetrix.labres.notifications.Notification
import com.healthmetrix.labres.notifications.Notifier
import com.healthmetrix.labres.order.PreIssuedOrderNumberController
import com.healthmetrix.labres.order.Sample
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.persistence.InMemoryOrderInformationRepository
import com.healthmetrix.labres.persistence.OrderInformationRepository
import com.ninjasquad.springmockk.SpykBean
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
class IlluminaFlowTest {
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
    }

    private val labId = "test_lab"
    private val testSite = "site"
    private val labIdHeader = "$labId:pass".encodeBase64()
    private val issuerId = "test_issuer"
    private val orderNumber = "1234567890"
    private val fcmToken = "test"
    private val notificationUrl = "fcm://labres@$fcmToken"

    // TODO rewrite this to use bulk upload

    @Test
    fun `an order can be registered`() {
        val registeredResponse = registerOrder(Sample.SALIVA)

        val result = repository.findById(registeredResponse.id)

        assertThat(result).isNotNull
        assertThat(result!!).matches { order ->
            order.status == Status.IN_PROGRESS &&
                order.sample == Sample.SALIVA
        }
    }

    @Test
    fun `a lab result can be successfully created, fetched, updated and a result can be uploaded for testType NGS`() {
        val registeredResponse = registerOrder(Sample.SALIVA)

        val orderId = registeredResponse.id
        mockMvc.get("/v1/issuers/$issuerId/orders/$orderId")
            .andExpect { status { isOk } }

        setNotificationUrlFor(orderId)

        uploadResult(TestType.NGS)

        val orderInformation = repository.findById(orderId)!!
        assertThat(orderInformation.status).isEqualTo(Status.POSITIVE)

        verify(exactly = 1) { fcmNotifier.send(match { it.token == fcmToken }) }
    }

    @Test
    fun `a lab result can be successfully created, fetched, updated and a result can be uploaded for testType ANTIBODY`() {
        val registeredResponse = registerOrder(Sample.BLOOD)

        val orderId = registeredResponse.id
        mockMvc.get("/v1/issuers/$issuerId/orders/$orderId")
            .andExpect {
                jsonPath("$.sampledAt") { doesNotExist() }
                status { isOk }
            }

        uploadResult(TestType.ANTIBODY)

        val orderInformation = repository.findById(orderId)!!
        assertThat(orderInformation.status).isEqualTo(Status.POSITIVE)
    }

    @Test
    fun `updating results sets labId and testType`() {
        val createResponse = registerOrder(Sample.BLOOD)

        val orderId = createResponse.id
        val orderInformation = repository.findById(orderId)!!
        assertThat(orderInformation.labId).isNull()
        assertThat(orderInformation.testType).isNull()

        uploadResult(TestType.ANTIBODY)

        val updatedOrderInformation = repository.findById(orderId)
        assertThat(updatedOrderInformation).isNotNull
        assertThat(updatedOrderInformation!!).matches {
            it.labId == labId && it.testType == TestType.ANTIBODY
        }
    }

    private fun registerOrder(sample: Sample): PreIssuedOrderNumberController.RegisterOrderResponse.Created {
        return mockMvc.post("/v1/issuers/$issuerId/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsBytes(
                mapOf(
                    "orderNumber" to orderNumber,
                    "testSiteId" to testSite,
                    "sample" to sample.toString()
                )
            )
        }.andReturn().responseBody()
    }

    private fun setNotificationUrlFor(id: UUID) =
        mockMvc.put("/v1/issuers/$issuerId/orders/$id") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsBytes(mapOf("notificationUrl" to notificationUrl))
        }.andExpect { status { isOk } }

    private fun uploadResult(testType: TestType) =
        mockMvc.put("/v1/results") {
            contentType = MediaType.APPLICATION_JSON
            headers { setBasicAuth(labIdHeader) }
            param("issuerId", issuerId)
            content = objectMapper.writeValueAsBytes(
                mapOf(
                    "orderNumber" to orderNumber,
                    "result" to Result.POSITIVE.toString(),
                    "type" to testType.toString()
                )
            )
        }.andExpect { status { isOk } }

    private inline fun <reified T> MvcResult.responseBody(): T {
        return objectMapper.readValue(response.contentAsString)
    }
}
