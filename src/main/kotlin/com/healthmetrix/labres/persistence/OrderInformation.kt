package com.healthmetrix.labres.persistence

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import com.healthmetrix.labres.order.ALPHABET
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Status

data class OrderInformation(
    val number: OrderNumber,
    val status: Status
) {
    internal fun raw() = RawOrderInformation(number.externalOrderNumber, status.toString())
}

@DynamoDBTable(tableName = "order_information")
data class RawOrderInformation(
    @DynamoDBHashKey
    var number: String? = null,
    @DynamoDBAttribute
    var status: String? = null
) {
    fun cook(): OrderInformation? {
        val orderNumber = number?.let { n ->
            if (n.matches(Regex("[$ALPHABET]{8}")))
                OrderNumber(n)
            else
                null
        }

        val status = status?.let {
            try {
                Status.valueOf(it)
            } catch (ex: Exception) {
                null
            }
        }

        return orderNumber?.let { n ->
            status?.let { s ->
                OrderInformation(n, s)
            }
        }
    }
}
