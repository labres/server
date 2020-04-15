package com.healthmetrix.labres.lab

import com.fasterxml.jackson.databind.ObjectMapper
import com.healthmetrix.labres.LabResTestApplication
import com.healthmetrix.labres.OrderId
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
import org.springframework.test.web.servlet.put

@SpringBootTest(
    classes = [LabResTestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
class LabOrderControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var updateLabOrderUseCase: UpdateLabOrderUseCase

    @Nested
    inner class UpdateLabOrderEndpointTest {
        private var labOrderNumber = "abc123"

        @BeforeEach
        fun setup() {
            every { updateLabOrderUseCase(any()) } returns UpdateLabOrderUseCase.Result(
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
                jsonPath("$.token") { isString }
            }
        }
    }
}