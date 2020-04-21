package com.healthmetrix.labres.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.healthmetrix.labres.LabResApplication
import com.healthmetrix.labres.encodeBase64
import com.healthmetrix.labres.order.CreateOrderResponse
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.order.StatusResponse
import com.healthmetrix.labres.persistence.OrderInformationRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest(
    classes = [LabResApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
class LabResultsTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var orderInformationRepository: OrderInformationRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val labIdHeader = "${"user:pass".encodeBase64()}"

    @Nested
    inner class LabResultsFlow {
        @Test
        fun `an orderInformation has IN_PROGRESS when first created`() {
            val createResponse = mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
            }.andReturn().responseBody<CreateOrderResponse.Created>()

            val orderInformation = orderInformationRepository.findById(createResponse.id)!!
            assertThat(orderInformation.status).isEqualTo(Status.IN_PROGRESS)
        }

        @Test
        fun `an orderInformation can be created with a notification id`() {
            val createResponse = mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "notificationId" to "abc123"
                    )
                )
            }.andReturn().responseBody<CreateOrderResponse.Created>()
            val orderInformation = orderInformationRepository.findById(createResponse.id)!!
            assertThat(orderInformation.notificationId).isEqualTo("abc123")
        }

        @Test
        fun `a lab result can be successfully created, fetched, and updated`() {
            val createResponse = mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
            }.andReturn().responseBody<CreateOrderResponse.Created>()

            val orderId = createResponse.id
            mockMvc.get("/v1/orders/$orderId")
                .andReturn().responseBody<StatusResponse.Found>()

            val orderNumber = createResponse.orderNumber
            mockMvc.put("/v1/results/json") {
                contentType = MediaType.APPLICATION_JSON
                headers { setBasicAuth(labIdHeader) }
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "orderNumber" to orderNumber,
                        "result" to "POSITIVE"
                    )
                )
            }

            val orderInformation = orderInformationRepository.findById(orderId)!!
            assertThat(orderInformation.status).isEqualTo(Status.POSITIVE)
        }

        @Test
        fun `updating results sets the lab id`() {
            val createResponse = mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "notificationId" to "beforeId"
                    )
                )
            }.andReturn().responseBody<CreateOrderResponse.Created>()

            val orderId = createResponse.id
            val orderNumber = createResponse.orderNumber
            val orderInformation = orderInformationRepository.findById(orderId)!!
            assertThat(orderInformation.labId).isNull()

            mockMvc.put("/v1/results/json") {
                contentType = MediaType.APPLICATION_JSON
                headers { setBasicAuth(labIdHeader) }
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "orderNumber" to orderNumber,
                        "result" to "POSITIVE"
                    )
                )
            }

            val updatedOrderInformation = orderInformationRepository.findById(orderId)!!
            assertThat(updatedOrderInformation.labId).isEqualTo("user")
        }
    }

    private inline fun <reified T> MvcResult.responseBody(): T {
        return objectMapper.readValue<T>(response.contentAsString)
    }
}
