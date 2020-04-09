package com.healthmetrix.labres.persistence

import org.socialsignin.spring.data.dynamodb.repository.EnableScan
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component

interface OrderInformationRepository {
    fun findById(orderNumber: String): OrderInformation?

    fun save(orderInformation: OrderInformation)
}

@EnableScan
internal interface RawOrderInformationRepository : CrudRepository<RawOrderInformation, String>

@Component
@Profile("dynamo")
class DynamoOrderInformationRepository internal constructor(
    private val rawOrderInformationRepository: RawOrderInformationRepository
) : OrderInformationRepository {
    override fun findById(orderNumber: String): OrderInformation? = rawOrderInformationRepository
        .findById(orderNumber)
        .orElse(null)
        ?.cook()

    override fun save(orderInformation: OrderInformation) {
        rawOrderInformationRepository.save(orderInformation.raw())
    }
}

@Component
@Profile("!dynamo")
class InMemoryOrderInformationRepository : OrderInformationRepository {

    private val map: HashMap<String, OrderInformation> = hashMapOf()

    override fun findById(orderNumber: String): OrderInformation? {
        return map.getOrDefault(orderNumber, null)
    }

    override fun save(orderInformation: OrderInformation) {
        map[orderInformation.number.externalOrderNumber] = orderInformation
    }
}
