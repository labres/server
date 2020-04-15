package com.healthmetrix.labres.persistence

import java.util.UUID
import org.socialsignin.spring.data.dynamodb.repository.EnableScan
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component

interface OrderInformationRepository {
    fun findById(id: UUID): OrderInformation?

    fun save(orderInformation: OrderInformation)

    fun findByExternalOrderNumber(externalOrderNumber: String): OrderInformation?
}

@EnableScan
internal interface RawOrderInformationRepository : CrudRepository<RawOrderInformation, String> {
    // spring magic
    fun findByExternalOrderNumber(externalOrderNumber: String): List<RawOrderInformation>
}

@Component
@Profile("dynamo")
class DynamoOrderInformationRepository internal constructor(
    private val rawOrderInformationRepository: RawOrderInformationRepository
) : OrderInformationRepository {
    override fun findById(id: UUID): OrderInformation? = rawOrderInformationRepository
        .findById(id.toString())
        .orElse(null)
        ?.cook()

    override fun findByExternalOrderNumber(externalOrderNumber: String): OrderInformation? {
        return rawOrderInformationRepository.findByExternalOrderNumber(externalOrderNumber)
            .mapNotNull(RawOrderInformation::cook)
            .let { l ->
                if (l.size == 1) {
                    l.first()
                } else null
            }
    }

    override fun save(orderInformation: OrderInformation) {
        rawOrderInformationRepository.save(orderInformation.raw())
    }
}

@Component
@Profile("!dynamo")
class InMemoryOrderInformationRepository : OrderInformationRepository {

    private val map: HashMap<UUID, OrderInformation> = hashMapOf()

    override fun findById(id: UUID): OrderInformation? {
        return map.getOrDefault(id, null)
    }

    override fun findByExternalOrderNumber(externalOrderNumber: String) = map.filter {
        it.value.number == OrderNumber.External.from(externalOrderNumber)
    }.entries.toList().let { l ->
        if (l.size == 1) l[0].value else null
    }

    override fun save(orderInformation: OrderInformation) {
        map[orderInformation.id] = orderInformation
    }
}
