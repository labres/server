package com.healthmetrix.labres.persistence

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey
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
    val issuedAt: Date,
    val labId: String? = null,
    val reportedAt: Date? = null,
    val notifiedAt: Date? = null,
    val notificationUrl: String? = null,
    val enteredLabAt: Date? = null,
    val testType: String? = null
) {
    internal fun raw() = RawOrderInformation(
        id = id,
        externalOrderNumber = number.eonOrNull(),
        status = status.toString(),
        issuedAt = issuedAt,
        reportedAt = reportedAt,
        notifiedAt = notifiedAt,
        notificationUrl = notificationUrl,
        enteredLabAt = enteredLabAt,
        testType = testType
    )
}

// WARNING tableName is replaced dynamically
@DynamoDBTable(tableName = "order_information")
data class RawOrderInformation(
    @DynamoDBHashKey
    var id: UUID? = null,

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "externalOrderNumberIndex")
    var externalOrderNumber: String? = null,

    @DynamoDBAttribute
    var status: String? = null,

    @DynamoDBAttribute
    var labId: String? = null,

    @DynamoDBAttribute
    var issuedAt: Date? = null,

    @DynamoDBAttribute
    var reportedAt: Date? = null,

    @DynamoDBAttribute
    var notifiedAt: Date? = null,

    @DynamoDBAttribute
    var notificationUrl: String? = null,

    @DynamoDBAttribute
    var enteredLabAt: Date? = null,

    @DynamoDBAttribute
    var testType: String? = null
) {
    fun cook(): OrderInformation? {
        val id = id

        val orderNumber = OrderNumber.External.from(externalOrderNumber)

        val status = status?.let(Status.Companion::from)

        val createdAt = issuedAt

        return if (id != null && orderNumber != null && status != null && createdAt != null)
            OrderInformation(
                id = id,
                number = orderNumber,
                status = status,
                issuedAt = createdAt,
                labId = labId,
                reportedAt = reportedAt,
                notifiedAt = notifiedAt,
                notificationUrl = notificationUrl,
                enteredLabAt = enteredLabAt
            )
        else {
            logger.warn("Unable to cook $id")
            null
        }
    }
}
