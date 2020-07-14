package com.healthmetrix.labres.persistence

import com.healthmetrix.labres.logger
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Sample
import net.logstash.logback.argument.StructuredArguments.kv
import org.socialsignin.spring.data.dynamodb.repository.EnableScan
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import java.util.UUID

interface OrderInformationRepository {
    fun findById(id: UUID): OrderInformation?

    fun save(orderInformation: OrderInformation): OrderInformation

    fun findByOrderNumber(orderNumber: OrderNumber): List<OrderInformation>

    fun findByOrderNumberAndSample(orderNumber: OrderNumber, sample: Sample): List<OrderInformation>

    // TODO delete after successful migration
    fun migrate(migration: (RawOrderInformation) -> RawOrderInformation?)
}

@EnableScan
internal interface RawOrderInformationRepository : CrudRepository<RawOrderInformation, UUID> {
    fun findByIssuerIdAndOrderNumber(issuerId: String, orderNumber: String): List<RawOrderInformation>
}

@Component
@Profile("dynamo")
class DynamoOrderInformationRepository internal constructor(
    private val repository: RawOrderInformationRepository
) : OrderInformationRepository {
    override fun findById(id: UUID): OrderInformation? = repository
        .findById(id)
        .orElse(null)
        ?.cook()

    override fun findByOrderNumber(orderNumber: OrderNumber): List<OrderInformation> =
        repository.findByIssuerIdAndOrderNumber(orderNumber.issuerId, orderNumber.number)
            .mapNotNull(RawOrderInformation::cook)

    override fun findByOrderNumberAndSample(orderNumber: OrderNumber, sample: Sample): List<OrderInformation> {
        val existingOrders = findByOrderNumber(orderNumber).filter { it.sample == sample }

        if (existingOrders.size > 1) {
            logger.warn(
                "Conflict in finding order with orderNumber ${orderNumber.number}, issuerId ${orderNumber.issuerId} " +
                    "and sample type $sample: More than one result found",
                kv("orderNumber", orderNumber.number),
                kv("issuerId", orderNumber.issuerId),
                kv("sample", sample)
            )
        }

        return existingOrders
    }

    // TODO make it return nullable
    override fun save(orderInformation: OrderInformation) =
        repository.save(orderInformation.raw()).cook()!!

    override fun migrate(migration: (RawOrderInformation) -> RawOrderInformation?) {
        val numberOfRows = repository
            .findAll()
            .mapNotNull(migration)
            .map(repository::save)
            .count()

        logger.info("Database migration: $numberOfRows rows migrated")
    }
}

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
