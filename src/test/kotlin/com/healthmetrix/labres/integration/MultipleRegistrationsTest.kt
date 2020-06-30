package com.healthmetrix.labres.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.healthmetrix.labres.LabResApplication
import com.healthmetrix.labres.encodeBase64
import com.healthmetrix.labres.lab.APPLICATION_KEVB_CSV
import com.healthmetrix.labres.lab.PCR_LOINC
import com.healthmetrix.labres.lab.Result
import com.healthmetrix.labres.notifications.Notification
import com.healthmetrix.labres.notifications.Notifier
import com.healthmetrix.labres.order.PreIssuedOrderNumberController
import com.healthmetrix.labres.order.Sample
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.persistence.InMemoryOrderInformationRepository
import com.healthmetrix.labres.persistence.OrderInformationRepository
import com.ninjasquad.springmockk.SpykBean
import io.mockk.verifyAll
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
class MultipleRegistrationsTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var repository: OrderInformationRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @SpykBean
    private lateinit var fcmNotifier: Notifier<Notification.FcmNotification>

    @SpykBean
    private lateinit var httpNotifier: Notifier<Notification.HttpNotification>

    @BeforeEach
    internal fun setUp() {
        (repository as InMemoryOrderInformationRepository).clear()
    }

    private val labId = "test_lab"
    private val labIdHeader = "$labId:pass".encodeBase64()
    private val issuerId = "test_issuer"
    private val fcmToken = "test"
    private val notificationUrl = "fcm://labres@$fcmToken"

    @Test
    fun `an order can be registered ten times without notification url`() {
        val orderNumber = UUID.randomUUID().toString()
        val response = (0..9).map { registerOrder(orderNumber) }.last()

        setNotificationUrlFor(response.id)

        val result = repository.findById(response.id)

        assertThat(result).isNotNull
        assertThat(result!!).matches {
            it.status == Status.IN_PROGRESS &&
                it.sample == Sample.SALIVA &&
                it.notificationUrls == listOf(notificationUrl)
        }
    }

    @Test
    fun `an order can be registered ten times with the same notification url`() {
        val orderNumber = UUID.randomUUID().toString()
        val response = (0..9).map { registerOrder(orderNumber, notificationUrl) }.last()

        val result = repository.findById(response.id)

        assertThat(result).isNotNull
        assertThat(result!!).matches {
            it.status == Status.IN_PROGRESS &&
                it.sample == Sample.SALIVA &&
                it.notificationUrls == listOf(notificationUrl)
        }
    }

    @Test
    fun `an order can be registered three times with different notification urls`() {
        val orderNumber = UUID.randomUUID().toString()
        val notificationUrls = listOf("https://a.test", "https://b.test", "https://c.test")
        val response = notificationUrls.map { registerOrder(orderNumber, it) }.last()

        val result = repository.findById(response.id)

        assertThat(result).isNotNull
        assertThat(result!!).matches {
            it.status == Status.IN_PROGRESS &&
                it.sample == Sample.SALIVA &&
                it.notificationUrls == notificationUrls
        }
    }

    @Test
    fun `should return conflict when four different notification urls for an order are being registered`() {
        val orderNumber = UUID.randomUUID().toString()
        val notificationUrls = listOf("https://a.test", "https://b.test", "https://c.test")
        notificationUrls.map { registerOrder(orderNumber, it) }.last()

        mockMvc.post("/v1/issuers/$issuerId/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsBytes(
                mapOf(
                    "orderNumber" to orderNumber,
                    "sample" to Sample.SALIVA,
                    "notificationUrl" to "https://d.test"
                )
            )
        }.andExpect { status { isConflict } }
    }

    @Test
    fun `should return conflict when the order already has a result`() {
        val orderNumber = UUID.randomUUID().toString()
        registerOrder(orderNumber, notificationUrl)

        uploadResult(orderNumber)

        mockMvc.post("/v1/issuers/$issuerId/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsBytes(
                mapOf(
                    "orderNumber" to orderNumber,
                    "sample" to Sample.SALIVA,
                    "notificationUrl" to notificationUrl
                )
            )
        }.andExpect { status { isConflict } }
    }

    @Test
    fun `an order can be registered three times with different notification urls and all of them get notified on update`() {
        val orderNumber = UUID.randomUUID().toString()
        val fcmToken = "test_token"
        val notificationUrls = listOf("https://a.test", "fcm://labres@$fcmToken", "https://c.test")
        notificationUrls.map { registerOrder(orderNumber, it) }.last()

        uploadResult(orderNumber)

        verifyAll {
            httpNotifier.send(match { it.url == notificationUrls[0] })
            httpNotifier.send(match { it.url == notificationUrls[2] })
            fcmNotifier.send(match { it.token == fcmToken })
        }
    }

    private fun registerOrder(
        orderNumber: String,
        notificationUrl: String? = null
    ): PreIssuedOrderNumberController.RegisterOrderResponse.Created {
        return mockMvc.post("/v1/issuers/$issuerId/orders") {
            contentType = MediaType.APPLICATION_JSON

            val requestBody = mutableMapOf(
                "orderNumber" to orderNumber,
                "sample" to Sample.SALIVA
            )

            if (notificationUrl != null) {
                requestBody["notificationUrl"] = notificationUrl
            }

            content = objectMapper.writeValueAsBytes(requestBody)
        }.andReturn().responseBody()
    }

    private fun setNotificationUrlFor(id: UUID) =
        mockMvc.put("/v1/issuers/$issuerId/orders/$id") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsBytes(mapOf("notificationUrl" to notificationUrl))
        }.andExpect { status { isOk } }

    private fun uploadResult(orderNumber: String) =
        mockMvc.put("/v1/results") {
            contentType = APPLICATION_KEVB_CSV
            headers { setBasicAuth(labIdHeader) }
            param("issuerId", issuerId)
            content = "$orderNumber,${Result.POSITIVE},$PCR_LOINC"
        }.andExpect { status { isOk } }

    private inline fun <reified T> MvcResult.responseBody(): T {
        return objectMapper.readValue(response.contentAsString)
    }
}
