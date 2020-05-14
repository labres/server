package com.healthmetrix.labres.order

import com.fasterxml.jackson.databind.ObjectMapper
import com.healthmetrix.labres.LabResTestApplication
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.util.UUID
import org.hamcrest.core.Is
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
class PreIssuedOrderNumberControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var registerOrder: RegisterOrderUseCase

    @MockkBean
    private lateinit var updateOrderUseCase: UpdateOrderUseCase

    @MockkBean
    private lateinit var queryStatus: QueryStatusUseCase

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val orderId = UUID.randomUUID()
    private val orderNumberString = "1234567890"
    private val issuerId = "issuerA"
    private val orderNumber = OrderNumber.from(issuerId, orderNumberString)
    private val testSiteId = "testSiteA"
    private val notificationUrl = "http://callme.test"

    @Nested
    inner class RegisterOrderEndpointTest {
        @Test
        fun `registering a preissued order number returns status 201`() {
            every { registerOrder.invoke(any(), any(), any(), any()) } returns (orderId to orderNumber)

            val request = PreIssuedOrderNumberController.RegisterOrderRequestBody(
                orderNumber = orderNumberString,
                testSiteId = testSiteId,
                notificationUrl = notificationUrl
            )

            mockMvc.post("/v1/issuers/$issuerId/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated }
            }
        }

        @Test
        fun `registering a preissued order number returns order number and id`() {
            every { registerOrder.invoke(any(), any(), any(), any()) } returns (orderId to orderNumber)

            val request = PreIssuedOrderNumberController.RegisterOrderRequestBody(
                orderNumber = orderNumberString,
                testSiteId = null,
                notificationUrl = null
            )

            mockMvc.post("/v1/issuers/$issuerId/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                jsonPath("$.orderNumber") { isString }
                jsonPath("$.id") { isString }
            }
        }
    }

    @Nested
    inner class GetOrderNumberEndpointTest {

        @Test
        fun `querying the status of an order returns 200`() {
            every { queryStatus.invoke(any(), any()) } returns Status.POSITIVE

            mockMvc.get("/v1/orders/$orderId").andExpect {
                status { isOk }
            }
        }

        @Test
        fun `querying the status of an order returns status`() {
            every { queryStatus.invoke(any(), any()) } returns Status.POSITIVE

            mockMvc.get("/v1/orders/$orderId").andExpect {
                jsonPath("$.status", Is.`is`(Status.POSITIVE.toString()))
            }
        }

        @Test
        fun `returns status 400 when orderId is not a valid UUID`() {
            mockMvc.get("/v1/orders/lenotsoniceuuid").andExpect {
                status { isBadRequest }
            }
        }

        @Test
        fun `returns status 404 when no order is found`() {
            every { queryStatus.invoke(any(), any()) } returns null
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
            every { updateOrderUseCase(any(), any(), any()) } returns UpdateOrderUseCase.Result.SUCCESS

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
            every { updateOrderUseCase(any(), any(), any()) } returns UpdateOrderUseCase.Result.NOT_FOUND
            mockMvc.put("/v1/orders/$orderId") {
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
        fun `it returns 400 if the orderId is invalid`() {
            mockMvc.put("/v1/orders/nonexistentOrder") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "notificationUrl" to notificationUrl
                    )
                )
            }.andExpect {
                status { isBadRequest }
            }
        }
    }
}
