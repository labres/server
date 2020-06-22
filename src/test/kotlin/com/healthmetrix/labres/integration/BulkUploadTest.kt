package com.healthmetrix.labres.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.healthmetrix.labres.LabResApplication
import com.healthmetrix.labres.encodeBase64
import com.healthmetrix.labres.lab.BulkUpdateResultRequest
import com.healthmetrix.labres.lab.Result
import com.healthmetrix.labres.lab.TestType
import com.healthmetrix.labres.lab.UpdateResultRequest
import com.healthmetrix.labres.order.ExternalOrderNumberController.IssueExternalOrderNumberResponse
import com.healthmetrix.labres.order.PreIssuedOrderNumberController
import com.healthmetrix.labres.order.Sample
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.persistence.InMemoryOrderInformationRepository
import com.healthmetrix.labres.persistence.OrderInformationRepository
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.core.Is
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest(
    classes = [LabResApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
class BulkUploadTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var repository: OrderInformationRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    internal fun setUp() {
        (repository as InMemoryOrderInformationRepository).clear()
    }

    private val labId = "test_lab"
    private val labIdHeader = "$labId:pass".encodeBase64()
    private val issuerId = "test_issuer"
    private val notificationUrl = "http://notify.test"

    @Test
    fun `bulk upload works for EONs`() {
        val responses = (0 until 3).map { issueEONWithNotificationUrl() }

        mockMvc.put("/v1/results/bulk") {
            contentType = MediaType.APPLICATION_JSON
            headers { setBasicAuth(labIdHeader) }
            content = objectMapper.writeValueAsBytes(
                BulkUpdateResultRequest(
                    responses.map { res ->
                        UpdateResultRequest(
                            orderNumber = res.orderNumber,
                            result = Result.POSITIVE
                        )
                    }
                )
            )
        }.andExpect {
            jsonPath("$.processedRows", Is.`is`(3))
        }

        responses.forEach { res ->
            val orderInfo = repository.findById(res.id)
            assertThat(orderInfo?.status).isEqualTo(Status.POSITIVE)
        }
    }

    @Test
    fun `bulk upload works for PONs`() {
        val responses = listOf(
            registerPON("1", Sample.SALIVA),
            registerPON("1", Sample.BLOOD),
            registerPON("2", Sample.SALIVA)
        )

        val updateResultRequests = listOf(
            UpdateResultRequest(
                orderNumber = "1",
                result = Result.POSITIVE
            ),
            UpdateResultRequest(
                orderNumber = "1",
                result = Result.POSITIVE,
                type = TestType.ANTIBODY
            ),
            UpdateResultRequest(
                orderNumber = "2",
                result = Result.POSITIVE,
                type = TestType.NGS
            )
        )

        mockMvc.put("/v1/results/bulk") {
            contentType = MediaType.APPLICATION_JSON
            headers { setBasicAuth(labIdHeader) }
            param("issuerId", issuerId)
            content = objectMapper.writeValueAsBytes(BulkUpdateResultRequest(updateResultRequests))
        }.andExpect {
            jsonPath("$.processedRows", Is.`is`(3))
        }

        responses.forEach { res ->
            val orderInfo = repository.findById(res.id)
            assertThat(orderInfo?.status).isEqualTo(Status.POSITIVE)
        }
    }

    private fun issueEONWithNotificationUrl(url: String? = notificationUrl) = mockMvc.post("/v1/orders") {
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsBytes(
            mapOf(
                "notificationUrl" to url
            )
        )
    }.andReturn().responseBody<IssueExternalOrderNumberResponse.Created>()

    private fun registerPON(orderNumber: String, sample: Sample) = mockMvc.post("/v1/issuers/$issuerId/orders") {
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsBytes(
            mapOf(
                "orderNumber" to orderNumber,
                "sample" to sample.toString()
            )
        )
    }.andReturn().responseBody<PreIssuedOrderNumberController.RegisterOrderResponse.Created>()

    private inline fun <reified T> MvcResult.responseBody(): T {
        return objectMapper.readValue(response.contentAsString)
    }
}
