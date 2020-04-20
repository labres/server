package com.healthmetrix.labres.persistence

import com.healthmetrix.labres.order.OrderNumber
import java.util.UUID
import org.socialsignin.spring.data.dynamodb.repository.EnableScan
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component

interface OrderInformationRepository {
    fun findById(id: UUID): OrderInformation?

    fun save(orderInformation: OrderInformation): OrderInformation

    fun findByOrderNumber(orderNumber: OrderNumber): OrderInformation?
}

@EnableScan
internal interface RawOrderInformationRepository : CrudRepository<RawOrderInformation, UUID> {
    fun findByExternalOrderNumber(externalOrderNumber: String): List<RawOrderInformation>
}

@Component
@Profile("dynamo")
class DynamoOrderInformationRepository internal constructor(
    private val rawOrderInformationRepository: RawOrderInformationRepository
) : OrderInformationRepository {
    override fun findById(id: UUID): OrderInformation? = rawOrderInformationRepository
        .findById(id)
        .orElse(null)
        ?.cook()

    override fun findByOrderNumber(orderNumber: OrderNumber): OrderInformation? = when (orderNumber) {
        is OrderNumber.External -> rawOrderInformationRepository.findByExternalOrderNumber(orderNumber.eon())
    }.mapNotNull(RawOrderInformation::cook).singleOrNull()

    override fun save(orderInformation: OrderInformation) =
        rawOrderInformationRepository.save(orderInformation.raw()).cook()!!
}

@Component
@Profile("!dynamo")
class InMemoryOrderInformationRepository : OrderInformationRepository {

    private val map: HashMap<UUID, OrderInformation> = hashMapOf()

    override fun findById(id: UUID): OrderInformation? {
        return map.getOrDefault(id, null)
    }

    override fun findByOrderNumber(orderNumber: OrderNumber) = map.filter {
        it.value.number == orderNumber
    }.entries.singleOrNull()?.value

    override fun save(orderInformation: OrderInformation): OrderInformation {
        map[orderInformation.id] = orderInformation
        return orderInformation
    }
}
