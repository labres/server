package com.healthmetrix.labres.lab

import com.healthmetrix.labres.LabResTestApplication
import com.healthmetrix.labres.OrderId
import com.healthmetrix.labres.order.Status
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put

@SpringBootTest(
    classes = [LabResTestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
class LabOrderControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var updateLabOrderUseCase: UpdateLabOrderUseCase

    @MockkBean
    private lateinit var getLabOrderUseCase: GetLabOrderUseCase

    @Nested
    inner class UpdateLabOrderEndpointTest {
        private var labOrderNumber = "abc123"

        @BeforeEach
        fun setup() {
            every { updateLabOrderUseCase(any(), any()) } returns UpdateLabOrderUseCase.Result.Created(
                OrderId.randomUUID(),
                labOrderNumber
            )
        }

        @Test
        fun `it responds with 200 when updating lab order`() {
            mockMvc.put("/v1/lab-orders/$labOrderNumber") {
                contentType = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk }
            }
        }

        @Test
        fun `success response body contains all necessary fields`() {
            mockMvc.put("/v1/lab-orders/$labOrderNumber") {
                contentType = MediaType.APPLICATION_JSON
            }.andExpect {
                jsonPath("$.id") { isString }
                jsonPath("$.labOrderNumber") { isString }
            }
        }
    }

    @Nested
    inner class GetLabOrderEndpointTest {
        private val orderId = "abc123"

        @Test
        fun `it responds with 200 when lab order is found`() {
            every { getLabOrderUseCase(any()) } returns GetLabOrderUseCase.Result(Status.POSITIVE)

            mockMvc.get("/v1/lab-orders/$orderId").andExpect {
                status { isOk }
            }
        }

        @Test
        fun `it responds with status in body when lab order is found`() {
            every { getLabOrderUseCase(any()) } returns GetLabOrderUseCase.Result(Status.POSITIVE)

            mockMvc.get("/v1/lab-orders/$orderId").andExpect {
                jsonPath("$.status") { isString }
            }
        }

        @Test
        fun `it responds with 404 when lab order is not found`() {
            every { getLabOrderUseCase(any()) } returns null
            mockMvc.get("/v1/lab-orders/$orderId").andExpect {
                status { isNotFound }
            }
        }
    }
}
