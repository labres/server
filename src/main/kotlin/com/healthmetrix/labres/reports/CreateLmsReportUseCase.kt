package com.healthmetrix.labres.reports

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.healthmetrix.labres.logger
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.stereotype.Service

@Service
class CreateLmsReportUseCase(
    private val repository: OrderInformationRepository,
    private val reportClientAuthenticator: ReportClientAuthenticator
) {

    operator fun invoke(
        client: String,
        params: ReportParameters,
        paginationToken: PaginationToken?,
        pageSize: Int?
    ): Result<LmsReport, Error> {
        val whitelistedTestSiteIds = reportClientAuthenticator.getWhitelistedTestSites(client)

        if (whitelistedTestSiteIds == null || whitelistedTestSiteIds.isEmpty())
            return Err(Error.FORBIDDEN)

        if (paginationToken != null && paginationToken.reportParameters != params)
            return Err(Error.BAD_REQUEST)

        val page = repository.scanForTestSiteAndEvent(
            whitelistedTestSiteIds,
            params.event,
            params.reportedAfter,
            params.sampledAfter,
            paginationToken?.exclusiveStartKey,
            pageSize
        )

        return LmsReport(
            results = page.results.mapNotNull { order -> map(order, client) },
            paginationToken = page.lastEvaluatedKey?.let { key ->
                PaginationToken(
                    exclusiveStartKey = key,
                    reportParameters = params
                )
            }
        ).let(::Ok)
    }

    private fun map(order: OrderInformation, client: String): LmsTicket? {
        val event = order.metadata?.get("event")?.asText()
        val ticket = order.metadata?.get("ticket")?.asText()

        if (event == null) {
            logger.warn(
                "[{}]: Found order without event set for {} - ${order.id}",
                kv("method", "createLmsReport"),
                kv("client", client)
            )
            return null
        }

        if (ticket == null) {
            logger.warn(
                "[{}]: Found order without ticket set for {} - ${order.id}",
                kv("method", "createLmsReport"),
                kv("client", client)
            )
            return null
        }

        // Can never happen when AWS query works correctly
        if (order.reportedAt == null) {
            logger.error(
                "[{}]: Found order without reportedAt set for {} - ${order.id}",
                kv("method", "createLmsReport"),
                kv("client", client)
            )
            return null
        }

        val identifier = LmsTicketIdentifier(
            event = event,
            ticket = ticket
        )

        return LmsTicket(
            data = identifier,
            sampledAt = order.sampledAt,
            issuedAt = order.issuedAt.time,
            reportedAt = order.reportedAt.time
        )
    }

    enum class Error {
        FORBIDDEN, BAD_REQUEST
    }
}
