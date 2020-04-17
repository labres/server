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
        labId = "labId",
        internalOrderNumber = "internalOrderNumber",
        status = "NEGATIVE",
        createdAt = Date.from(Instant.now()),
        updatedAt = null
    )

    @Test
    fun `cooking a valid raw order info without lab ID and ION is successful`() {
        val raw = baseRawOrderInformation.copy(labId = null, internalOrderNumber = null)

        assertThat(raw.cook()).isEqualTo(
            OrderInformation(
                id = baseRawOrderInformation.id!!,
                number = OrderNumber.External(baseRawOrderInformation.externalOrderNumber!!),
                status = Status.NEGATIVE,
                createdAt = baseRawOrderInformation.createdAt!!,
                updatedAt = null
            )
        )
    }

    @Test
    fun `cooking a valid raw order info without EON is successful`() {
        val raw = baseRawOrderInformation.copy(externalOrderNumber = null)

        assertThat(raw.cook()).isEqualTo(
            OrderInformation(
                id = baseRawOrderInformation.id!!,
                number = OrderNumber.Internal.from(
                    baseRawOrderInformation.labId,
                    baseRawOrderInformation.internalOrderNumber
                )!!,
                status = Status.NEGATIVE,
                createdAt = baseRawOrderInformation.createdAt!!,
                updatedAt = null
            )
        )
    }

    @Test
    fun `cooking a raw order info without either EON or ION fails`() {
        val raw = baseRawOrderInformation.copy(
            externalOrderNumber = null,
            labId = null,
            internalOrderNumber = null
        )

        assertThat(raw.cook()).isNull()
    }
}
