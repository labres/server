package com.healthmetrix.labres.order

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.result.Ok
import com.healthmetrix.labres.persistence.OrderInformation
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.core.Is
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

class ExternalOrderNumberControllerTest {
    private val issueExternalOrderNumberUseCase: IssueExternalOrderNumberUseCase = mockk()
    private val updateOrderUseCase: UpdateOrderUseCase = mockk()
    private val findOrderUseCase: FindOrderUseCase = mockk()

    private val underTest = ExternalOrderNumberController(
        issueExternalOrderNumber = issueExternalOrderNumberUseCase,
        updateOrderUseCase = updateOrderUseCase,
        findOrderUseCase = findOrderUseCase,
        metrics = mockk(relaxed = true)
    )

    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val mockMvc = MockMvcBuilders.standaloneSetup(underTest).build()

    private val orderId = UUID.randomUUID()
    private val orderNumberString = "1234567890"
    private val orderNumber = OrderNumber.External.from(orderNumberString)
    private val order = OrderInformation(
        id = orderId,
        orderNumber = orderNumber,
        status = Status.IN_PROGRESS,
        issuedAt = Date.from(Instant.now()),
        sample = Sample.SALIVA
    )

    @Nested
    inner class CreateOrderEndpointWithoutNotificationUrlTest {
        @Test
        fun `issuing an external order number returns status 201`() {
            every { issueExternalOrderNumberUseCase.invoke(any(), any(), any()) } returns Ok(order)

            mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isCreated }
            }
        }

        @Test
        fun `issuing an external order number returns order number and id`() {
            every { issueExternalOrderNumberUseCase.invoke(any(), any(), any()) } returns Ok(order)

            mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
            }.andExpect {
                jsonPath("$.orderNumber") { isString }
                jsonPath("$.id") { isString }
            }
        }
    }

    @Nested
    inner class CreateOrderEndpointWithNotificationUrlTest {
        @Test
        fun `issuing an external order number returns status 201`() {
            every { issueExternalOrderNumberUseCase.invoke(any(), any(), any()) } returns Ok(order)

            val request = ExternalOrderNumberController.IssueExternalOrderNumberRequestBody(notificationUrl = "http://test.test")

            mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated }
            }
        }

        @Test
        fun `issuing an external order number returns order number and id`() {
            every { issueExternalOrderNumberUseCase.invoke(any(), any(), any()) } returns Ok(order)

            val request = ExternalOrderNumberController.IssueExternalOrderNumberRequestBody(notificationUrl = "http://test.test")

            mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                jsonPath("$.orderNumber") { isString }
                jsonPath("$.id") { isString }
            }
        }
    }

    @Nested
    inner class CreateOrderEndpointWithSalivaTest {
        @Test
        fun `issuing an external order number returns status 201`() {
            every { issueExternalOrderNumberUseCase.invoke(any(), any(), any()) } returns Ok(order)

            val request = ExternalOrderNumberController.IssueExternalOrderNumberRequestBody(notificationUrl = null, sample = Sample.SALIVA)

            mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated }
            }
        }

        @Test
        fun `issuing an external order number returns order number and id`() {
            every { issueExternalOrderNumberUseCase.invoke(any(), any(), any()) } returns Ok(order)

            val request = ExternalOrderNumberController.IssueExternalOrderNumberRequestBody(notificationUrl = null, sample = Sample.SALIVA)

            mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                jsonPath("$.orderNumber") { isString }
                jsonPath("$.id") { isString }
            }
        }
    }

    @Nested
    inner class CreateOrderEndpointWithBloodTest {
        @Test
        fun `issuing an external order number returns status 201`() {
            every { issueExternalOrderNumberUseCase.invoke(any(), any(), any()) } returns Ok(order)

            val request = ExternalOrderNumberController.IssueExternalOrderNumberRequestBody(notificationUrl = null, sample = Sample.BLOOD)

            mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated }
            }
        }

        @Test
        fun `issuing an external order number returns order number and id`() {
            every { issueExternalOrderNumberUseCase.invoke(any(), any(), any()) } returns Ok(order)

            val request = ExternalOrderNumberController.IssueExternalOrderNumberRequestBody(notificationUrl = null, sample = Sample.BLOOD)

            mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                jsonPath("$.orderNumber") { isString }
                jsonPath("$.id") { isString }
            }
        }
    }

    @Nested
    inner class CreateOrderWithVerificationSecretTest {

        val secret = UUID.randomUUID().toString()

        @Test
        fun `issuing an external order number returns status 201`() {
            every { issueExternalOrderNumberUseCase.invoke(any(), any(), any()) } returns Ok(order)

            val request = ExternalOrderNumberController.IssueExternalOrderNumberRequestBody(
                notificationUrl = null,
                verificationSecret = secret
            )

            mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated }
            }
        }

        @Test
        fun `issuing an external order number returns order number and id`() {
            every { issueExternalOrderNumberUseCase.invoke(any(), any(), any()) } returns Ok(order)

            val request = ExternalOrderNumberController.IssueExternalOrderNumberRequestBody(
                notificationUrl = null,
                verificationSecret = secret
            )

            mockMvc.post("/v1/orders") {
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
            every { findOrderUseCase.invoke(any(), any()) } returns order.copy(status = Status.POSITIVE)

            mockMvc.get("/v1/orders/$orderId").andExpect {
                status { isOk }
            }
        }

        @Test
        fun `querying the status of an order returns status`() {
            every { findOrderUseCase.invoke(any(), any()) } returns order.copy(status = Status.POSITIVE)

            mockMvc.get("/v1/orders/$orderId").andExpect {
                jsonPath("$.status", Is.`is`(Status.POSITIVE.toString()))
            }
        }

        @Test
        fun `querying the status of an order does not return sampledAt if it is not set in the database`() {
            every { findOrderUseCase.invoke(any(), any()) } returns order.copy(status = Status.POSITIVE)

            mockMvc.get("/v1/orders/$orderId").andExpect {
                jsonPath("$.sampledAt") { doesNotExist() }
            }
        }

        @Test
        fun `querying the status of an order returns sampledAt`() {
            val sampledAt: Long = 1596186947
            every { findOrderUseCase.invoke(any(), any()) } returns order.copy(status = Status.POSITIVE, sampledAt = sampledAt)

            mockMvc.get("/v1/orders/$orderId").andExpect {
                jsonPath("$.sampledAt", Is.`is`(sampledAt.toInt()))
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
            every { findOrderUseCase.invoke(any(), any()) } returns null
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
