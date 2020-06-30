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
    }

    private val labId = "test_lab"
    private val labIdHeader = "$labId:pass".encodeBase64()
    private val notificationUrl = "http://notify.test"

    @Test
    fun `an orderInformation can be created with a notification url`() {
        val registeredResponse = registerOrderWithNotificationUrl()

        val result = repository.findById(registeredResponse.id)

        assertThat(result).isNotNull
        assertThat(result!!).matches { order ->
            order.notificationUrls == listOf(notificationUrl) &&
                order.status == Status.IN_PROGRESS &&
                order.sample == Sample.SALIVA
        }
    }

    @Test
    fun `a lab result can be successfully created, fetched and a result can be uploaded`() {
        val registeredResponse = registerOrderWithNotificationUrl()

        val orderId = registeredResponse.id
        mockMvc.get("/v1/orders/$orderId")
            .andReturn().responseBody<StatusResponse.Found>()

        val orderNumber = registeredResponse.orderNumber
        updateResultFor(orderNumber)

        val orderInformation = repository.findById(orderId)!!
        assertThat(orderInformation.status).isEqualTo(Status.POSITIVE)

        verify(exactly = 1) { httpNotifier.send(match { it.url == notificationUrl }) }
    }

    @Test
    fun `updating results sets labId and testType`() {
        val createResponse = registerOrderWithNotificationUrl("http://before.test")

        val orderId = createResponse.id
        val orderNumber = createResponse.orderNumber
        val orderInformation = repository.findById(orderId)!!
        assertThat(orderInformation.labId).isNull()
        assertThat(orderInformation.testType).isNull()

        updateResultFor(orderNumber)

        val updatedOrderInformation = repository.findById(orderId)
        assertThat(updatedOrderInformation).isNotNull
        assertThat(updatedOrderInformation!!).matches {
            it.labId == labId && it.testType == TestType.PCR
        }
    }

    private fun registerOrderWithNotificationUrl(url: String? = notificationUrl) = mockMvc.post("/v1/orders") {
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsBytes(
            mapOf(
                "notificationUrl" to url
            )
        )
    }.andReturn().responseBody<IssueExternalOrderNumberResponse.Created>()

    private fun updateResultFor(orderNumber: String) =
        mockMvc.put("/v1/results") {
            contentType = MediaType.APPLICATION_JSON
            headers { setBasicAuth(labIdHeader) }
            content = objectMapper.writeValueAsBytes(
                mapOf(
                    "orderNumber" to orderNumber,
                    "result" to Result.POSITIVE.toString()
                )
            )
        }.andExpect { status { isOk } }

    private inline fun <reified T> MvcResult.responseBody(): T {
        return objectMapper.readValue(response.contentAsString)
    }
}
