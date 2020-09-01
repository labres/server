package com.healthmetrix.labres.reports

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Sample
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import com.healthmetrix.labres.persistence.ScanResult
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Date
import java.util.UUID

internal class CreateLmsReportUseCaseTest {
    private val objectMapper = jacksonObjectMapper().registerKotlinModule()

    private val reportClient = "client"
    private val event = "event"
    private val params = ReportParameters(
        event = event,
        reportedAfter = null,
        sampledAfter = null
    )
    private val testSites = listOf("test_site_1", "test_site_2")
    private val ticketIdentifier1 = LmsTicketIdentifier(event, UUID.randomUUID().toString())
    private val ticketIdentifier2 = LmsTicketIdentifier(event, UUID.randomUUID().toString())

    private val returnedOrder1 = OrderInformation(
        id = UUID.randomUUID(),
        orderNumber = OrderNumber.from("hmx", UUID.randomUUID().toString()),
        status = Status.NEGATIVE,
        issuedAt = Date.from(Instant.now().minusSeconds(30)),
        sample = Sample.SALIVA,
        reportedAt = Date.from(Instant.now().minusSeconds(10)),
        sampledAt = Instant.now().minusSeconds(150).toEpochMilli(),
        metadata = ticketIdentifier1.let { objectMapper.valueToTree<JsonNode>(it) }
    )
    private val returnedOrder2 = OrderInformation(
        id = UUID.randomUUID(),
        orderNumber = OrderNumber.from("hmx", UUID.randomUUID().toString()),
        status = Status.NEGATIVE,
        issuedAt = Date.from(Instant.now().minusSeconds(50)),
        sample = Sample.SALIVA,
        reportedAt = Date.from(Instant.now().minusSeconds(40)),
        sampledAt = Instant.now().minusSeconds(120).toEpochMilli(),
        metadata = ticketIdentifier2.let { objectMapper.valueToTree<JsonNode>(it) }
    )

    private val scanResult = ScanResult(
        results = listOf(returnedOrder1, returnedOrder2),
        lastEvaluatedKey = null
    )

    private val repository: OrderInformationRepository = mockk() {
        every { scanForTestSiteAndEvent(any(), any(), any(), any(), any(), any()) } returns scanResult
    }
    private val clientAuthenticator: ReportClientAuthenticator = mockk() {
        every { getWhitelistedTestSites(any()) } returns testSites
    }

    private val underTest = CreateLmsReportUseCase(repository, clientAuthenticator)

    @Test
    fun `it should return a correct report`() {
        val res = underTest.invoke(
            client = reportClient,
            params = params,
            paginationToken = null,
            pageSize = null
        )

        assertThat(res).isInstanceOf(Ok::class.java)
        assertThat(res.unwrap()).isEqualTo(
            LmsReport(
                results = listOf(
                    LmsTicket(
                        data = ticketIdentifier1,
                        sampledAt = returnedOrder1.sampledAt,
                        reportedAt = returnedOrder1.reportedAt!!.time,
                        issuedAt = returnedOrder1.issuedAt.time
                    ),
                    LmsTicket(
                        data = ticketIdentifier2,
                        sampledAt = returnedOrder2.sampledAt,
                        reportedAt = returnedOrder2.reportedAt!!.time,
                        issuedAt = returnedOrder2.issuedAt.time
                    )
                ),
                paginationToken = null
            )
        )
    }

    @Test
    fun `it should return a correct report with pagination token and page size`() {
        val res = underTest.invoke(
            client = reportClient,
            params = params,
            paginationToken = PaginationToken(
                reportParameters = params,
                exclusiveStartKey = returnedOrder2.id.toString()
            ),
            pageSize = 2
        )

        assertThat(res).isInstanceOf(Ok::class.java)
        assertThat(res.unwrap()).isEqualTo(
            LmsReport(
                results = listOf(
                    LmsTicket(
                        data = ticketIdentifier1,
                        sampledAt = returnedOrder1.sampledAt,
                        reportedAt = returnedOrder1.reportedAt!!.time,
                        issuedAt = returnedOrder1.issuedAt.time
                    ),
                    LmsTicket(
                        data = ticketIdentifier2,
                        sampledAt = returnedOrder2.sampledAt,
                        reportedAt = returnedOrder2.reportedAt!!.time,
                        issuedAt = returnedOrder2.issuedAt.time
                    )
                ),
                paginationToken = null
            )
        )
    }

    @Test
    fun `it should return forbidden if client has not been registered`() {
        every { clientAuthenticator.getWhitelistedTestSites(any()) } returns null

        val res = underTest.invoke(
            client = reportClient,
            params = params,
            paginationToken = null,
            pageSize = null
        )

        assertThat(res).isInstanceOf(Err::class.java)
        assertThat(res.component2()).isEqualTo(CreateLmsReportUseCase.Error.FORBIDDEN)
    }

    @Test
    fun `it should return forbidden if client has no whitelisted test sites`() {
        every { clientAuthenticator.getWhitelistedTestSites(any()) } returns emptyList()

        val res = underTest.invoke(
            client = reportClient,
            params = params,
            paginationToken = null,
            pageSize = null
        )

        assertThat(res).isInstanceOf(Err::class.java)
        assertThat(res.component2()).isEqualTo(CreateLmsReportUseCase.Error.FORBIDDEN)
    }

