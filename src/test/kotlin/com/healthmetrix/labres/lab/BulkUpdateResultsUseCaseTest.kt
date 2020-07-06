package com.healthmetrix.labres.lab

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BulkUpdateResultsUseCaseTest {

    private val updateResultUseCase: UpdateResultUseCase = mockk()
    private val metrics: LabMetrics = mockk(relaxed = true)

    private val labId = "test-lab"
    private val issuerId = "test-issuer"
    private val orderNumber = "1234567890"
    private val result = UpdateResultRequest(
        orderNumber = orderNumber,
        result = Result.POSITIVE
    )
    private val underTest = BulkUpdateResultsUseCase(updateResultUseCase, metrics)

    @BeforeEach
    internal fun setUp() {
        clearMocks(updateResultUseCase)
        every { updateResultUseCase.invoke(any(), any(), any(), any()) } returns UpdateResult.SUCCESS
    }

    @Test
    fun `it should return empty list of errors`() {
        val res = underTest(listOf(result), labId, issuerId)

        assertThat(res).isEmpty()
    }

    @Test
    fun `it should call updateResultUseCase for each result`() {
        underTest(listOf(result, result.copy(orderNumber = "0987654321")), labId, issuerId)

        verify(exactly = 2) { updateResultUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `it should return an error for each order number that could not be updated`() {
        val secondOrderNumber = "0987654321"

        every { updateResultUseCase(match { it.orderNumber == orderNumber }, any(), any(), any()) } returns UpdateResult.ORDER_NOT_FOUND
        every { updateResultUseCase(match { it.orderNumber == secondOrderNumber }, any(), any(), any()) } returns UpdateResult.INVALID_ORDER_NUMBER

        val res = underTest(
            listOf(
                result,
                result.copy(orderNumber = secondOrderNumber)
            ),
            labId,
            issuerId
        )

        assertThat(res).hasSize(2)
    }
}
