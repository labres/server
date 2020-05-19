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

    private val labId = "test-lab"
    private val issuerId = "test-issuer"
    private val orderNumber = "1234567890"
    private val result = JsonResult(
        orderNumber = orderNumber,
        result = Result.POSITIVE
    )
    private val underTest = BulkUpdateResultsUseCase(updateResultUseCase)

    @BeforeEach
    internal fun setUp() {
        clearMocks(updateResultUseCase)
        every { updateResultUseCase.invoke(any(), any()) } returns mockk()
    }

    @Test
    fun `it should return empty list of errors`() {
        val res = underTest(listOf(result), labId, issuerId)

        assertThat(res).isEmpty()
    }

    @Test
    fun `it should call updateResultUseCase for each result`() {
        val res = underTest(listOf(result, result.copy(orderNumber = "0987654321")), labId, issuerId)

        verify(exactly = 2) { updateResultUseCase(any(), any()) }
    }

    @Test
    fun `it should return an error for each order number that could not be parsed`() {
        val res = underTest(
            listOf(
                result,
                result.copy(orderNumber = "wrong"),
                result.copy(orderNumber = "also wrong")
            ),
            labId,
            null
        )

        assertThat(res).hasSize(2)
    }

    @Test
    fun `it should return an error for each order number that could not be updated`() {
        every { updateResultUseCase(any(), any()) } returns null

        val res = underTest(
            listOf(
                result,
                result.copy(orderNumber = "0987654321")
            ),
            labId,
            null
        )

        assertThat(res).hasSize(2)
    }
}