    @Test
    fun `it should return bad request if report parameters and pagination token don't match`() {
        val res = underTest.invoke(
            client = reportClient,
            params = params,
            paginationToken = PaginationToken(
                reportParameters = params.copy(sampledAfter = Instant.now().toEpochMilli()),
                exclusiveStartKey = returnedOrder2.id.toString()
            ),
            pageSize = null
        )

        assertThat(res).isInstanceOf(Err::class.java)
        assertThat(res.component2()).isEqualTo(CreateLmsReportUseCase.Error.BAD_REQUEST)
    }

    @Test
    fun `it should return a correct report with pagination token`() {
        every {
            repository.scanForTestSiteAndEvent(any(), any(), any(), any(), any(), any())
        } returns scanResult.copy(lastEvaluatedKey = returnedOrder2.id.toString())

        val res = underTest.invoke(
            client = reportClient,
            params = params,
            paginationToken = null,
            pageSize = null
        )

        assertThat(res).isInstanceOf(Ok::class.java)
        assertThat(res.unwrap()).isEqualTo(
            LmsReport(
                results = listOf(
                    LmsTicket(
                        data = ticketIdentifier1,
                        sampledAt = returnedOrder1.sampledAt,
                        reportedAt = returnedOrder1.reportedAt!!.time,
                        issuedAt = returnedOrder1.issuedAt.time
                    ),
                    LmsTicket(
                        data = ticketIdentifier2,
                        sampledAt = returnedOrder2.sampledAt,
                        reportedAt = returnedOrder2.reportedAt!!.time,
                        issuedAt = returnedOrder2.issuedAt.time
                    )
                ),
                paginationToken = PaginationToken(
                    reportParameters = params,
                    exclusiveStartKey = returnedOrder2.id.toString()
                )
            )
        )
    }

    @Test
    fun `it should filter orders without ticket`() {
        every {
            repository.scanForTestSiteAndEvent(any(), any(), any(), any(), any(), any())
        } returns scanResult.copy(
            results = listOf(
                returnedOrder1.copy(
                    metadata = objectMapper.valueToTree(
                        mapOf(
                            "event" to event
                        )
                    )
                ),
                returnedOrder2
            )
        )

        val res = underTest.invoke(
            client = reportClient,
            params = params,
            paginationToken = null,
            pageSize = null
        )

        assertThat(res).isInstanceOf(Ok::class.java)
        assertThat(res.unwrap()).isEqualTo(
            LmsReport(
                results = listOf(
                    LmsTicket(
                        data = ticketIdentifier2,
                        sampledAt = returnedOrder2.sampledAt,
                        reportedAt = returnedOrder2.reportedAt!!.time,
                        issuedAt = returnedOrder2.issuedAt.time
                    )
                ),
                paginationToken = null
            )
        )
    }

    @Test
    fun `it should filter orders without event`() {
        every {
            repository.scanForTestSiteAndEvent(any(), any(), any(), any(), any(), any())
        } returns scanResult.copy(
            results = listOf(
                returnedOrder1.copy(
                    metadata = objectMapper.valueToTree(
                        mapOf(
                            "ticket" to UUID.randomUUID().toString()
                        )
                    )
                ),
                returnedOrder2
            )
        )

        val res = underTest.invoke(
            client = reportClient,
            params = params,
            paginationToken = null,
            pageSize = null
        )

        assertThat(res).isInstanceOf(Ok::class.java)
        assertThat(res.unwrap()).isEqualTo(
            LmsReport(
                results = listOf(
                    LmsTicket(
                        data = ticketIdentifier2,
                        sampledAt = returnedOrder2.sampledAt,
                        reportedAt = returnedOrder2.reportedAt!!.time,
                        issuedAt = returnedOrder2.issuedAt.time
                    )
                ),
                paginationToken = null
            )
        )
    }

    @Test
    fun `it should filter orders without reportedAt`() {
        every {
            repository.scanForTestSiteAndEvent(any(), any(), any(), any(), any(), any())
        } returns scanResult.copy(
            results = listOf(
                returnedOrder1.copy(reportedAt = null), returnedOrder2
            )
        )

        val res = underTest.invoke(
            client = reportClient,
            params = params,
            paginationToken = null,
            pageSize = null
        )

        assertThat(res).isInstanceOf(Ok::class.java)
        assertThat(res.unwrap()).isEqualTo(
            LmsReport(
                results = listOf(
                    LmsTicket(
                        data = ticketIdentifier2,
                        sampledAt = returnedOrder2.sampledAt,
                        reportedAt = returnedOrder2.reportedAt!!.time,
                        issuedAt = returnedOrder2.issuedAt.time
                    )
                ),
                paginationToken = null
            )
        )
    }

    @Test
    fun `it should filter orders without metadata`() {
        every {
            repository.scanForTestSiteAndEvent(any(), any(), any(), any(), any(), any())
        } returns scanResult.copy(
            results = listOf(
                returnedOrder1.copy(metadata = null), returnedOrder2
            )
        )

        val res = underTest.invoke(
            client = reportClient,
            params = params,
            paginationToken = null,
            pageSize = null
        )

        assertThat(res).isInstanceOf(Ok::class.java)
        assertThat(res.unwrap()).isEqualTo(
            LmsReport(
                results = listOf(
                    LmsTicket(
                        data = ticketIdentifier2,
                        sampledAt = returnedOrder2.sampledAt,
                        reportedAt = returnedOrder2.reportedAt!!.time,
                        issuedAt = returnedOrder2.issuedAt.time
                    )
                ),
                paginationToken = null
            )
        )
    }
}
