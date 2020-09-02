package com.healthmetrix.labres.persistence

import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Sample
import com.healthmetrix.labres.order.Status
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date
import java.util.HashMap
import java.util.UUID

@Component
@Profile("!dynamo")
class InMemoryOrderInformationRepository : OrderInformationRepository {

    private val map: HashMap<UUID, OrderInformation> = hashMapOf()

    override fun findById(id: UUID): OrderInformation? {
        return map.getOrDefault(id, null)
    }

    override fun findByOrderNumber(orderNumber: OrderNumber) =
        map.values.filter { it.orderNumber == orderNumber }

    override fun findByOrderNumberAndSample(orderNumber: OrderNumber, sample: Sample) =
        map.values.filter { it.orderNumber == orderNumber && it.sample == sample }

    override fun scanForTestSiteAndEvent(
        testSiteIds: List<String>,
        event: String,
        reportedAfter: Long?,
        sampledAfter: Long?,
        exclusiveStartKey: String?,
        pageSize: Int?
    ): ScanResult {
        val orders = if (exclusiveStartKey == null)
            map.values.sortedBy { it.issuedAt }
        else
            map.values
                .sortedBy { it.issuedAt }
                .dropWhile { it.id.toString() != exclusiveStartKey }
                .drop(1) // remove exclusive start key

        var results = orders
            .filter { order -> order.status == Status.NEGATIVE }
            .filter { order -> testSiteIds.contains(order.testSiteId) }
            .filter { order -> order.metadata?.get("event")?.asText() == event }

        if (sampledAfter != null)
            results = results.filter { order -> order.sampledAt != null && order.sampledAt > sampledAfter }

        if (reportedAfter != null) {
            val reportedAfterDate = Date.from(Instant.ofEpochMilli(reportedAfter))
            results = results.filter { order -> order.reportedAt != null && order.reportedAt.after(reportedAfterDate) }
        }

        return if (pageSize != null && results.size > pageSize) {
            results = results.subList(0, pageSize)
            ScanResult(
                results = results,
                lastEvaluatedKey = results.last().id.toString()
            )
        } else {
            ScanResult(
                results = results,
                lastEvaluatedKey = null
            )
        }
    }

    override fun migrate(migration: (RawOrderInformation) -> RawOrderInformation?) {
        map.values
            .map(OrderInformation::raw)
            .mapNotNull(migration)
            .mapNotNull(RawOrderInformation::cook)
            .forEach { order -> map[order.id] = order }
    }

    override fun save(orderInformation: OrderInformation): OrderInformation {
        map[orderInformation.id] = orderInformation
        return orderInformation
    }

    fun clear() = map.clear()
}
