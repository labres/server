package com.healthmetrix.labres.order

import com.fasterxml.jackson.databind.ObjectMapper
import com.healthmetrix.labres.LabResTestApplication
import com.healthmetrix.labres.persistence.OrderInformation
import com.healthmetrix.labres.persistence.OrderInformationRepository
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest(
    classes = [LabResTestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
class OrderControolerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var orderInformationRepository: OrderInformationRepository

    @MockkBean
    private lateinit var updateOrderUseCase: UpdateOrderUseCase

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Nested
    inner class CreateOrderEndpointTest {
        @Test
        fun `asking for an order returns an order and 201`() {
            every { orderInformationRepository.findByOrderNumber(any()) } returns null
            every { orderInformationRepository.save(any()) } answers { this.value }

            mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isCreated }
                jsonPath("$.orderNumber") { isString }
                jsonPath("$.id") { isString }
            }
        }
    }

    @Nested
    inner class GetOrderNumberEndpointTest {
        private val orderId = UUID.randomUUID()
        private val orderNumber = "12345678"

        @Test
        fun `asking for an order number returns status with 200`() {
            every { orderInformationRepository.findById(any()) } returns OrderInformation(
                id = UUID.randomUUID(),
                number = OrderNumber.External(orderNumber),
                status = Status.POSITIVE,
                issuedAt = Date.from(Instant.now())
            )

            mockMvc.get("/v1/orders/$orderId").andExpect {
                status { isOk }
                jsonPath("$.status") { exists() }
            }
        }

        @Test
        fun `returns status 404 when no order is found`() {
            every { orderInformationRepository.findById(any()) } returns null
            mockMvc.get("/v1/orders/$orderId").andExpect {
                status { isNotFound }
            }
        }
    }

    @Nested
    inner class UpdateOrderEndpointTest {
        private val orderId = UUID.randomUUID()
        private val notificationUrl = "123"

        @Test
        fun `it updates an order with the notification url`() {
            every { updateOrderUseCase(any(), any()) } returns UpdateOrderUseCase.Result.SUCCESS

            mockMvc.put("/v1/orders/$orderId") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "notificationUrl" to notificationUrl
                    )
                )
            }.andExpect {
                status { isOk }
            }
        }

        @Test
        fun `it returns 404 if order cant be found`() {
            every { updateOrderUseCase(any(), any()) } returns UpdateOrderUseCase.Result.NOT_FOUND
            mockMvc.put("/v1/orders/nonexistentOrder") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "notificationUrl" to notificationUrl
                    )
                )
            }.andExpect {
                status { isNotFound }
            }
        }

        @Test
        fun `it returns 404 if invalid order id`() {
            every { updateOrderUseCase(any(), any()) } returns UpdateOrderUseCase.Result.INVALID_ORDER_ID
            mockMvc.put("/v1/orders/nonexistentOrder") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "notificationUrl" to notificationUrl
                    )
                )
            }.andExpect {
                status { isNotFound }
            }
        }
    }
}
