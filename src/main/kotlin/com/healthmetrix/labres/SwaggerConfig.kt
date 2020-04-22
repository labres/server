package com.healthmetrix.labres

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    @Bean
    fun api(): OpenAPI {
        return OpenAPI()
            .info(apiInfo())
    }

    private fun apiInfo(): Info {
        return Info()
            .title("lab-res API - Beta")
            .version("1")
            .description("LabRes provides an API to establish a direct connection between laboratory results and citizens.")
            .contact(Contact()
                .name("Healthmetrix GmbH")
                .url("https://www.labres.de")
                .email("admin@healthmetrix.com"))
    }
}
