package com.healthmetrix.labres.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.healthmetrix.labres.LabResApplication
import com.healthmetrix.labres.order.PreIssuedOrderNumberController
import com.healthmetrix.labres.order.Sample
import com.healthmetrix.labres.order.Status
import com.healthmetrix.labres.persistence.InMemoryOrderInformationRepository
import com.healthmetrix.labres.persistence.OrderInformationRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest(
    classes = [LabResApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
class IosBugWorkaroundTest {
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

    private val orderNumber = "1234567890"

    @Test
    fun `should set issuerId to mvz if incoming issuerId is hpi`() {
        val incomingIssuerId = "hpi"
        val incomingTestSiteId = "something"
        val registeredResponse = registerOrder(incomingIssuerId, incomingTestSiteId)

        val result = repository.findById(registeredResponse.id)

        assertThat(result).isNotNull
        assertThat(result!!).matches {
            it.status == Status.IN_PROGRESS &&
                it.orderNumber.issuerId == "mvz" &&
                it.sample == Sample.SALIVA &&
                it.testSiteId == incomingIssuerId
        }
    }

    @Test
    fun `should set issuerId to mvz if incoming issuerId is wmt`() {
        val incomingIssuerId = "wmt"
        val incomingTestSiteId = "something"
        val registeredResponse = registerOrder(incomingIssuerId, incomingTestSiteId)

        val result = repository.findById(registeredResponse.id)

        assertThat(result).isNotNull
        assertThat(result!!).matches {
            it.status == Status.IN_PROGRESS &&
                it.orderNumber.issuerId == "mvz" &&
                it.sample == Sample.SALIVA &&
                it.testSiteId == incomingIssuerId
        }
    }

    private fun registerOrder(
        issuerId: String,
        testSiteId: String
    ): PreIssuedOrderNumberController.RegisterOrderResponse.Created {
        return mockMvc.post("/v1/issuers/$issuerId/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsBytes(
                mapOf(
                    "orderNumber" to orderNumber,
                    "sample" to Sample.SALIVA,
                    "testSiteId" to testSiteId
                )
            )
        }.andReturn().responseBody()
    }

    private inline fun <reified T> MvcResult.responseBody(): T {
        return objectMapper.readValue(response.contentAsString)
    }
}
