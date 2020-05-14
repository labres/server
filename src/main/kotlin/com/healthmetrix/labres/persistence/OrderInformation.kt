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
    val orderNumber: OrderNumber,
    val status: Status,
    val issuedAt: Date,
    val labId: String? = null,
    val testSiteId: String? = null,
    val reportedAt: Date? = null,
    val notifiedAt: Date? = null,
    val notificationUrl: String? = null,
    val enteredLabAt: Date? = null,
    val testType: String? = null
) {
    internal fun raw() = RawOrderInformation(
        id = id,
        issuerId = orderNumber.issuerId,
        orderNumber = orderNumber.number,
        status = status.toString(),
        issuedAt = issuedAt,
        reportedAt = reportedAt,
        notifiedAt = notifiedAt,
        notificationUrl = notificationUrl,
        enteredLabAt = enteredLabAt,
        testType = testType,
        labId = labId,
        testSiteId = testSiteId
    )
}

// WARNING tableName is replaced dynamically
@DynamoDBTable(tableName = "order_information")
data class RawOrderInformation(
    @DynamoDBHashKey
    var id: UUID? = null,

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "orderNumberIndex")
    var issuerId: String? = null,

    @DynamoDBIndexRangeKey(globalSecondaryIndexName = "orderNumberIndex")
    var orderNumber: String? = null,

    @DynamoDBAttribute
    var status: String? = null,

    @DynamoDBAttribute
    var labId: String? = null,

    @DynamoDBAttribute
    var testSiteId: String? = null,

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
        // for smart casts
        val id = id
        val issuerId = issuerId
        val issuedAt = issuedAt

        val status = status?.let(Status.Companion::from)

        if (id == null) {
            logger.warn("Unable to cook: Attribute id must not be null")
            return null
        }

        if (issuerId == null) {
            logger.warn("Unable to cook $id: Attribute issuerId must not be null")
            return null
        }

        val orderNumber = orderNumber?.let { OrderNumber.from(issuerId, it) }
        if (orderNumber == null) {
            logger.warn("Unable to cook $id: Attribute orderNumber must not be null")
            return null
        }

        if (status == null) {
            logger.warn("Unable to cook $id: Attribute status must not be null")
            return null
        }

        if (issuedAt == null) {
            logger.warn("Unable to cook $id: Attribute createdAt must not be null")
            return null
        }

        return OrderInformation(
            id = id,
            orderNumber = orderNumber,
            status = status,
            testType = testType,
            labId = labId,
            testSiteId = testSiteId,
            notificationUrl = notificationUrl,
            issuedAt = issuedAt,
            reportedAt = reportedAt,
            notifiedAt = notifiedAt,
            enteredLabAt = enteredLabAt
        )
    }
}
