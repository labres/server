package com.healthmetrix.labres.persistence

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import com.healthmetrix.labres.lab.TestType
import com.healthmetrix.labres.logger
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Sample
import com.healthmetrix.labres.order.Status
import java.lang.IllegalArgumentException
import java.util.Date
import java.util.UUID

data class OrderInformation(
    val id: UUID,
    val orderNumber: OrderNumber,
    val status: Status,
    val issuedAt: Date,
    val sample: Sample,
    val labId: String? = null,
    val testSiteId: String? = null,
    val reportedAt: Date? = null,
    val notifiedAt: Date? = null,
    val notificationUrl: String? = null,
    val enteredLabAt: Date? = null,
    val testType: TestType? = null
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
        testType = testType?.toString(),
        labId = labId,
        testSiteId = testSiteId,
        sample = sample.toString()
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
    var testType: String? = null,

    @DynamoDBAttribute
    var sample: String? = null
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
            logger.warn("Unable to cook $id: Attribute issuedAt must not be null")
            return null
        }

        val cookedSample = try {
            sample?.let(Sample::valueOf)
        } catch (ex: IllegalArgumentException) {
            null
        }

        if (cookedSample == null) {
            logger.warn("Unable to cook $id: Attribute sample was ${sample ?: "null"} and could not be parsed")
            return null
        }

        val cookedTestType = try {
            cookTestType()
        } catch (ex: IllegalStateException) {
            logger.warn("Unable to cook $id: ${ex.message}")
            return null
        }

        return OrderInformation(
            id = id,
            orderNumber = orderNumber,
            status = status,
            testType = cookedTestType,
            labId = labId,
            testSiteId = testSiteId,
            notificationUrl = notificationUrl,
            issuedAt = issuedAt,
            reportedAt = reportedAt,
            notifiedAt = notifiedAt,
            enteredLabAt = enteredLabAt,
            sample = cookedSample
        )
    }

    private fun cookTestType(): TestType? {
        // for smart casts
        val rawTestType = testType

        if (testTypeShouldHaveBeenReported() && rawTestType == null) {
            throw IllegalStateException("Attribute testType must not be null when the lab has already updated the result")
        }

        if (rawTestType == null)
            return null

        return try {
            TestType.valueOf(rawTestType)
        } catch (ex: IllegalArgumentException) {
            throw IllegalStateException("'$rawTestType' could not be parsed to attribute testType")
        }
    }

    private fun testTypeShouldHaveBeenReported() =
        enteredLabAt != null || reportedAt != null
}
