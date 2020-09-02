package com.healthmetrix.labres.persistence

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
import com.amazonaws.services.dynamodbv2.datamodeling.ScanResultPage
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator
import com.amazonaws.services.dynamodbv2.model.Condition
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Sample
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.reports.LmsTicketIdentifier
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Date
import java.util.Optional
import java.util.UUID

internal class DynamoOrderInformationRepositoryTest {

    private val repository: RawOrderInformationRepository = mockk()
    private val dynamoMapper: DynamoDBMapper = mockk()
    private val underTest = DynamoOrderInformationRepository(
        repository = repository,
        dynamoMapper = dynamoMapper
    )

    private val id = UUID.randomUUID()
    private val rawOrderInformation: RawOrderInformation = mockk()
    private val secondRawOrderInformation: RawOrderInformation = mockk()
    private val orderInformation: OrderInformation = mockk()
    private val secondOrderInformation: OrderInformation = mockk()
    private val orderNumber: OrderNumber = mockk()

    @BeforeEach
    internal fun setUp() {
        every { rawOrderInformation.cook() } returns orderInformation
        every { orderInformation.raw() } returns rawOrderInformation
        every { secondRawOrderInformation.cook() } returns secondOrderInformation
        every { secondOrderInformation.raw() } returns secondRawOrderInformation
        every { orderNumber.issuerId } returns "issuer"
        every { orderNumber.number } returns "number"
    }

    @Test
    fun `findById should return orderInformation`() {
        every { repository.findById(any()) } returns Optional.of(rawOrderInformation)

        val result = underTest.findById(id)

        assertThat(result).isEqualTo(orderInformation)
    }

    @Test
    fun `findById should return null when there is no order`() {
        every { repository.findById(any()) } returns Optional.empty()

        val result = underTest.findById(id)

        assertThat(result).isNull()
    }

    @Test
    fun `findById should return null when cooking goes wrong`() {
        every { repository.findById(any()) } returns Optional.of(rawOrderInformation)
        every { rawOrderInformation.cook() } returns null

        val result = underTest.findById(id)

        assertThat(result).isNull()
    }

    @Test
    fun `findByOrderNumber should return an empty list when there are no orders`() {
        every { repository.findByIssuerIdAndOrderNumber(any(), any()) } returns emptyList()

        val result = underTest.findByOrderNumber(orderNumber)

        assertThat(result).isEmpty()
    }

    @Test
    fun `findByOrderNumber should return an empty list when cooking goes wrong`() {
        every { repository.findByIssuerIdAndOrderNumber(any(), any()) } returns listOf(rawOrderInformation)
        every { rawOrderInformation.cook() } returns null

        val result = underTest.findByOrderNumber(orderNumber)

        assertThat(result).isEmpty()
    }

    @Test
    fun `findByOrderNumber should return one orderInformation`() {
        every { repository.findByIssuerIdAndOrderNumber(any(), any()) } returns listOf(rawOrderInformation)

        val result = underTest.findByOrderNumber(orderNumber)

        assertThat(result).containsOnly(orderInformation)
    }

    @Test
    fun `findByOrderNumber should return two orderInformation`() {
        every { repository.findByIssuerIdAndOrderNumber(any(), any()) } returns listOf(
            rawOrderInformation,
            secondRawOrderInformation
        )

        val result = underTest.findByOrderNumber(orderNumber)

        assertThat(result).containsOnly(orderInformation, secondOrderInformation)
    }

    @Test
    fun `findByOrderNumberAndSample should return an empty list when there is no order with the according sample`() {
        every { orderInformation.sample } returns Sample.BLOOD
        every { repository.findByIssuerIdAndOrderNumber(any(), any()) } returns listOf(rawOrderInformation)

        val result = underTest.findByOrderNumberAndSample(orderNumber, Sample.SALIVA)

        assertThat(result).isEmpty()
    }

    @Test
    fun `findByOrderNumberAndSample should return one order`() {
        every { orderInformation.sample } returns Sample.SALIVA
        every { repository.findByIssuerIdAndOrderNumber(any(), any()) } returns listOf(rawOrderInformation)

        val result = underTest.findByOrderNumberAndSample(orderNumber, Sample.SALIVA)

        assertThat(result).isEqualTo(listOf(orderInformation))
    }

    @Test
    fun `findByOrderNumberAndSample should return the multiple orders`() {
        every { orderInformation.sample } returns Sample.SALIVA
        every { orderInformation.issuedAt } returns Date.from(Instant.now().minusSeconds(60))
        every { secondOrderInformation.sample } returns Sample.SALIVA
        every { secondOrderInformation.issuedAt } returns Date.from(Instant.now())
        every { repository.findByIssuerIdAndOrderNumber(any(), any()) } returns listOf(
            rawOrderInformation,
            secondRawOrderInformation
        )

        val result = underTest.findByOrderNumberAndSample(orderNumber, Sample.SALIVA)

        assertThat(result).isEqualTo(listOf(orderInformation, secondOrderInformation))
    }

