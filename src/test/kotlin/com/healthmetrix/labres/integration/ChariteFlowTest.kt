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
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Sample
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.order.StatusResponse
import com.healthmetrix.labres.persistence.InMemoryOrderInformationRepository
import com.healthmetrix.labres.persistence.OrderInformation
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
import java.time.Instant
import java.util.Date
import java.util.UUID

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
    lateinit var objectMapper: ObjectMapper

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
    inner class SalivaImplicit : AbstractChariteFlowTest(null, null, null)

    @Nested
    inner class Saliva : AbstractChariteFlowTest(Sample.SALIVA, TestType.PCR, null)

    @Nested
    inner class Blood : AbstractChariteFlowTest(Sample.BLOOD, TestType.ANTIBODY, null)

    @Nested
    inner class VerificationSecretOnIssueEon : AbstractChariteFlowTest(
        sample = null,
        testType = null,
        verificationSecret = UUID.randomUUID().toString()
    ) {

        @Test
        fun `result can be fetched by EON and verification secret`() {
            val orderNumberString = "1234567890"

            val order = OrderInformation(
                id = UUID.randomUUID(),
                orderNumber = OrderNumber.from(null, orderNumberString),
                sample = Sample.SALIVA,
                issuedAt = Date.from(Instant.now()),
                status = Status.POSITIVE,
                verificationSecret = verificationSecret
            )

            repository.save(order)

            val actual = mockMvc.get("/v1/orders") {
                param("orderNumber", orderNumberString)
                param("verificationSecret", verificationSecret!!)
            }.andExpect {
                status { isOk }
            }.andReturn().responseBody<StatusResponse.Found>()

            assertThat(actual.status).isEqualTo(order.status)
        }

        @Test
        fun `result can be fetched by EON, verification secret and sample`() {
            val orderNumberString = "1234567890"

            val order = OrderInformation(
                id = UUID.randomUUID(),
                orderNumber = OrderNumber.from(null, orderNumberString),
                sample = Sample.BLOOD,
                issuedAt = Date.from(Instant.now()),
                status = Status.POSITIVE,
                verificationSecret = verificationSecret
            )

            repository.save(order)

            val actual = mockMvc.get("/v1/orders") {
                param("orderNumber", orderNumberString)
                param("verificationSecret", verificationSecret!!)
                param("sample", Sample.BLOOD.toString())
            }.andExpect {
                status { isOk }
            }.andReturn().responseBody<StatusResponse.Found>()

            assertThat(actual.status).isEqualTo(order.status)
        }

        @Test
        fun `result can not be fetched with wrong verification secret`() {
            val orderNumberString = "1234567890"

            val order = OrderInformation(
                id = UUID.randomUUID(),
                orderNumber = OrderNumber.from(null, orderNumberString),
                sample = Sample.BLOOD,
                issuedAt = Date.from(Instant.now()),
                status = Status.POSITIVE,
                verificationSecret = verificationSecret
            )

            repository.save(order)

            val actual = mockMvc.get("/v1/orders") {
                param("orderNumber", orderNumberString)
                param("verificationSecret", "wrong")
                param("sample", Sample.BLOOD.toString())
            }.andExpect {
                status { isForbidden }
            }
        }

        @Test
        fun `result can not be fetched if no verification secret is set in the database`() {
            val orderNumberString = "1234567890"

            val order = OrderInformation(
                id = UUID.randomUUID(),
                orderNumber = OrderNumber.from(null, orderNumberString),
                sample = Sample.BLOOD,
                issuedAt = Date.from(Instant.now()),
                status = Status.POSITIVE,
                verificationSecret = verificationSecret
            )

            repository.save(order)

            val actual = mockMvc.get("/v1/orders") {
                param("orderNumber", orderNumberString)
                param("verificationSecret", "wrong")
                param("sample", Sample.BLOOD.toString())
            }.andExpect {
                status { isForbidden }
            }
        }
    }

    abstract inner class AbstractChariteFlowTest(
        val sample: Sample?,
        val testType: TestType?,
        val verificationSecret: String?
    ) {

        @Test
        fun `an orderInformation can be created with a notification url`() {
            val registeredResponse = registerOrder(notificationUrl, sample, verificationSecret)

            val result = repository.findById(registeredResponse.id)

            assertThat(result).isNotNull
            assertThat(result!!.notificationUrls).containsExactly(notificationUrl)
            assertThat(result.status).isEqualTo(Status.IN_PROGRESS)
            assertThat(result.sample).isEqualTo(sample ?: Sample.SALIVA)
            assertThat(result.verificationSecret).isEqualTo(verificationSecret)
        }

        @Test
        fun `a lab result can be successfully created, fetched and a result can be uploaded`() {
            // register
            val registeredResponse = registerOrder(notificationUrl, sample)
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

            // query result by id
            queryResult = mockMvc.get("/v1/orders/$orderId")
                .andReturn().responseBody<StatusResponse.Found>()

            assertThat(queryResult.sampledAt).isEqualTo(sampledAt)
            assertThat(queryResult.status).isEqualTo(Status.POSITIVE)
        }

        @Test
        fun `updating results sets labId, sampledAt and testType`() {
            val createResponse = registerOrder("http://before.test", sample)

            val orderId = createResponse.id
            val orderNumber = createResponse.orderNumber
            val orderInformation = repository.findById(orderId)!!
            assertThat(orderInformation.labId).isNull()
            assertThat(orderInformation.testType).isNull()
            assertThat(orderInformation.sampledAt).isNull()

            updateResultFor(orderNumber, testType, sampledAt)

            val updatedOrderInformation = repository.findById(orderId)
            assertThat(updatedOrderInformation).isNotNull
            assertThat(updatedOrderInformation!!.labId).isEqualTo(labId)
            assertThat(updatedOrderInformation.testType).isEqualTo(testType ?: TestType.PCR)
            assertThat(updatedOrderInformation.sampledAt).isEqualTo(sampledAt)
        }

        private fun registerOrder(
            url: String? = notificationUrl,
            sample: Sample?,
            verificationSecret: String? = null
        ) = mockMvc.post("/v1/orders") {
            contentType = MediaType.APPLICATION_JSON
            val body = mutableMapOf(
                "notificationUrl" to url
            )

            if (sample != null)
                body["sample"] = sample.toString()

            if (verificationSecret != null)
                body["verificationSecret"] = verificationSecret

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

        inline fun <reified T> MvcResult.responseBody(): T {
            return objectMapper.readValue(response.contentAsString)
        }
    }
}
