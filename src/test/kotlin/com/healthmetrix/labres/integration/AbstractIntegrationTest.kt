package com.healthmetrix.labres.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.healthmetrix.labres.LabResApplication
import com.healthmetrix.labres.lab.Result
import com.healthmetrix.labres.lab.TestType
import com.healthmetrix.labres.lab.UpdateResultRequest
import com.healthmetrix.labres.order.ExternalOrderNumberController
import com.healthmetrix.labres.order.PreIssuedOrderNumberController
import com.healthmetrix.labres.order.Sample
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.order.StatusResponse
import com.healthmetrix.labres.persistence.InMemoryOrderInformationRepository
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Instant
import org.junit.jupiter.api.BeforeEach
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
abstract class AbstractIntegrationTest {
    @Autowired
    protected lateinit var repository: OrderInformationRepository

    @Autowired
    protected final lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var mockMvc: MockMvc

    protected val orderNumber = UUID.randomUUID().toString()
    protected val issuerId = "test_issuer"
    protected val testSiteId = "test_test_site"
    protected val httpNotificationUrl = "http://notify.test"
    protected val fcmNotificationUrl = "fcm://labres@notify.test"
    protected val sampledAt = Instant.now().millis

    @BeforeEach
    internal fun setUp() {
        (repository as InMemoryOrderInformationRepository).clear()
    }

    fun assertThatOrderHasBeenSaved(
        id: UUID,
        orderNumber: String? = null,
        issuerId: String? = null,
        status: Status? = null,
        testSiteId: String? = null,
        notificationUrl: String? = null,
        sample: Sample? = null,
        sampledAt: Long? = null,
        metadata: JsonNode? = null,
        verificationSecret: String? = null,
        labId: String? = null,
        testType: TestType? = null
    ): OrderInformation {
        val result = repository.findById(id)

        assertThat(result).isNotNull

        result?.apply {
            if (orderNumber != null)
                assertThat(this.orderNumber.number).isEqualTo(orderNumber)

            if (issuerId != null)
                assertThat(this.orderNumber.issuerId).isEqualTo(issuerId)

            if (testSiteId != null)
                assertThat(testSiteId).isEqualTo(testSiteId)

            if (notificationUrl != null)
                assertThat(notificationUrls).contains(notificationUrl)

            if (sample != null)
                assertThat(sample).isEqualTo(sample)

            if (sampledAt != null)
                assertThat(sampledAt).isEqualTo(sampledAt)

            if (metadata != null)
                assertThat(metadata).isEqualTo(metadata)

            if (verificationSecret != null)
                assertThat(verificationSecret).isEqualTo(verificationSecret)

            if (status != null)
                assertThat(status).isEqualTo(status)

            if (labId != null)
                assertThat(labId).isEqualTo(labId)

            if (testType != null)
                assertThat(testType).isEqualTo(testType)
        }

        return result!!
    }

    fun registerOrder(
        orderNumber: String,
        issuerId: String,
        testSiteId: String? = null,
        notificationUrl: String? = null,
        sample: Sample = Sample.SALIVA,
        sampledAt: Long? = null,
        metadata: JsonNode? = null
    ): PreIssuedOrderNumberController.RegisterOrderResponse.Created =
        mockMvc.post("/v1/issuers/$issuerId/orders") {
            val request = PreIssuedOrderNumberController.RegisterOrderRequest(
                orderNumber = orderNumber,
                testSiteId = testSiteId,
                notificationUrl = notificationUrl,
                sample = sample,
                sampledAt = sampledAt,
                metadata = metadata
            )

            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsBytes(request)
        }.andReturn().responseBody()

    fun issueEon(
        notificationUrl: String? = null,
        sample: Sample = Sample.SALIVA,
        verificationSecret: String? = null
    ): ExternalOrderNumberController.IssueExternalOrderNumberResponse.Created =
        mockMvc.post("/v1/orders") {
            contentType = MediaType.APPLICATION_JSON
            val request = ExternalOrderNumberController.IssueExternalOrderNumberRequestBody(
                notificationUrl = notificationUrl,
                sample = sample,
                verificationSecret = verificationSecret
            )
            content = objectMapper.writeValueAsBytes(request)
        }.andReturn().responseBody()

    fun updateResultFor(
        orderNumber: String,
        issuerId: String? = null,
        labIdHeader: String,
        result: Result,
        testType: TestType = TestType.PCR,
        sampledAt: Long? = null,
        verificationSecret: String? = null
    ) = mockMvc.put("/v1/results") {
        contentType = MediaType.APPLICATION_JSON
        headers { setBasicAuth(labIdHeader) }

        if (issuerId != null)
            param("issuerId", issuerId)

        val request = UpdateResultRequest(
            orderNumber = orderNumber,
            result = result,
            verificationSecret = verificationSecret,
            sampledAt = sampledAt,
            type = testType
        )

        content = objectMapper.writeValueAsBytes(request)
    }.andExpect { status { isOk } }

    fun getResultByOrderNumber(
        orderNumber: String,
        verificationSecret: String,
        sample: Sample? = null
    ): StatusResponse.Found = mockMvc.get("/v1/orders") {
        param("orderNumber", orderNumber)
        param("verificationSecret", verificationSecret)

        if (sample != null)
            param("sample", sample.toString())
    }.andExpect {
        status { isOk }
    }.andReturn().responseBody()

    fun getResultByOrderId(
        id: UUID,
        issuerId: String? = null
    ): StatusResponse.Found {
        val url = if (issuerId == null) {
            "/v1/orders/$id"
        } else {
            "/v1/issuers/$issuerId/orders/$id"
        }

        return mockMvc.get(url).andReturn().responseBody()
    }

    private inline fun <reified T> MvcResult.responseBody(): T {
        return objectMapper.readValue(response.contentAsString)
    }
}