    @Test
    fun `save should save to the database`() {
        every { repository.save(any<RawOrderInformation>()) } returns rawOrderInformation

        underTest.save(orderInformation)

        verify { repository.save(rawOrderInformation) }
    }

    @Test
    fun `save should return persisted orderInformation`() {
        every { repository.save(any<RawOrderInformation>()) } returns rawOrderInformation

        val result = underTest.save(orderInformation)

        assertThat(result).isEqualTo(orderInformation)
    }

    @Nested
    inner class ScanForTestSiteAndEvent {

        private val testSites = listOf("test_site_1", "test_site_2")
        private val event = "event"

        private val objectMapper = jacksonObjectMapper().registerKotlinModule()

        @Test
        fun `it should return a page of two results`() {
            val mockResult = createMockResult(
                listOf(
                    createRawOrderMock(event, UUID.randomUUID().toString()),
                    createRawOrderMock(event, UUID.randomUUID().toString())
                )
            )
            every { dynamoMapper.scanPage(RawOrderInformation::class.java, any()) } returns mockResult

            val res = underTest.scanForTestSiteAndEvent(
                testSiteIds = testSites,
                event = event,
                reportedAfter = null,
                sampledAfter = null,
                pageSize = null,
                exclusiveStartKey = null
            )

            assertThat(res.results).hasSize(2)
            assertThat(res.lastEvaluatedKey).isNull()
        }

        @Test
        fun `it should send a correct scan request with two filters`() {
            val scanRequest = slot<DynamoDBScanExpression>()
            val mockResult = createMockResult()

            every { dynamoMapper.scanPage(RawOrderInformation::class.java, capture(scanRequest)) } answers {
                assertThat(scanRequest.captured.scanFilter.entries).hasSize(2)
                assertThat(scanRequest.captured.scanFilter["status"]).isEqualTo(
                    Condition()
                        .withComparisonOperator(ComparisonOperator.EQ)
                        .withAttributeValueList(AttributeValue().withS(Status.NEGATIVE.toString()))
                )
                assertThat(scanRequest.captured.scanFilter["testSiteId"]).isEqualTo(
                    Condition()
                        .withComparisonOperator(ComparisonOperator.IN)
                        .withAttributeValueList(
                            AttributeValue().withS(testSites[0]),
                            AttributeValue().withS(testSites[1])
                        )
                )

                mockResult
            }

            underTest.scanForTestSiteAndEvent(
                testSiteIds = testSites,
                event = event,
                reportedAfter = null,
                sampledAfter = null,
                pageSize = null,
                exclusiveStartKey = null
            )
        }

        @Test
        fun `it should filter requests on event after the scan`() {
            val mockResult = createMockResult(
                listOf(
                    createRawOrderMock(event, UUID.randomUUID().toString()),
                    createRawOrderMock("another", UUID.randomUUID().toString())
                )
            )
            every { dynamoMapper.scanPage(RawOrderInformation::class.java, any()) } returns mockResult

            val res = underTest.scanForTestSiteAndEvent(
                testSiteIds = testSites,
                event = event,
                reportedAfter = null,
                sampledAfter = null,
                pageSize = null,
                exclusiveStartKey = null
            )

            assertThat(res.results).hasSize(1)
            assertThat(res.lastEvaluatedKey).isNull()
        }

        @Test
        fun `it should send a correct scan request with all optional filters`() {
            val scanRequest = slot<DynamoDBScanExpression>()
            val mockResult = createMockResult()
            val reportedAfter = Instant.now().toEpochMilli()
            val sampledAfter = Instant.now().minusSeconds(60).toEpochMilli()

            every { dynamoMapper.scanPage(RawOrderInformation::class.java, capture(scanRequest)) } answers {
                assertThat(scanRequest.captured.scanFilter.entries).hasSize(4)
                assertThat(scanRequest.captured.scanFilter["reportedAt"]).isEqualTo(
                    Condition()
                        .withComparisonOperator(ComparisonOperator.GT)
                        .withAttributeValueList(AttributeValue().withS(Instant.ofEpochMilli(reportedAfter).toString()))
                )

                assertThat(scanRequest.captured.scanFilter["sampledAt"]).isEqualTo(
                    Condition()
                        .withComparisonOperator(ComparisonOperator.GT)
                        .withAttributeValueList(AttributeValue().withN(sampledAfter.toString()))
                )

                mockResult
            }

            underTest.scanForTestSiteAndEvent(
                testSiteIds = testSites,
                event = event,
                reportedAfter = reportedAfter,
                sampledAfter = sampledAfter,
                pageSize = null,
                exclusiveStartKey = null
            )
        }

        @Test
        fun `it should return only one page of results even when dynamo returns more`() {
            val returnedOrders = (0..9).map { index ->
                createRawOrderMock(event, index.toString())
            }

            val mockResult = createMockResult(returnedOrders)
            every { dynamoMapper.scanPage(RawOrderInformation::class.java, any()) } returns mockResult

            val res = underTest.scanForTestSiteAndEvent(
                testSiteIds = testSites,
                event = event,
                reportedAfter = null,
                sampledAfter = null,
                pageSize = 5,
                exclusiveStartKey = null
            )

            assertThat(res.results).hasSize(5)
        }

        @Test
        fun `it should return a pagination token when there are more results than one page`() {
            val secondOrderId = UUID.randomUUID()
            val orderIds = listOf(
                UUID.randomUUID(),
                secondOrderId,
                UUID.randomUUID()
            )
            val returnedOrders = orderIds.map { id ->
                createRawOrderMock(event, UUID.randomUUID().toString(), id)
            }

            val mockResult = createMockResult(returnedOrders)
            every { dynamoMapper.scanPage(RawOrderInformation::class.java, any()) } returns mockResult

            val res = underTest.scanForTestSiteAndEvent(
                testSiteIds = testSites,
                event = event,
                reportedAfter = null,
                sampledAfter = null,
                pageSize = 2,
                exclusiveStartKey = null
            )

            assertThat(res.lastEvaluatedKey).isNotNull()
            assertThat(res.lastEvaluatedKey).isEqualTo(secondOrderId.toString())
        }

        @Test
        fun `it should send a request with exclusiveStartKey`() {
            val scanRequest = slot<DynamoDBScanExpression>()
            val mockResult = createMockResult()
            val lastEvaluatedKey = UUID.randomUUID().toString()

            every { dynamoMapper.scanPage(RawOrderInformation::class.java, capture(scanRequest)) } answers {
                val exclusiveStartKey = scanRequest.captured.exclusiveStartKey
                assertThat(exclusiveStartKey.entries).hasSize(1)
                assertThat(exclusiveStartKey["id"]?.s).isEqualTo(lastEvaluatedKey)

                mockResult
            }

            underTest.scanForTestSiteAndEvent(
                testSiteIds = testSites,
                event = event,
                reportedAfter = null,
                sampledAfter = null,
                pageSize = null,
                exclusiveStartKey = lastEvaluatedKey
            )
        }

        @Test
        fun `it should send a second scan request when more than 1MB data had to be scanned for one page`() {
            clearMocks(dynamoMapper)
            val firstOrders = (0..4).map { index ->
                createRawOrderMock(event, index.toString())
            }

            val secondOrders = (5..9).map { index ->
                createRawOrderMock(event, index.toString())
            }

            val firstMockResult = createMockResult(
                orders = firstOrders,
                lastId = UUID.randomUUID()
            )

            val secondMockResult = createMockResult(secondOrders)

            every { dynamoMapper.scanPage(RawOrderInformation::class.java, any()) } returnsMany listOf(
                firstMockResult,
                secondMockResult
            )

            val res = underTest.scanForTestSiteAndEvent(
                testSiteIds = testSites,
                event = event,
                reportedAfter = null,
                sampledAfter = null,
                pageSize = null,
                exclusiveStartKey = null
            )

            assertThat(res.results).hasSize(10)
            verify(exactly = 2) { dynamoMapper.scanPage(RawOrderInformation::class.java, any()) }
        }

        @Test
        fun `it should send only one scan request when returned orders fit exactly in one page`() {
            clearMocks(dynamoMapper)
            val returnedOrders = (0..4).map { index ->
                createRawOrderMock(event, index.toString())
            }

            val mockresult = createMockResult(
                orders = returnedOrders,
                lastId = UUID.randomUUID()
            )

            every { dynamoMapper.scanPage(RawOrderInformation::class.java, any()) } returns mockresult

            val res = underTest.scanForTestSiteAndEvent(
                testSiteIds = testSites,
                event = event,
                reportedAfter = null,
                sampledAfter = null,
                pageSize = 5,
                exclusiveStartKey = null
            )

            assertThat(res.results).hasSize(5)
            verify(exactly = 1) { dynamoMapper.scanPage(RawOrderInformation::class.java, any()) }
        }

        private fun createMockResult(
            orders: List<RawOrderInformation> = emptyList(),
            lastId: UUID? = null
        ): ScanResultPage<RawOrderInformation> = mockk() {
            every { results } returns orders
            every { lastEvaluatedKey } returns lastId?.let {
                mapOf("id" to AttributeValue().withS(it.toString()))
            }
        }

        private fun createRawOrderMock(
            event: String,
            ticket: String,
            orderId: UUID = UUID.randomUUID()
        ): RawOrderInformation = mockk() {
            every { cook() } returns mockk() {
                every { metadata } returns objectMapper.valueToTree<JsonNode>(LmsTicketIdentifier(event, ticket))
                every { id } returns orderId
            }
        }
    }
}
