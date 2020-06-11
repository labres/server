package com.healthmetrix.labres.lab

import com.healthmetrix.labres.notifications.NotifyOnStatusChangeUseCase
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Sample
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UpdateResultUseCaseTest {
    private val repository: OrderInformationRepository = mockk()
    private val notifier: NotifyOnStatusChangeUseCase = mockk()
    private val underTest = UpdateResultUseCase(repository, notifier)
    private val orderNumberString = "1234567891"
    private val orderNumber = OrderNumber.External.from(orderNumberString)
    private val notificationUrl = "http://callMe.test"
    private val orderInfo = OrderInformation(
        id = UUID.randomUUID(),
        issuedAt = Date.from(Instant.now()),
        orderNumber = orderNumber,
        status = Status.IN_PROGRESS,
        notificationUrl = notificationUrl,
        sample = Sample.SALIVA
    )
    private val labId = "labId"
    private val issuerId = "issuerId"
    private val testType = TestType.ANTIBODY
    private val now = Date.from(Instant.now())
    private val updateResultRequest = UpdateResultRequest(orderNumber.number, Result.POSITIVE, TestType.PCR)
    private val updated = orderInfo.copy(status = Status.POSITIVE, labId = labId, testType = TestType.PCR)

    @BeforeEach
    internal fun setUp() {
        mockkObject(OrderNumber)
        every { OrderNumber.from(any(), any()) } returns orderNumber
        every { OrderNumber.from(null, any()) } returns orderNumber
    }

    @AfterEach
    internal fun tearDown() {
        clearMocks(OrderNumber)
    }

    @Test
    fun `returns SUCCESS if successfully updated`() {
        every { repository.findByOrderNumberAndSample(any(), any()) } returns orderInfo
        every { repository.save(any()) } returns updated
        every { notifier.invoke(any(), any()) } returns true

        assertThat(underTest(updateResultRequest, labId, null)).isEqualTo(UpdateResult.SUCCESS)
    }

    @Test
    fun `updates orderInformation with enteredLabAt set if status is IN_PROGRESS`() {
        clearMocks(repository)

        every { repository.findByOrderNumberAndSample(any(), any()) } returns orderInfo
        val updated = updated.copy(status = Status.IN_PROGRESS, enteredLabAt = now)
        every { repository.save(any()) } returns updated
        every { notifier.invoke(any(), any()) } returns true

        underTest(updateResultRequest.copy(result = Result.IN_PROGRESS), labId, null, now = now)

        verify(exactly = 1) { repository.save(updated) }
    }

    @Test
    fun `notifies on updated order`() {
        clearMocks(notifier)

        every { repository.findByOrderNumberAndSample(any(), any()) } returns orderInfo
        every { repository.save(any()) } returns updated
        every { notifier.invoke(any(), any()) } returns true

        underTest(updateResultRequest, labId, issuerId, now = now)

        verify(exactly = 1) { notifier.invoke(orderInfo.id, notificationUrl) }
    }

    @Test
    fun `returns ORDER_NOT_FOUND if no orderNumber found`() {
        every { repository.findByOrderNumberAndSample(any(), any()) } returns null
        assertThat(underTest(updateResultRequest, labId, issuerId)).isEqualTo(UpdateResult.ORDER_NOT_FOUND)
    }

    @Test
    fun `doesn't update or notify if no orderNumber found`() {
        clearMocks(repository)
        every { repository.findByOrderNumberAndSample(any(), any()) } returns null

        underTest(updateResultRequest, labId, issuerId)

        verify(exactly = 0) {
            repository.save(any())
            notifier.invoke(any(), any())
        }
    }

    @Test
    fun `updates orderInformation with reportedAt set if status is not IN_PROGRESS`() {
        clearMocks(repository)

        every { repository.findByOrderNumberAndSample(any(), any()) } returns orderInfo
        val updated = updated.copy(reportedAt = now, testType = testType)
        every { repository.save(any()) } returns updated
        every { notifier.invoke(any(), any()) } returns true

        underTest(updateResultRequest.copy(type = testType), labId, null, now)

        verify(exactly = 1) { repository.save(updated) }
    }

    @Test
    fun `returns INVALID_ORDER_NUMBER if order number can't be parsed`() {
        every { repository.findByOrderNumberAndSample(any(), any()) } returns orderInfo
        every { repository.save(any()) } returns updated
        every { notifier.invoke(any(), any()) } returns true
        every { OrderNumber.from(any(), any()) } throws IllegalArgumentException()

        assertThat(
            underTest(
                updateResultRequest,
                labId,
                issuerId
            )
        ).isEqualTo(UpdateResult.INVALID_ORDER_NUMBER)
    }
}
