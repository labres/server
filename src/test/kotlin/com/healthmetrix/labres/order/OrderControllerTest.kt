package com.healthmetrix.labres.order

import com.healthmetrix.labres.LabResTestApplication
import com.healthmetrix.labres.persistence.OrderInformationRepository
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
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

    @Test
    fun `asking for an order returns an order and 201`() {
        every { orderInformationRepository.findById(any()) } returns null
        every { orderInformationRepository.save(any()) } returns Unit

        mockMvc.post("/v1/order").andExpect {
            status { isCreated }
            jsonPath("$.externalOrderNumber") { exists() }
        }
    }
}
