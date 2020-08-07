package com.healthmetrix.labres.order

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.unwrap
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Date
import java.util.UUID

internal class FindOrderByOrderNumberUseCaseTest {

    private val repository: OrderInformationRepository = mockk()

    private val underTest = FindOrderByOrderNumberUseCase(repository)

    private val orderNumber = OrderNumber.from(null, "1234567890")
    private val sample = Sample.SALIVA
    private val verificationSecret = UUID.randomUUID().toString()
    private val order: OrderInformation = OrderInformation(
        id = UUID.randomUUID(),
        orderNumber = orderNumber,
        status = Status.POSITIVE,
        issuedAt = Date.from(Instant.now()),
        sample = sample,
        verificationSecret = verificationSecret
    )

    @Test
    fun `it should return order`() {
        every { repository.findByOrderNumberAndSample(any(), any()) } returns listOf(order)

        val result = underTest(orderNumber, sample, verificationSecret)

        assertThat(result).isInstanceOf(Ok::class.java)
        assertThat(result.unwrap()).isEqualTo(order)
    }

    @Test
    fun `it should return the newest order when multiple are found`() {
        every {
            repository.findByOrderNumberAndSample(any(), any())
        } returns listOf(
            order,
            order.copy(id = UUID.randomUUID(), issuedAt = Date.from(Instant.now().minusSeconds(60)))
        )

        val result = underTest(orderNumber, sample, verificationSecret)

        assertThat(result.component1()).isNotNull
        assertThat(result.unwrap()).isEqualTo(order)
    }

    @Test
    fun `it should return NOT_FOUND if order could not be found`() {
        every { repository.findByOrderNumberAndSample(any(), any()) } returns emptyList()

        val result = underTest(orderNumber, sample, verificationSecret)

        assertThat(result).isInstanceOf(Err::class.java)
        result.onFailure { err ->
            assertThat(err).isEqualTo(FindOrderByOrderNumberUseCase.FindOrderError.NOT_FOUND)
        }
    }

    @Test
    fun `it should return FORBIDDEN if persisted order doesn't have a verificationSecret set`() {
        every { repository.findByOrderNumberAndSample(any(), any()) } returns listOf(
            order.copy(verificationSecret = null)
        )

        val result = underTest(orderNumber, sample, verificationSecret)

        assertThat(result).isInstanceOf(Err::class.java)
        result.onFailure { err ->
            assertThat(err).isEqualTo(FindOrderByOrderNumberUseCase.FindOrderError.FORBIDDEN)
        }
    }

    @Test
    fun `it should return FORBIDDEN if verificationSecret is not correct`() {
        every { repository.findByOrderNumberAndSample(any(), any()) } returns listOf(order)

        val result = underTest(orderNumber, sample, "wrong")

        assertThat(result).isInstanceOf(Err::class.java)
        result.onFailure { err ->
            assertThat(err).isEqualTo(FindOrderByOrderNumberUseCase.FindOrderError.FORBIDDEN)
        }
    }
}
