package com.healthmetrix.labres.persistence

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Status
import java.util.Date

data class OrderInformation(
    val number: OrderNumber,
    val status: Status,
    val hash: String?,
    val updatedAt: Date?
) {
    internal fun raw() = RawOrderInformation(
        number.externalOrderNumber,
        status.toString(),
        hash,
        updatedAt
    )
}

@DynamoDBTable(tableName = "order_information")
data class RawOrderInformation(
    @DynamoDBHashKey
    var number: String? = null,
    @DynamoDBAttribute
    var status: String? = null,
    @DynamoDBAttribute
    var hash: String? = null,
    @DynamoDBAttribute
    var updatedAt: Date? = null
) {
    fun cook(): OrderInformation? {
        val orderNumber = OrderNumber.from(number)

        val status = status?.let(Status.Companion::from)

        return orderNumber?.let { n ->
            status?.let { s ->
                OrderInformation(n, s, hash, updatedAt)
            }
        }
    }
}
