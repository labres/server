package com.healthmetrix.labres.persistence

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator
import com.amazonaws.services.dynamodbv2.model.Condition
import com.healthmetrix.labres.logger
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Sample
import com.healthmetrix.labres.order.Status
import net.logstash.logback.argument.StructuredArguments
import org.socialsignin.spring.data.dynamodb.repository.EnableScan
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@EnableScan
@Profile("dynamo")
internal interface RawOrderInformationRepository : CrudRepository<RawOrderInformation, UUID> {
    fun findByIssuerIdAndOrderNumber(issuerId: String, orderNumber: String): List<RawOrderInformation>
}

@Component
@Profile("dynamo")
class DynamoOrderInformationRepository internal constructor(
    private val repository: RawOrderInformationRepository,
    private val dynamoMapper: DynamoDBMapper
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
                StructuredArguments.kv("orderNumber", orderNumber.number),
                StructuredArguments.kv("issuerId", orderNumber.issuerId),
                StructuredArguments.kv("sample", sample)
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

    override fun scanForTestSiteAndEvent(
        testSiteIds: List<String>,
        event: String,
        reportedAfter: Long?,
        sampledAfter: Long?,
        exclusiveStartKey: String?,
        pageSize: Int?
    ): ScanResult {
        val filter = mutableMapOf(
            "status" to Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(AttributeValue().withS(Status.NEGATIVE.toString())),

            "testSiteId" to Condition()
                .withComparisonOperator(ComparisonOperator.IN)
                .withAttributeValueList(testSiteIds.map { AttributeValue().withS(it) })
        )

        if (sampledAfter != null)
            filter["sampledAt"] = Condition()
                .withComparisonOperator(ComparisonOperator.GT)
                .withAttributeValueList(AttributeValue().withN(sampledAfter.toString()))

        if (reportedAfter != null) {
            // DynamoDB can handle comparisons of datetimes as ISO strings
            val dateString = Instant.ofEpochMilli(reportedAfter).toString()
            filter["reportedAt"] = Condition()
                .withComparisonOperator(ComparisonOperator.GT)
                .withAttributeValueList(AttributeValue().withS(dateString))
        }

        return scan(emptyList(), event, filter, pageSize, exclusiveStartKey)
    }

    private fun scan(
        currentResults: List<OrderInformation>,
        event: String,
        filter: Map<String, Condition>,
        pageSize: Int?,
        exclusiveStartKey: String?
    ): ScanResult {
        val request = DynamoDBScanExpression().apply {
            withScanFilter(filter)

            if (exclusiveStartKey != null)
                withExclusiveStartKey(mapOf("id" to AttributeValue().withS(exclusiveStartKey)))
        }

        val response = dynamoMapper.scanPage(RawOrderInformation::class.java, request)
        val results = currentResults + response.results
            .mapNotNull(RawOrderInformation::cook)
            // though the AWS documentation shows examples of filtering nested JSON in scans and queries, I could neither get
            // that working in the SDK, nor in the CLI
            .filter { it.metadata?.get("event")?.asText() == event }

        val lastEvaluatedKey = response.lastEvaluatedKey?.get("id")?.s

        if (shouldNotFetchMoreResults(lastEvaluatedKey, pageSize, results) && resultsFitInOnePage(pageSize, results))
            return ScanResult(results, null)

        if (tooManyResultsForPage(pageSize, results)) {
            // pageSize can't be null due to condition in tooManyResultsForPage, but apparently the compiler doesn't know
            val returnedResults = results.subList(0, pageSize!!)
            return ScanResult(returnedResults, returnedResults.last().id.toString())
        }

        // this happens if more than 1MB data in dynamoDB has to be scanned
        return scan(results, event, filter, pageSize, lastEvaluatedKey)
    }

    private fun shouldNotFetchMoreResults(lastEvaluatedKey: String?, pageSize: Int?, results: List<OrderInformation>) =
        allResultsHaveBeenFetched(lastEvaluatedKey) || pageIsFull(pageSize, results)

    private fun allResultsHaveBeenFetched(lastEvaluatedKey: String?) = lastEvaluatedKey == null

    private fun pageIsFull(pageSize: Int?, results: List<OrderInformation>) =
        pageSize != null && pageSize == results.size

    private fun resultsFitInOnePage(pageSize: Int?, results: List<OrderInformation>) =
        pageSize == null || results.size <= pageSize

    private fun tooManyResultsForPage(pageSize: Int?, results: List<OrderInformation>) =
        pageSize != null && results.size > pageSize
}
