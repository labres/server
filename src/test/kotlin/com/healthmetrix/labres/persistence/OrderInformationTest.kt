package com.healthmetrix.labres.persistence

import com.healthmetrix.labres.order.OrderNumber
import com.healthmetrix.labres.order.Status
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OrderInformationTest {
    private val baseRawOrderInformation = RawOrderInformation(
        id = UUID.randomUUID(),
        externalOrderNumber = "0123456789",
        status = "NEGATIVE",
        issuedAt = Date.from(Instant.now()),
        reportedAt = null,
        notifiedAt = null,
        notificationId = null
    )

    @Test
    fun `cooking a valid raw order info without lab ID and ION is successful`() {
        assertThat(baseRawOrderInformation.cook()).isEqualTo(
            OrderInformation(
                id = baseRawOrderInformation.id!!,
                number = OrderNumber.External(baseRawOrderInformation.externalOrderNumber!!),
                status = Status.NEGATIVE,
                issuedAt = baseRawOrderInformation.issuedAt!!
            )
        )
    }

    @Test
    fun `cooking a raw order info without EON fails`() {
        val raw = baseRawOrderInformation.copy(
            externalOrderNumber = null
        )

        assertThat(raw.cook()).isNull()
    }
}
