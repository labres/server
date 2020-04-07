package com.healthmetrix.labres.lab

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class LabController {

    @PutMapping("/v1/{externalOrderNumber}/result")
    fun putLabResult(@PathVariable externalOrderNumber: String, @RequestBody bytes: ByteArray) {
        // TODO dynamo and ldt
    }
}
