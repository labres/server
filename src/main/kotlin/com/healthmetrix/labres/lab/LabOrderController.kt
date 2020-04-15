package com.healthmetrix.labres.lab

import com.healthmetrix.labres.ApiResponse
import com.healthmetrix.labres.OrderId
import com.healthmetrix.labres.asEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class LabOrderController(
    private val updateLabOrderUseCase: UpdateLabOrderUseCase
) {
    @PutMapping("/v1/lab-orders/{labOrderNumber}")
    fun updateLabOrderNumber(@PathVariable labOrderNumber: String): ResponseEntity<UpdateLabOrderResponse> {
        val update = updateLabOrderUseCase(labOrderNumber)

        return UpdateLabOrderResponse.Updated(
            update.id,
            update.labOrderNumber,
            "fake token"
        ).asEntity()
    }
}

sealed class UpdateLabOrderResponse(httpStatus: HttpStatus, hasBody: Boolean = true) :
    ApiResponse(httpStatus, hasBody) {
    data class Updated(
        val id: OrderId,
        val labOrderNumber: String,
        val token: String
    ) : UpdateLabOrderResponse(HttpStatus.OK)

    data class Created(
        val id: OrderId,
        val labOrderNumber: String,
        val token: String
    ) : UpdateLabOrderResponse(HttpStatus.CREATED)
}
