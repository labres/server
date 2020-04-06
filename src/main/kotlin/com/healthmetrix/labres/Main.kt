package com.healthmetrix.labres

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import org.slf4j.event.Level

fun Application.d4lModule() {
    install(DefaultHeaders)
    install(CallLogging) {
        level = Level.INFO
    }

    routing {
        get("/hi") {
            call.respond("Nice")
        }
    }
}