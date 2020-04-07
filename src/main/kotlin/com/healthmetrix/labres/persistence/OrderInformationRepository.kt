package com.healthmetrix.labres.persistence

import org.socialsignin.spring.data.dynamodb.repository.EnableScan
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component

@EnableScan
internal interface RawOrderInformationRepository : CrudRepository<RawOrderInformation, String>

@Component
class OrderInformationRepository internal constructor(
    private val rawOrderInformationRepository: RawOrderInformationRepository
) {
    fun findById(orderNumber: String): OrderInformation? = rawOrderInformationRepository
        .findById(orderNumber)
        .orElse(null)
        ?.cook()

    fun save(orderInformation: OrderInformation) {
        rawOrderInformationRepository.save(orderInformation.raw())
    }
}
