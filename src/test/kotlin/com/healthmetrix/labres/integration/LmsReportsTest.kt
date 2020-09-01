package com.healthmetrix.labres.integration

import com.fasterxml.jackson.databind.JsonNode
import com.healthmetrix.labres.encodeBase64
import com.healthmetrix.labres.lab.Result
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Sample
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.reports.LmsTicketIdentifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import java.time.Instant
import java.util.Date
import java.util.UUID

class LmsReportsTest : AbstractIntegrationTest() {
    private val sample = Sample.SALIVA

    private val event = "sp-1"
    private val ticket = LmsTicketIdentifier(event = event, ticket = UUID.randomUUID().toString())
    private val metadata: JsonNode by lazy { objectMapper.valueToTree<JsonNode>(ticket) }

    @Test
    fun `an orderInformation can be created with a notification url`() {
        val registeredResponse = registerOrder(
            orderNumber = orderNumber,
            issuerId = issuerId,
            testSiteId = testSiteId,
            notificationUrl = fcmNotificationUrl,
            sample = Sample.SALIVA,
            sampledAt = sampledAt,
            metadata = metadata
        )

        assertThatOrderHasBeenSaved(
            id = registeredResponse.id,
            orderNumber = orderNumber,
            issuerId = issuerId,
            testSiteId = testSiteId,
            sample = sample,
            sampledAt = sampledAt,
            metadata = metadata
        )
    }

    @Test
    fun `a report should only return orders with negative results`() {
        val orderNumbers = (0..9).map {
            registerOrder(
                orderNumber = UUID.randomUUID().toString(),
                issuerId = issuerId,
                testSiteId = testSiteId,
                sample = Sample.SALIVA,
                sampledAt = sampledAt,
                metadata = createMetadata(event = event, ticket = UUID.randomUUID().toString())
            ).orderNumber
        }

        orderNumbers.subList(0, 5).forEach { orderNumber ->
            updateResultFor(orderNumber = orderNumber, issuerId = issuerId, result = Result.NEGATIVE)
        }

        val res = getLmsReport(event)

        res.apply {
            assertThat(results).hasSize(5)
            assertThat(nextKey).isNull()
        }
    }

    @Test
    fun `a report should only return orders from whitelisted test sites`() {
        val nonWhitelistedTestSite = "non-whitelisted"

        listOf(testSiteId, testSiteId, nonWhitelistedTestSite, nonWhitelistedTestSite, testSiteId).map { testSiteId ->
            registerOrder(
                orderNumber = UUID.randomUUID().toString(),
                issuerId = issuerId,
                testSiteId = testSiteId,
                sample = Sample.SALIVA,
                sampledAt = sampledAt,
                metadata = createMetadata(event = event, ticket = UUID.randomUUID().toString())
            ).orderNumber
        }.forEach { orderNumber ->
            updateResultFor(orderNumber = orderNumber, issuerId = issuerId, result = Result.NEGATIVE)
        }

        val res = getLmsReport(event)

        res.apply {
            assertThat(results).hasSize(3)
            assertThat(nextKey).isNull()
        }
    }

    @Test
    fun `a report should only return orders for an event`() {
        listOf(event, "another", event, "third", "fourth").map { event ->
            registerOrder(
                orderNumber = UUID.randomUUID().toString(),
                issuerId = issuerId,
                testSiteId = testSiteId,
                sample = Sample.SALIVA,
                sampledAt = sampledAt,
                metadata = createMetadata(event = event, ticket = UUID.randomUUID().toString())
            ).orderNumber
        }.forEach { orderNumber ->
            updateResultFor(orderNumber = orderNumber, issuerId = issuerId, result = Result.NEGATIVE)
        }

        val res = getLmsReport(event)

        res.apply {
            assertThat(results).hasSize(2)
            assertThat(nextKey).isNull()
        }
    }

    @Test
    fun `it should return unauthorized, when reportClient is no valid basic auth user`() {
        mockMvc.get("/v1/reports/lms") {
            headers { setBasicAuth("somethingwrong") }
            param("event", event)
        }.andExpect { status { isUnauthorized } }
    }

    @Test
    fun `it should return forbidden, when reportClient is not registered`() {
        mockMvc.get("/v1/reports/lms") {
            headers { setBasicAuth("nonWhitelisted:pass".encodeBase64()) }
            param("event", event)
        }.andExpect { status { isForbidden } }
    }

    @Test
    fun `it should return forbidden, when reportClient has no whitelisted test sites`() {
        mockMvc.get("/v1/reports/lms") {
            headers { setBasicAuth("test_report_client_2:pass".encodeBase64()) }
            param("event", event)
        }.andExpect { status { isForbidden } }
    }

