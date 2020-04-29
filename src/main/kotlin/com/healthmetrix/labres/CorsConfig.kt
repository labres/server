package com.healthmetrix.labres

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig(
    @Value("\${cors-domains}")
    private val allowedDomains: List<String>
) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/v1/orders/**")
            .allowedMethods("PUT", "GET", "POST")
            .allowedOrigins(*allowedDomains.toTypedArray())
    }
}
