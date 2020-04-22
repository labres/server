package com.healthmetrix.labres

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig(
    private val documentationInfo: DocumentationInfo
) {
    @Bean
    fun api(): OpenAPI {
        return OpenAPI()
            .info(documentationInfo.toApiInfo())
    }
}

@ConfigurationProperties(prefix = "documentation-info")
@ConstructorBinding
data class DocumentationInfo(
    val title: String,
    val version: String,
    val description: String,
    val contact: DocumentationContact
) {
    data class DocumentationContact(
        val name: String,
        val url: String,
        val email: String
    )

    fun toApiInfo(): Info {
        return Info()
            .title(title)
            .version(version)
            .description(description)
            .contact(Contact()
                .url(contact.url)
                .name(contact.name)
                .email(contact.email))
    }
}
