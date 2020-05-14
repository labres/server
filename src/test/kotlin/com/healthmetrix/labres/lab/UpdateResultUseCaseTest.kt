package com.healthmetrix.labres.lab

import com.healthmetrix.labres.notifications.NotifyOnStatusChangeUseCase
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import io.mockk.clearMocks
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
    private val notifier: NotifyOnStatusChangeUseCase = mockk()
    private val underTest = UpdateResultUseCase(orderInformationRepository, notifier)
    private val orderNumber = OrderNumber.External.from("1234567891")
    private val notificationUrl = "http://callMe.test"
    private val orderInfo = OrderInformation(
        id = UUID.randomUUID(),
        issuedAt = Date.from(Instant.now()),
        orderNumber = orderNumber,
        status = Status.IN_PROGRESS,
        notificationUrl = notificationUrl
    )
    private val labId = "labId"
    private val testType = "multipleChoice"
    private val now = Date.from(Instant.now())
    private val labResult = LabResult(orderNumber, labId, Result.POSITIVE, null)
    private val updated = orderInfo.copy(status = Status.POSITIVE, labId = labId)

    @Test
    fun `returns updated orderInfromation if successfully updated`() {
        every { orderInformationRepository.findByOrderNumber(any()) } returns orderInfo
        every { orderInformationRepository.save(any()) } returns updated
        every { notifier(any(), any()) } returns true

        assertThat(underTest(labResult)).isEqualTo(updated)
    }

    @Test
    fun `updates orderInformation with enteredLabAt set if status is IN_PROGRESS`() {
        clearMocks(orderInformationRepository)

        every { orderInformationRepository.findByOrderNumber(any()) } returns orderInfo
        val updated = updated.copy(status = Status.IN_PROGRESS, enteredLabAt = now)
        every { orderInformationRepository.save(any()) } returns updated
        every { notifier(any(), any()) } returns true

        underTest(labResult.copy(result = Result.IN_PROGRESS), now = now)

        verify(exactly = 1) { orderInformationRepository.save(updated) }
    }

    @Test
    fun `updates orderInformation with reportedAt set if status is not IN_PROGRESS`() {
        clearMocks(orderInformationRepository)

        every { orderInformationRepository.findByOrderNumber(any()) } returns orderInfo
        val updated = updated.copy(reportedAt = now, testType = testType)
        every { orderInformationRepository.save(any()) } returns updated
        every { notifier(any(), any()) } returns true

        underTest(labResult.copy(testType = testType), now = now)

        verify(exactly = 1) { orderInformationRepository.save(updated) }
    }

    @Test
    fun `notifies on updated order`() {
        clearMocks(notifier)

        every { orderInformationRepository.findByOrderNumber(any()) } returns orderInfo
        every { orderInformationRepository.save(any()) } returns updated
        every { notifier(any(), any()) } returns true

        underTest(labResult, now = now)

        verify(exactly = 1) { notifier.invoke(updated.id, notificationUrl) }
    }

    @Test
    fun `returns null if no orderNumber found`() {
        every { orderInformationRepository.findByOrderNumber(any()) } returns null
        assertThat(underTest(labResult)).isNull()
    }

    @Test
    fun `doesn't update or notify if no orderNumber found`() {
        clearMocks(orderInformationRepository)
        every { orderInformationRepository.findByOrderNumber(any()) } returns null

        underTest(labResult)

        verify(exactly = 0) {
            orderInformationRepository.save(any())
            notifier.invoke(any(), any())
        }
    }
}
