package com.healthmetrix.labres.persistence

import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Sample
import java.util.UUID

interface OrderInformationRepository {
    fun findById(id: UUID): OrderInformation?

    fun save(orderInformation: OrderInformation): OrderInformation

    fun findByOrderNumber(orderNumber: OrderNumber): List<OrderInformation>

    fun findByOrderNumberAndSample(orderNumber: OrderNumber, sample: Sample): List<OrderInformation>

    fun scanForTestSiteAndEvent(
        testSiteIds: List<String>,
        event: String,
        reportedAfter: Long?,
        sampledAfter: Long?,
        exclusiveStartKey: String?,
        pageSize: Int?
    ): ScanResult

    // TODO delete after successful migration
    fun migrate(migration: (RawOrderInformation) -> RawOrderInformation?)
}

data class ScanResult(val results: List<OrderInformation>, val lastEvaluatedKey: String?)
