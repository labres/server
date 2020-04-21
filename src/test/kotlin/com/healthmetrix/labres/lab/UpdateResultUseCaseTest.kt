package com.healthmetrix.labres.lab

import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UpdateResultUseCaseTest {
    private val orderInformationRepository: OrderInformationRepository = mockk()
    private val notifier: NotifyUseCase = mockk()
    private val underTest = UpdateResultUseCase(orderInformationRepository, notifier)
    private val orderNumber = OrderNumber.External.from("1234567891")!!
    private val orderInfo = OrderInformation(
        id = UUID.randomUUID(),
        issuedAt = Date.from(Instant.now()),
        number = orderNumber,
        status = Status.IN_PROGRESS
    )

    private val labResult = LabResult(orderNumber, "labId", Result.POSITIVE)

    @Test
    fun `returns Success result`() {
        every { orderInformationRepository.findByOrderNumber(any()) } returns orderInfo
        every { orderInformationRepository.save(any()) } returns orderInfo.copy(
            status = Status.POSITIVE,
            reportedAt = Date.from(Instant.now()),
            labId = "labId"
        )
        every { notifier(any()) } returns Unit
        assertThat(underTest(labResult)).isInstanceOf(UpdateStatusResponse.Success::class.java)
    }

    @Test
    fun `returns OrderNotFound if no orderNumber found`() {
        every { orderInformationRepository.findByOrderNumber(any()) } returns null
        assertThat(underTest(labResult)).isInstanceOf(UpdateStatusResponse.OrderNotFound::class.java)
    }

    @Test
    fun `it updates orderInfo with labId`() {
        every { orderInformationRepository.findByOrderNumber(any()) } returns orderInfo
        every { orderInformationRepository.save(any()) } returns orderInfo.copy(
            status = Status.POSITIVE,
            reportedAt = Date.from(Instant.now()),
            labId = "labId"
        )
        every { notifier(any()) } returns Unit

        val now = Date.from(Instant.now())
        underTest(labResult, now)
        verify {
            orderInformationRepository.save(
                orderInfo.copy(
                    status = Status.POSITIVE,
                    reportedAt = now,
                    labId = "labId"
                )
            )
        }
    }
}
