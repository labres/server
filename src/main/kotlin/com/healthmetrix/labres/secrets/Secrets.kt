package com.healthmetrix.labres.secrets

import com.amazonaws.secretsmanager.caching.SecretCache
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import com.healthmetrix.labres.logger
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

interface Secrets {
    fun get(id: String): String?
}

@Service
@Profile("secrets")
class AwsSecrets(private val secretCache: SecretCache = SecretCache()) : Secrets {
    override fun get(id: String): String? = try {
        secretCache.getSecretString(id)
            ?: throw ResourceNotFoundException("Failed to retrieve secret $id")
    } catch (ex: ResourceNotFoundException) {
        val message = "Failed to retrieve secret {}"
        logger.error(
            message,
            kv("secretId", id),
            ex
        )
        throw InternalError(ex)
    }
}

@Service
@Profile("!secrets")
class MockSecrets(private val mockSecrets: Map<String, String> = mapOf()) : Secrets {
    override fun get(id: String): String? = mockSecrets[id]
}
