package com.healthmetrix.labres.order

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.healthmetrix.labres.persistence.OrderInformation
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.core.Is
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.util.Date
import java.util.UUID

class PreIssuedOrderNumberControllerTest {
    private val registerOrderUseCase: RegisterOrderUseCase = mockk()
    private val updateOrderUseCase: UpdateOrderUseCase = mockk()
    private val queryStatusUseCase: QueryStatusUseCase = mockk()
    private val metrics: OrderMetrics = mockk(relaxUnitFun = true)

    private val underTest =
        PreIssuedOrderNumberController(registerOrderUseCase, updateOrderUseCase, queryStatusUseCase, metrics)
    private val mockMvc = MockMvcBuilders.standaloneSetup(underTest).build()

    private val objectMapper = ObjectMapper().registerKotlinModule()

    private val orderId = UUID.randomUUID()
    private val orderNumberString = "1234567890"
    private val issuerId = "issuerA"
    private val orderNumber = OrderNumber.from(issuerId, orderNumberString)
    private val testSiteId = "testSiteA"
    private val notificationUrl = "http://callme.test"
    private val order = OrderInformation(
        id = orderId,
        orderNumber = orderNumber,
        status = Status.IN_PROGRESS,
        issuedAt = Date.from(Instant.now()),
        sample = Sample.SALIVA
    )

    @Nested
    inner class RegisterOrderEndpointTest {

        @BeforeEach
        internal fun setUp() {
            clearAllMocks()
        }

        @Test
        fun `registering a preissued order number returns status 201`() {
            every { registerOrderUseCase.invoke(any(), any(), any(), any(), any()) } returns Ok(order)

            val request = PreIssuedOrderNumberController.RegisterOrderRequest(
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
        fun `registering a preissued order number for kevb with more than 8 digits truncates the analyt`() {
            every { registerOrderUseCase.invoke(any(), any(), any(), any(), any()) } returns Ok(order)

            val request = PreIssuedOrderNumberController.RegisterOrderRequest(
                orderNumber = "0123456799",
                testSiteId = testSiteId,
                notificationUrl = notificationUrl
            )

            mockMvc.post("/v1/issuers/kevb/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }

            val correct = OrderNumber.from("kevb", "01234567")
            verify(exactly = 1) {
                registerOrderUseCase.invoke(eq(correct), any(), any(), any(), any())
            }
        }

        @Test
        fun `registering a preissued order number returns order number and id`() {
            every { registerOrderUseCase.invoke(any(), any(), any(), any(), any()) } returns Ok(order)

            val request = PreIssuedOrderNumberController.RegisterOrderRequest(
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

        @Test
        fun `registering a preissued order number returns 409 if it has already been registered before`() {
            every { registerOrderUseCase.invoke(any(), any(), any(), any(), any()) } returns Err("already exists")

            val request = PreIssuedOrderNumberController.RegisterOrderRequest(
                orderNumber = orderNumberString,
                testSiteId = null,
                notificationUrl = null
            )

            mockMvc.post("/v1/issuers/$issuerId/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isConflict }
            }
        }
    }

    @Nested
    inner class GetOrderNumberEndpointTest {

        @Test
        fun `querying the status of an order returns 200`() {
            every { queryStatusUseCase.invoke(any(), any()) } returns Status.POSITIVE

            mockMvc.get("/v1/issuers/$issuerId/orders/$orderId").andExpect {
                status { isOk }
            }
        }

        @Test
        fun `querying the status of an order returns status`() {
            every { queryStatusUseCase.invoke(any(), any()) } returns Status.POSITIVE

            mockMvc.get("/v1/issuers/$issuerId/orders/$orderId").andExpect {
                jsonPath("$.status", Is.`is`(Status.POSITIVE.toString()))
            }
        }

        @Test
        fun `returns status 400 when orderId is not a valid UUID`() {
            mockMvc.get("/v1/issuers/$issuerId/orders/invalid").andExpect {
                status { isBadRequest }
            }
        }

        @Test
        fun `returns status 404 when no order is found`() {
            every { queryStatusUseCase.invoke(any(), any()) } returns null
            mockMvc.get("/v1/issuers/$issuerId/orders/$orderId").andExpect {
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

            mockMvc.put("/v1/issuers/$issuerId/orders/$orderId") {
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
            mockMvc.put("/v1/issuers/$issuerId/orders/$orderId") {
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
            mockMvc.put("/v1/issuers/$issuerId/orders/invalid") {
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
