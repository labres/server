package com.healthmetrix.labres

import dagger.Component
import dagger.Module
import dagger.Provides
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import org.slf4j.event.Level

fun Application.d4lModule(component: D4LComponent) {
    install(DefaultHeaders)
    install(CallLogging) {
        level = Level.INFO
    }

    routing {
        post("/eons") {
            val eon = Eon.random()
            component.eonRepository().save(eon)
            call.respond(eon.value)
        }
    }
}

fun Application.d4lModule() {
    d4lModule(DaggerD4LComponent.builder().build())
}

@Component(modules = [EonModule::class])
abstract class D4LComponent {
    abstract fun eonRepository(): EonRepository
}

@Module
class EonModule {

    @Provides
    fun provideEonRepository(): EonRepository = InMemoryEonRepository()
}