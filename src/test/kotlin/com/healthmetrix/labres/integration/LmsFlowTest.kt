package com.healthmetrix.labres.integration

import com.fasterxml.jackson.databind.JsonNode
import com.healthmetrix.labres.order.LmsTicketIdentifier
import com.healthmetrix.labres.order.Sample
import org.junit.jupiter.api.Test
import java.util.UUID

class LmsFlowTest : AbstractIntegrationTest() {
    private val sample = Sample.SALIVA

    private val ticket = LmsTicketIdentifier(event = "sp-1", ticket = UUID.randomUUID().toString())
    private val metadata: JsonNode by lazy { objectMapper.valueToTree<JsonNode>(ticket) }

    @Test
    fun `an orderInformation can be created with a notification url`() {
        val registeredResponse = registerOrder(
            orderNumber = orderNumber,
            issuerId = issuerId,
            testSiteId = testSiteId,
            notificationUrl = fcmNotificationUrl,
            sample = Sample.SALIVA,
            sampledAt = sampledAt,
            metadata = metadata
        )

        assertThatOrderHasBeenSaved(
            id = registeredResponse.id,
            orderNumber = orderNumber,
            issuerId = issuerId,
            testSiteId = testSiteId,
            sample = sample,
            sampledAt = sampledAt,
            metadata = metadata
        )
    }
}
