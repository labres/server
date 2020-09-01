package com.healthmetrix.labres.reports

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.healthmetrix.labres.encodeBase64
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.format.support.FormattingConversionService
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.util.UUID

internal class ReportsControllerTest {
    private val objectMapper = jacksonObjectMapper().registerKotlinModule()

    private val event = "event"
    private val reportClient = "test_report_client"
    private val reportClientHeader = "$reportClient:pass".encodeBase64()

    private val paginationToken = PaginationToken(
        reportParameters = ReportParameters(
            event = event,
            reportedAfter = null,
            sampledAfter = null
        ),
        exclusiveStartKey = UUID.randomUUID().toString()
    )

    private val report = LmsReport(
        results = listOf(
            LmsTicket(
                data = LmsTicketIdentifier(
                    event = event,
                    ticket = UUID.randomUUID().toString()
                ),
                sampledAt = Instant.now().minusSeconds(120).toEpochMilli(),
                reportedAt = Instant.now().minusSeconds(60).toEpochMilli(),
                issuedAt = Instant.now().minusSeconds(90).toEpochMilli()
            ),
            LmsTicket(
                data = LmsTicketIdentifier(
                    event = event,
                    ticket = UUID.randomUUID().toString()
                ),
                sampledAt = Instant.now().minusSeconds(120).toEpochMilli(),
                reportedAt = Instant.now().minusSeconds(50).toEpochMilli(),
                issuedAt = Instant.now().minusSeconds(80).toEpochMilli()
            )
        ),
        paginationToken = paginationToken
    )

    private val createLmsReportUseCase: CreateLmsReportUseCase = mockk()

    private val paginationTokenString = "I'm a pagination token!"
    private val mockObjectMapper: ObjectMapper = mockk() {
        every { writeValueAsString(any()) } returns paginationTokenString
    }

    private val underTest = ReportsController(createLmsReportUseCase, mockObjectMapper)

    private val paginationTokenBase64Converter: PaginationTokenBase64Converter = mockk() {
        every { convert(any()) } returns paginationToken
    }

    private val mockMvc = MockMvcBuilders.standaloneSetup(underTest)
        .setConversionService(
            FormattingConversionService().apply {
                addConverter(paginationTokenBase64Converter)
            }
        )
        .build()

    @Test
    fun `it should return 200`() {
        every { createLmsReportUseCase.invoke(any(), any(), any(), any()) } returns Ok(
            report.copy(paginationToken = null)
        )

        mockMvc.get("/v1/reports/lms") {
            headers { setBasicAuth(reportClientHeader) }
            param("event", event)
        }.andExpect { status { isOk } }
    }

    @Test
    fun `it should return 200 with all optional parameters`() {
        every { createLmsReportUseCase.invoke(any(), any(), any(), any()) } returns Ok(
            report.copy(paginationToken = null)
        )

        mockMvc.get("/v1/reports/lms") {
            headers { setBasicAuth(reportClientHeader) }
            param("event", event)
            param("reportedAfter", Instant.now().toEpochMilli().toString())
            param("sampledAfter", Instant.now().toEpochMilli().toString())
            param("startKey", paginationTokenString.encodeBase64())
            param("pageSize", 50.toString())
        }.andExpect { status { isOk } }
    }

    @Test
    fun `it should return a correct report`() {
        every { createLmsReportUseCase.invoke(any(), any(), any(), any()) } returns Ok(
            report.copy(paginationToken = null)
        )

        val res = mockMvc.get("/v1/reports/lms") {
            headers { setBasicAuth(reportClientHeader) }
            param("event", event)
        }.andReturn().response.contentAsString
            .let { objectMapper.readValue(it, ReportsController.LmsReportResponse.Success::class.java) }

        assertThat(res).isEqualTo(
            ReportsController.LmsReportResponse.Success(
                results = report.results,
                nextKey = null
            )
        )
    }

    @Test
    fun `it should return a correct report with pagination token`() {
        every { createLmsReportUseCase.invoke(any(), any(), any(), any()) } returns Ok(report)

        val res = mockMvc.get("/v1/reports/lms") {
            headers { setBasicAuth(reportClientHeader) }
            param("event", event)
        }.andReturn().response.contentAsString
            .let { objectMapper.readValue(it, ReportsController.LmsReportResponse.Success::class.java) }

        assertThat(res).isEqualTo(
            ReportsController.LmsReportResponse.Success(
                results = report.results,
                nextKey = paginationTokenString.encodeBase64()
            )
        )
    }

    @Test
    fun `it should return 401 if basic auth header is not present`() {
        every { createLmsReportUseCase.invoke(any(), any(), any(), any()) } returns Ok(
            report.copy(paginationToken = null)
        )

        mockMvc.get("/v1/reports/lms") {
            param("event", event)
        }.andExpect { status { isUnauthorized } }
    }

    @Test
    fun `it should return 401 if basic auth header is not in a valid format`() {
        every { createLmsReportUseCase.invoke(any(), any(), any(), any()) } returns Ok(
            report.copy(paginationToken = null)
        )

        mockMvc.get("/v1/reports/lms") {
            headers { setBasicAuth("wrong") }
            param("event", event)
        }.andExpect { status { isUnauthorized } }
    }

    @Test
    fun `it should return 403 if basic auth use is not whitelisted`() {
        every {
            createLmsReportUseCase.invoke(any(), any(), any(), any())
        } returns Err(CreateLmsReportUseCase.Error.FORBIDDEN)

        mockMvc.get("/v1/reports/lms") {
            headers { setBasicAuth(reportClientHeader) }
            param("event", event)
        }.andExpect { status { isForbidden } }
    }

    @Test
    fun `it should return 400 if pagination token and report parameters don't match`() {
        every {
            createLmsReportUseCase.invoke(any(), any(), any(), any())
        } returns Err(CreateLmsReportUseCase.Error.BAD_REQUEST)

        mockMvc.get("/v1/reports/lms") {
            headers { setBasicAuth(reportClientHeader) }
            param("event", event)
            param("startKey", paginationTokenString)
        }.andExpect { status { isBadRequest } }
    }

    @Test
    fun `it should return 400 if pagination token can not be deserialized`() {
        every {
            paginationTokenBase64Converter.convert(any())
        } throws IllegalArgumentException("test")

        mockMvc.get("/v1/reports/lms") {
            headers { setBasicAuth(reportClientHeader) }
            param("event", event)
            param("startKey", paginationTokenString)
        }.andExpect { status { isBadRequest } }
    }
}
