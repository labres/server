package com.healthmetrix.labres

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.servers.ServerVariable
import io.swagger.v3.oas.models.servers.ServerVariables
import io.swagger.v3.oas.models.tags.Tag
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

const val EXTERNAL_ORDER_NUMBER_API_TAG = "External Order Number API"
const val PRE_ISSUED_ORDER_NUMBER_API_TAG = "Laboratory Issued Order Number API"
const val LABORATORY_API_TAG = "Laboratory API"

@Configuration
class SwaggerConfig(
    private val documentationInfo: DocumentationInfo
) {
    @Bean
    fun api(): OpenAPI {
        return OpenAPI()
            .info(documentationInfo.toApiInfo())
            .addTagsItem(labApiTag)
            .addTagsItem(externalOrderNumberApiTag)
            .servers(documentationInfo.toServers())
            .components(
                Components()
                    .addSecuritySchemes("OrdersApiToken", bearerSecurityScheme)
                    .addSecuritySchemes("LabCredential", basicAuthSecurityScheme)
            )
    }

    private val externalOrderNumberApiTag = Tag()
        .name(EXTERNAL_ORDER_NUMBER_API_TAG)
        .description("Endpoints to support External Order Numbers (EONs)")
    private val labApiTag = Tag()
        .name(LABORATORY_API_TAG)
        .description("Endpoints to be invoked by laboratories to report results")
    private val basicAuthSecurityScheme = SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("basic")
        .description("Base-64 encoded basic auth credential, the username of which is the Client ID used to authorize labs to upload results for an external or internal order number")
    private val bearerSecurityScheme = SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT")
        .description("validated by JWKS")
}

@ConfigurationProperties(prefix = "documentation-info")
@ConstructorBinding
data class DocumentationInfo(
    val title: String,
    val version: String,
    val description: String,
    val contact: ContactConfig,
    val servers: List<ServerConfig>
) {
    data class ContactConfig(
        val name: String,
        val url: String,
        val email: String
    )

    data class ServerConfig(
        val url: String,
        val description: String,
        val version: String
    )

    fun toServers(): List<Server> {
        return servers.map { server ->
            Server()
                .url(server.url)
                .description(server.description)
                .variables(
                    ServerVariables().addServerVariable("version", ServerVariable()._default(server.version))
                )
        }
    }

    fun toApiInfo(): Info {
        return Info()
            .title(title)
            .version(version)
            .description(description)
            .contact(
                Contact()
                    .url(contact.url)
                    .name(contact.name)
                    .email(contact.email)
            )
    }
}
