package com.healthmetrix.labres.integration

import com.healthmetrix.labres.encodeBase64
import com.healthmetrix.labres.lab.Result
import com.healthmetrix.labres.lab.TestType
import com.healthmetrix.labres.notifications.Notification
import com.healthmetrix.labres.notifications.Notifier
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Sample
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.persistence.InMemoryOrderInformationRepository
import com.healthmetrix.labres.persistence.OrderInformation
import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import java.time.Instant
import java.util.Date
import java.util.UUID

class ChariteFlowTest : AbstractIntegrationTest() {

    @SpykBean
    private lateinit var httpNotifier: Notifier<Notification.HttpNotification>

    @BeforeEach
    override fun setUp() {
        (repository as InMemoryOrderInformationRepository).clear()
        clearMocks(httpNotifier)
    }

    private val labId = "test_lab"
    private val labIdHeader = "$labId:pass".encodeBase64()

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

            val actual = getResultByOrderNumber(orderNumberString, verificationSecret!!)

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

            val actual = getResultByOrderNumber(
                orderNumber = orderNumberString,
                verificationSecret = verificationSecret!!,
                sample = Sample.BLOOD
            )

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

            mockMvc.get("/v1/orders") {
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

            mockMvc.get("/v1/orders") {
                param("orderNumber", orderNumberString)
                param("verificationSecret", "wrong")
                param("sample", Sample.BLOOD.toString())
            }.andExpect {
                status { isForbidden }
            }
        }

        @Test
        fun `updating results can set verificationSecret`() {
            val createResponse = issueEon(null, sample ?: Sample.SALIVA)

            val orderId = createResponse.id
            val orderNumber = createResponse.orderNumber

            assertThatOrderHasBeenSaved(
                id = orderId,
                verificationSecret = null
            )

            updateResultFor(
                orderNumber = orderNumber,
                labIdHeader = labIdHeader,
                result = Result.POSITIVE,
                verificationSecret = verificationSecret
            )

            assertThatOrderHasBeenSaved(
                id = orderId,
                verificationSecret = verificationSecret
            )
        }

        @Test
        fun `updating results can overwrite verificationSecret`() {
            val firstVerificationSecret = "first"
            val createResponse = issueEon(
                notificationUrl = null,
                sample = sample ?: Sample.SALIVA,
                verificationSecret = firstVerificationSecret
            )

            val orderId = createResponse.id
            val orderNumber = createResponse.orderNumber

            assertThatOrderHasBeenSaved(
                id = orderId,
                verificationSecret = firstVerificationSecret
            )

            updateResultFor(
                orderNumber = orderNumber,
                labIdHeader = labIdHeader,
                result = Result.POSITIVE,
                verificationSecret = verificationSecret
            )

            assertThatOrderHasBeenSaved(
                id = orderId,
                verificationSecret = verificationSecret
            )
        }

        @Test
        fun `updating results doesn't overwrite verificationSecret if verificationSecret is null on the request`() {
            val firstVerificationSecret = "first"
            val createResponse = issueEon(
                notificationUrl = null,
                sample = sample ?: Sample.SALIVA,
                verificationSecret = firstVerificationSecret
            )

            val orderId = createResponse.id
            val orderNumber = createResponse.orderNumber

            assertThatOrderHasBeenSaved(
                id = orderId,
                verificationSecret = firstVerificationSecret
            )

            updateResultFor(
                orderNumber = orderNumber,
                labIdHeader = labIdHeader,
                result = Result.POSITIVE
            )

            assertThatOrderHasBeenSaved(
                id = orderId,
                verificationSecret = firstVerificationSecret
            )
        }
    }

    abstract inner class AbstractChariteFlowTest(
        val sample: Sample?,
        val testType: TestType?,
        val verificationSecret: String?
    ) {

        @Test
        fun `an orderInformation can be created with a notification url`() {
            val registeredResponse = issueEon(
                notificationUrl = httpNotificationUrl,
                sample = sample ?: Sample.SALIVA,
                verificationSecret = verificationSecret
            )

            repository.findById(registeredResponse.id)

            assertThatOrderHasBeenSaved(
                id = registeredResponse.id,
                status = Status.IN_PROGRESS,
                sample = sample ?: Sample.SALIVA,
                verificationSecret = verificationSecret
            )
        }

        @Test
        fun `a lab result can be successfully created, fetched and a result can be uploaded`() {
            // register
            val registeredResponse = issueEon(
                notificationUrl = httpNotificationUrl,
                sample = sample ?: Sample.SALIVA,
                verificationSecret = testSiteId
            )
            val orderId = registeredResponse.id
            val orderNumber = registeredResponse.orderNumber

            // query result while IN_PROGRESS
            var queryResult = getResultByOrderId(orderId)
            assertThat(queryResult.sampledAt == null)

            assertThat(queryResult.status).isEqualTo(Status.IN_PROGRESS)

            // update result
            updateResultFor(
                orderNumber = orderNumber,
                labIdHeader = labIdHeader,
                result = Result.POSITIVE,
                testType = testType ?: TestType.PCR,
                sampledAt = sampledAt
            )

            assertThatOrderHasBeenSaved(
                id = orderId,
                status = Status.POSITIVE
            )

            verify(exactly = 1) { httpNotifier.send(match { it.url == httpNotificationUrl }) }

            // query result by id
            queryResult = getResultByOrderId(orderId)
            assertThat(queryResult.sampledAt).isEqualTo(sampledAt)
            assertThat(queryResult.status).isEqualTo(Status.POSITIVE)
        }

        @Test
        fun `updating results sets labId, sampledAt and testType`() {
            val createResponse = issueEon(
                notificationUrl = "http://before.test",
                sample = sample ?: Sample.SALIVA,
                verificationSecret = verificationSecret
            )

            val orderId = createResponse.id
            val orderNumber = createResponse.orderNumber

            assertThatOrderHasBeenSaved(
                id = orderId,
                labId = null,
                testType = null,
                sampledAt = null
            )

            updateResultFor(
                orderNumber = orderNumber,
                labIdHeader = labIdHeader,
                result = Result.POSITIVE,
                testType = testType ?: TestType.PCR,
                sampledAt = sampledAt
            )

            assertThatOrderHasBeenSaved(
                id = orderId,
                labId = labId,
                testType = testType ?: TestType.PCR,
                sampledAt = sampledAt
            )
        }
    }
}
