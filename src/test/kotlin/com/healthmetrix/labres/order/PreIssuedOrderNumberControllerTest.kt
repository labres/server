package com.healthmetrix.labres.order

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
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
    private val findOrderByIdUseCase: FindOrderByIdUseCase = mockk()
    private val metrics: OrderMetrics = mockk(relaxUnitFun = true)

    private val underTest =
        PreIssuedOrderNumberController(registerOrderUseCase, updateOrderUseCase, findOrderByIdUseCase, metrics)
    private val mockMvc = MockMvcBuilders.standaloneSetup(underTest).build()

    private val objectMapper = ObjectMapper().registerKotlinModule()

    private val orderId = UUID.randomUUID()
    private val orderNumberString = "1234567890"
    private val issuerId = "issuerA"
    private val orderNumber = OrderNumber.from(issuerId, orderNumberString)
    private val testSiteId = "testSiteA"
    private val notificationUrl = "http://callme.test"
    private val sampledAt = 1596186947L
    private val metadata = JsonNodeFactory.instance.objectNode().apply {
        put("hello", "world")
    }
    private val order = OrderInformation(
        id = orderId,
        orderNumber = orderNumber,
        status = Status.IN_PROGRESS,
        issuedAt = Date.from(Instant.now()),
        sample = Sample.SALIVA
    )

    @BeforeEach
    internal fun setUp() {
        clearAllMocks()
        every {
            registerOrderUseCase.invoke(
                orderNumber = any(),
                testSiteId = any(),
                sample = any(),
                notificationUrl = any(),
                verificationSecret = any(),
                sampledAt = any(),
                metadata = any(),
                now = any()
            )
        } returns Ok(order)
    }

    @Nested
    inner class RegisterOrderEndpointTest {

        @Test
        fun `registering a preissued order number returns status 201`() {
            val request = PreIssuedOrderNumberController.RegisterOrderRequest(
                orderNumber = orderNumberString,
                testSiteId = null,
                notificationUrl = null
            )

            mockMvc.post("/v1/issuers/$issuerId/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated }
            }
        }

        @Test
        fun `registering a preissued order number with all optional parameters returns status 201`() {
            val request = PreIssuedOrderNumberController.RegisterOrderRequest(
                orderNumber = orderNumberString,
                testSiteId = testSiteId,
                notificationUrl = notificationUrl,
                sampledAt = sampledAt,
                metadata = metadata,
                sample = Sample.BLOOD
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
                registerOrderUseCase.invoke(
                    orderNumber = eq(correct),
                    testSiteId = any(),
                    sample = any(),
                    notificationUrl = any(),
                    verificationSecret = any(),
                    sampledAt = any(),
                    metadata = any(),
                    now = any()
                )
            }
        }

        @Test
        fun `registering a preissued order number returns order number and id`() {
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
        fun `registering a preissued order number with all optional parameters returns order number and id`() {
            val request = PreIssuedOrderNumberController.RegisterOrderRequest(
                orderNumber = orderNumberString,
                testSiteId = testSiteId,
                notificationUrl = notificationUrl,
                sampledAt = sampledAt,
                metadata = metadata,
                sample = Sample.BLOOD
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
            every {
                registerOrderUseCase.invoke(
                    orderNumber = any(),
                    testSiteId = any(),
                    sample = any(),
                    notificationUrl = any(),
                    verificationSecret = any(),
                    sampledAt = any(),
                    metadata = any(),
                    now = any()
                )
            } returns Err("already exists")

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
            every { findOrderByIdUseCase.invoke(any(), any()) } returns order.copy(status = Status.POSITIVE)

            mockMvc.get("/v1/issuers/$issuerId/orders/$orderId").andExpect {
                status { isOk }
            }
        }

        @Test
        fun `querying the status of an order returns status`() {
            every { findOrderByIdUseCase.invoke(any(), any()) } returns order.copy(status = Status.POSITIVE)

            mockMvc.get("/v1/issuers/$issuerId/orders/$orderId").andExpect {
                jsonPath("$.status", Is.`is`(Status.POSITIVE.toString()))
            }
        }

        @Test
        fun `querying the status of an order does not set sampledAt on the response`() {
            every { findOrderByIdUseCase.invoke(any(), any()) } returns order.copy(status = Status.POSITIVE, sampledAt = 1596186947L)

            mockMvc.get("/v1/issuers/$issuerId/orders/$orderId").andExpect {
                jsonPath("$.sampledAt") { doesNotExist() }
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
            every { findOrderByIdUseCase.invoke(any(), any()) } returns null
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

    @Nested
    inner class IosIssuerIdBugWorkaround {

        @Test
        fun `registering a preissued order number for for issuerId hpi should transform it to mvz`() {
            val issuerId = "hpi"

            val request = PreIssuedOrderNumberController.RegisterOrderRequest(
                orderNumber = "01234567",
                testSiteId = testSiteId,
                notificationUrl = notificationUrl
            )

            mockMvc.post("/v1/issuers/$issuerId/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }

            val expected = OrderNumber.from("mvz", "01234567")
            verify(exactly = 1) {
                registerOrderUseCase.invoke(
                    orderNumber = eq(expected),
                    testSiteId = any(),
                    sample = any(),
                    notificationUrl = any(),
                    verificationSecret = any(),
                    sampledAt = any(),
                    metadata = any(),
                    now = any()
                )
            }
        }

        @Test
        fun `registering a preissued order number for for issuerId wmt should transform it to mvz`() {
            val issuerId = "wmt"

            val request = PreIssuedOrderNumberController.RegisterOrderRequest(
                orderNumber = "01234567",
                testSiteId = testSiteId,
                notificationUrl = notificationUrl
            )

            mockMvc.post("/v1/issuers/$issuerId/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }

            val expected = OrderNumber.from("mvz", "01234567")
            verify(exactly = 1) {
                registerOrderUseCase.invoke(
                    orderNumber = eq(expected),
                    testSiteId = any(),
                    sample = any(),
                    notificationUrl = any(),
                    verificationSecret = any(),
                    sampledAt = any(),
                    metadata = any(),
                    now = any()
                )
            }
        }

        @Test
        fun `querying the status of an order for issuerId hpi should transform it to mvz`() {
            every { findOrderByIdUseCase.invoke(any(), any()) } returns order.copy(status = Status.POSITIVE)

            val issuerId = "hpi"

            mockMvc.get("/v1/issuers/$issuerId/orders/$orderId")

            verify(exactly = 1) {
                findOrderByIdUseCase.invoke(any(), "mvz")
            }
        }

        @Test
        fun `querying the status of an order for issuerId wmt should transform it to mvz`() {
            every { findOrderByIdUseCase.invoke(any(), any()) } returns order.copy(status = Status.POSITIVE)

            val issuerId = "wmt"

            mockMvc.get("/v1/issuers/$issuerId/orders/$orderId")

            verify(exactly = 1) {
                findOrderByIdUseCase.invoke(any(), "mvz")
            }
        }

        @Test
        fun `updating an order for issuerId hpi should transform it to mvz`() {
            every { updateOrderUseCase(any(), any(), any()) } returns UpdateOrderUseCase.Result.SUCCESS

            val issuerId = "hpi"

            mockMvc.put("/v1/issuers/$issuerId/orders/$orderId") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "notificationUrl" to notificationUrl
                    )
                )
            }

            verify(exactly = 1) {
                updateOrderUseCase.invoke(any(), "mvz", any())
            }
        }

        @Test
        fun `updating an order for issuerId wmt should transform it to mvz`() {
            every { updateOrderUseCase(any(), any(), any()) } returns UpdateOrderUseCase.Result.SUCCESS

            val issuerId = "wmt"

            mockMvc.put("/v1/issuers/$issuerId/orders/$orderId") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "notificationUrl" to notificationUrl
                    )
                )
            }

            verify(exactly = 1) {
                updateOrderUseCase.invoke(any(), "mvz", any())
            }
        }
    }
}
