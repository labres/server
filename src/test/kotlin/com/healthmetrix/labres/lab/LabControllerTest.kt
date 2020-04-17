package com.healthmetrix.labres.lab

import com.fasterxml.jackson.databind.ObjectMapper
import com.healthmetrix.labres.LabResTestApplication
import com.healthmetrix.labres.encodeBase64
import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.put

@SpringBootTest(
    classes = [LabResTestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
class LabControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var orderInformationRepository: OrderInformationRepository

    @MockkBean
    private lateinit var extractObxResultUseCase: ExtractObxResultUseCase

    private val now = Date.from(Instant.now())

    @BeforeEach
    fun beforeEach() {
        every { orderInformationRepository.save(any()) } answers { this.value }
    }

    @Test
    fun `uploading a valid json doc with an external order number returns 200`() {

        val orderNumber = OrderNumber.External.random()

        every { orderInformationRepository.findByOrderNumber(any()) } returns
                OrderInformation(UUID.randomUUID(), orderNumber, Status.IN_PROGRESS, now, null)

        mockMvc.put("/v1/orders/result/json") {
            header(HttpHeaders.AUTHORIZATION, "labId")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "externalOrderNumber" to "0123456789",
                    "result" to Status.NEGATIVE
                )
            )
        }.andExpect {
            status { isOk }
        }

        verify(exactly = 1) {
            orderInformationRepository.save(match {
                it.number == orderNumber &&
                        it.status == Status.NEGATIVE
            })
        }
    }

    @Test
    fun `upload a document to an unknown external order number returns 404`() {
        every { orderInformationRepository.findByOrderNumber(any()) } returns null
        mockMvc.put("/v1/orders/result/json") {
            contentType = MediaType.APPLICATION_JSON
            header(HttpHeaders.AUTHORIZATION, "labId")
            content = objectMapper.writeValueAsBytes(
                mapOf(
                    "externalOrderNumber" to "0123456789",
                    "result" to Status.POSITIVE
                )
            )
        }.andExpect {
            status { isNotFound }
        }
    }

    @Test
    fun `upload a document to an invalid external order number returns 404`() {
        every { orderInformationRepository.findByOrderNumber(any()) } returns null
        mockMvc.put("/v1/orders/result/json") {
            header(HttpHeaders.AUTHORIZATION, "labId")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsBytes(
                mapOf(
                    "externalOrderNumber" to "nonsense",
                    "result" to Status.POSITIVE
                )
            )
        }.andExpect {
            status { isNotFound }
        }
    }

    @Test
    fun `uploading a pretend OBX message returns 200`() {
        val orderNumber = OrderNumber.External.random()

        every { orderInformationRepository.findByOrderNumber(any()) } returns
                OrderInformation(UUID.randomUUID(), orderNumber, Status.IN_PROGRESS, now, null)
        every { extractObxResultUseCase(any(), any()) } returns
                LabResult(OrderNumber.External.random(), Result.NEGATIVE)

        mockMvc.put("/v1/orders/result/obx") {
            header(HttpHeaders.AUTHORIZATION, "Basic ${"user:pass".encodeBase64()}")
            contentType = MediaType.TEXT_PLAIN
            content = "NEGATIVE"
        }.andExpect {
            status { isOk }
        }

        verify(exactly = 1) {
            orderInformationRepository.save(match {
                it.number == orderNumber &&
                        it.status == Status.NEGATIVE
            })
        }
    }

    @Test
    fun `uploading an invalid OBX message returns 500`() {
        val orderNumber = OrderNumber.External.random()

        every { orderInformationRepository.findByOrderNumber(any()) } returns
                OrderInformation(UUID.randomUUID(), orderNumber, Status.IN_PROGRESS, now, null)
        every { extractObxResultUseCase(any(), "labId") } returns null

        mockMvc.put("/v1/orders/result/obx") {
            header(HttpHeaders.AUTHORIZATION, "Basic ${"user:pass".encodeBase64()}")
            contentType = MediaType.TEXT_PLAIN
            content = "NOT OBX"
        }.andExpect {
            status { isInternalServerError }
        }
    }
}
