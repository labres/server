package com.healthmetrix.labres.persistence

import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Sample
import com.healthmetrix.labres.order.Status
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OrderInformationTest {
    private val baseRawOrderInformation = RawOrderInformation(
        id = UUID.randomUUID(),
        issuerId = "labres",
        orderNumber = "0123456789",
        status = "NEGATIVE",
        issuedAt = Date.from(Instant.now()),
        sample = Sample.SALIVA.toString(),
        reportedAt = null,
        notifiedAt = null,
        notificationUrl = null
    )

    @Test
    fun `cooking a valid raw order info without lab ID and ION is successful`() {
        assertThat(baseRawOrderInformation.cook()).isEqualTo(
            OrderInformation(
                id = baseRawOrderInformation.id!!,
                orderNumber = OrderNumber.External.from(baseRawOrderInformation.orderNumber!!),
                status = Status.NEGATIVE,
                issuedAt = baseRawOrderInformation.issuedAt!!,
                sample = Sample.SALIVA
            )
        )
    }

    @Test
    fun `cooking a raw order info without EON fails`() {
        val raw = baseRawOrderInformation.copy(
            orderNumber = null
        )

        assertThat(raw.cook()).isNull()
    }
}
