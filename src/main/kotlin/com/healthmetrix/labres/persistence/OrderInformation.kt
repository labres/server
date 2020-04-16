package com.healthmetrix.labres.persistence

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import com.healthmetrix.labres.logger
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Status
import java.util.Date
import java.util.UUID

data class OrderInformation(
    val id: UUID,
    val number: OrderNumber,
    val status: Status,
    val createdAt: Date,
    val updatedAt: Date?
) {
    internal fun raw() = RawOrderInformation(
        id = id,
        externalOrderNumber = number.eon(),
        labId = number.labId(),
        internalOrderNumber = number.ion(),
        status = status.toString(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// WARNING tableName is replaced dynamically
@DynamoDBTable(tableName = "order_information")
data class RawOrderInformation(
    @DynamoDBHashKey
    var id: UUID? = null,

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "externalOrderNumberIndex")
    var externalOrderNumber: String? = null,

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "internalOrderNumberIndex")
    var labId: String? = null,

    @DynamoDBIndexRangeKey(globalSecondaryIndexName = "internalOrderNumberIndex")
    var internalOrderNumber: String? = null,

    @DynamoDBAttribute
    var status: String? = null,

    @DynamoDBAttribute
    var createdAt: Date? = null,

    @DynamoDBAttribute
    var updatedAt: Date? = null
) {
    fun cook(): OrderInformation? {
        val id = id

        val orderNumber = OrderNumber.External.from(externalOrderNumber)
            ?: OrderNumber.Internal.from(labId, internalOrderNumber)

        val status = status?.let(Status.Companion::from)

        val createdAt = createdAt

        return if (id != null && orderNumber != null && status != null && createdAt != null)
            OrderInformation(id, orderNumber, status, createdAt, updatedAt)
        else {
            logger.warn("Unable to cook $id")
            null
        }
    }
}
