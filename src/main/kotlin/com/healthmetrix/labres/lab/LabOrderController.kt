package com.healthmetrix.labres.lab

import com.healthmetrix.labres.ApiResponse
import com.healthmetrix.labres.OrderId
import com.healthmetrix.labres.asEntity
import com.healthmetrix.labres.order.Status
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class LabOrderController(
    private val updateLabOrderUseCase: UpdateLabOrderUseCase,
    private val getLabOrderUseCase: GetLabOrderUseCase
) {
    @PutMapping("/v1/lab-orders/{labOrderNumber}")
    fun updateLabOrder(@PathVariable labOrderNumber: String): ResponseEntity<UpdateLabOrderResponse> {
        val update = updateLabOrderUseCase(labOrderNumber)

        return UpdateLabOrderResponse.Updated(
            update.id,
            update.labOrderNumber
        ).asEntity()
    }

    @GetMapping("/v1/lab-orders/{orderId}")
    fun getLabOrder(@PathVariable orderId: String): ResponseEntity<GetLabOrderResponse> {
        val order = getLabOrderUseCase(orderId)
        if (order == null) {
            return GetLabOrderResponse.NotFound.asEntity()
        }

        return GetLabOrderResponse.Success(order.status).asEntity()
    }
}

sealed class UpdateLabOrderResponse(
    httpStatus: HttpStatus,
    hasBody: Boolean = true
) : ApiResponse(httpStatus, hasBody) {
    data class Updated(
        val id: OrderId,
        val labOrderNumber: String
    ) : UpdateLabOrderResponse(HttpStatus.OK)

    data class Created(
        val id: OrderId,
        val labOrderNumber: String
    ) : UpdateLabOrderResponse(HttpStatus.CREATED)
}

sealed class GetLabOrderResponse(httpStatus: HttpStatus, hasBody: Boolean = true) : ApiResponse(httpStatus, hasBody) {
    data class Success(val status: Status) : GetLabOrderResponse(HttpStatus.OK)
    object NotFound : GetLabOrderResponse(HttpStatus.NOT_FOUND)
}
