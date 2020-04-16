package com.healthmetrix.labres.order

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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest(
    classes = [LabResTestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var orderInformationRepository: OrderInformationRepository

    @Nested
    inner class CreateOrderEndpointTest {
        @Test
        fun `asking for an order returns an order and 201`() {
            every { orderInformationRepository.findByExternalOrderNumber(any()) } returns null
            every { orderInformationRepository.save(any()) } answers { this.value }

            mockMvc.post("/v1/orders").andExpect {
                status { isCreated }
                jsonPath("$.externalOrderNumber") { isString }
                jsonPath("$.id") { isString }
                jsonPath("$.token") { isString }
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
                UUID.randomUUID(),
                OrderNumber.External(orderNumber),
                Status.POSITIVE,
                Date.from(Instant.now()),
                null
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
}
