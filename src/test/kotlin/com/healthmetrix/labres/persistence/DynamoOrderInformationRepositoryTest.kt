package com.healthmetrix.labres.persistence

import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Sample
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Date
import java.util.Optional
import java.util.UUID

internal class DynamoOrderInformationRepositoryTest {

    private val repository: RawOrderInformationRepository = mockk()
    private val underTest = DynamoOrderInformationRepository(repository)

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
}
