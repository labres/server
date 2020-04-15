package com.healthmetrix.labres.persistence

import com.healthmetrix.labres.order.OrderNumber
import java.util.UUID
import org.socialsignin.spring.data.dynamodb.repository.EnableScan
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component

interface OrderInformationRepository {
    fun findById(id: UUID): OrderInformation?

    fun save(orderInformation: OrderInformation)

    fun findByExternalOrderNumber(externalOrderNumber: String): OrderInformation?

    fun findByLabIdAndInternalOrderNumber(labId: String, internalOrderNumber: String): OrderInformation?
}

@EnableScan
internal interface RawOrderInformationRepository : CrudRepository<RawOrderInformation, String> {
    fun findByExternalOrderNumber(externalOrderNumber: String): List<RawOrderInformation>

    fun findByLabIdAndInternalOrderNumber(labId: String, internalOrderNumber: String): List<RawOrderInformation>
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
                if (l.size == 1) l.first() else null
            }
    }

    override fun findByLabIdAndInternalOrderNumber(labId: String, internalOrderNumber: String): OrderInformation? {
        return rawOrderInformationRepository.findByLabIdAndInternalOrderNumber(labId, internalOrderNumber)
            .mapNotNull(RawOrderInformation::cook)
            .let { l ->
                if (l.size == 1) l.first() else null
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
        if (l.size == 1) l.first().value else null
    }

    override fun findByLabIdAndInternalOrderNumber(labId: String, internalOrderNumber: String): OrderInformation? {
        return map.filter {
            it.value.number == OrderNumber.Internal.from(labId, internalOrderNumber)
        }.entries.toList().let { l ->
            if (l.size == 1) l.first().value else null
        }
    }

    override fun save(orderInformation: OrderInformation) {
        map[orderInformation.id] = orderInformation
    }
}
