package com.healthmetrix.labres.integration

import com.healthmetrix.labres.LabResApplication
import com.healthmetrix.labres.persistence.OrderInformationRepository
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest(
    classes = [LabResApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
    ]
)
@AutoConfigureMockMvc
@DirtiesContext
class LabResults {
    @Autowired
    private lateinit var mockMvc: MockMvc


    @Nested
    inner class LabResultsFlow {
        @Test
        fun `a lab result can be successfully updated`() {
            mockMvc.post("/v1/orders") {
                contentType = MediaType.APPLICATION_JSON
            }.andExpect {

            }
        }
    }
}