    @Test
    fun `a report should only return orders that had been sampled after a timestamp`() {
        val sampledAfter = Instant.now()

        listOf(
            sampledAfter.minusSeconds(60),
            sampledAfter.minusSeconds(30),
            sampledAfter.plusSeconds(30),
            sampledAfter.plusSeconds(60),
            sampledAfter.plusSeconds(90)
        ).map { sampledTimestamp ->
            registerOrder(
                orderNumber = UUID.randomUUID().toString(),
                issuerId = issuerId,
                testSiteId = testSiteId,
                sample = Sample.SALIVA,
                sampledAt = sampledTimestamp.toEpochMilli(),
                metadata = createMetadata(event = event, ticket = UUID.randomUUID().toString())
            ).orderNumber
        }.forEach { orderNumber ->
            updateResultFor(orderNumber = orderNumber, issuerId = issuerId, result = Result.NEGATIVE)
        }

        val res = getLmsReport(event = event, sampledAfter = sampledAfter.toEpochMilli())

        res.apply {
            assertThat(results).hasSize(3)
            assertThat(nextKey).isNull()
        }
    }

    @Test
    fun `a report should only return results that had been reported after a timestamp`() {
        val reportedAfter = Instant.now()

        listOf(
            reportedAfter.minusSeconds(60),
            reportedAfter.minusSeconds(30),
            reportedAfter.plusSeconds(30),
            reportedAfter.plusSeconds(60),
            reportedAfter.plusSeconds(90)
        ).map { reportedAt ->
            OrderInformation(
                id = UUID.randomUUID(),
                orderNumber = OrderNumber.from(issuerId, UUID.randomUUID().toString()),
                reportedAt = Date.from(reportedAt),
                issuedAt = Date.from(Instant.now()),
                status = Status.NEGATIVE,
                testSiteId = testSiteId,
                metadata = createMetadata(event = event, ticket = UUID.randomUUID().toString()),
                sampledAt = Instant.now().toEpochMilli(),
                sample = Sample.SALIVA
            ).let(repository::save)
        }

        val res = getLmsReport(event = event, reportedAfter = reportedAfter.toEpochMilli())

        res.apply {
            assertThat(results).hasSize(3)
            assertThat(nextKey).isNull()
        }
    }

    @Test
    fun `pagination should work`() {
        (0..9).map { index ->
            registerOrder(
                orderNumber = UUID.randomUUID().toString(),
                issuerId = issuerId,
                testSiteId = testSiteId,
                sample = Sample.SALIVA,
                sampledAt = sampledAt,
                metadata = createMetadata(event = event, ticket = index.toString())
            ).orderNumber
        }.forEach { orderNumber ->
            updateResultFor(orderNumber = orderNumber, issuerId = issuerId, result = Result.NEGATIVE)
        }

        val res = getLmsReport(event = event, pageSize = 5)

        res.apply {
            assertThat(results).hasSize(5)
            assertThat(nextKey).isNotNull()
        }

        assertThat(res.results.map { r -> r.data.ticket }).containsExactlyInAnyOrder("0", "1", "2", "3", "4")

        val nextRes = getLmsReport(event = event, pageSize = 5, startKey = res.nextKey)

        nextRes.apply {
            assertThat(results).hasSize(5)
            assertThat(nextKey).isNull()
        }

        assertThat(nextRes.results.map { r -> r.data.ticket }).containsExactlyInAnyOrder("5", "6", "7", "8", "9")
    }

    @Test
    fun `returns bad request when a pagination token is being used with altered parameters`() {
        (0..9).map { index ->
            registerOrder(
                orderNumber = UUID.randomUUID().toString(),
                issuerId = issuerId,
                testSiteId = testSiteId,
                sample = Sample.SALIVA,
                sampledAt = sampledAt,
                metadata = createMetadata(event = event, ticket = index.toString())
            ).orderNumber
        }.forEach { orderNumber ->
            updateResultFor(orderNumber = orderNumber, issuerId = issuerId, result = Result.NEGATIVE)
        }

        val res = getLmsReport(event = event, pageSize = 5)
        assertThat(res.nextKey).isNotNull()

        mockMvc.get("/v1/reports/lms") {
            headers { setBasicAuth(reportClientHeader) }
            param("event", event)
            param("sampledAfter", Instant.now().toEpochMilli().toString())
            param("startKey", res.nextKey!!)
        }.andExpect { status { isBadRequest } }
    }

    private fun createMetadata(event: String, ticket: String) =
        LmsTicketIdentifier(event = event, ticket = ticket).let { objectMapper.valueToTree<JsonNode>(it) }
}
