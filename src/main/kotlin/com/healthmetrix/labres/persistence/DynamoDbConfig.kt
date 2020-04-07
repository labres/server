package com.healthmetrix.labres.persistence

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
@EnableDynamoDBRepositories(basePackages = ["com.healthmetrix.labres.persistence"])
class DynamoDbConfig {

    // WARNING this function must have this name
    @Bean
    fun amazonDynamoDB(
        env: Environment,
        @Value("\${dynamo.local-endpoint}")
        serviceEndpoint: String
    ): AmazonDynamoDB {
        val db = AmazonDynamoDBClientBuilder.standard().apply {
            if (!env.activeProfiles.contains("dynamo"))
                withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(serviceEndpoint, "eu-central-1"))
        }.build()

        db.ensureTable(RawOrderInformation::class.java)

        return db
    }

    private fun AmazonDynamoDB.ensureTable(clazz: Class<*>) {
        val req = DynamoDBMapper(this)
            .generateCreateTableRequest(clazz)
            .apply {
                provisionedThroughput = ProvisionedThroughput(1, 1)
            }

        try {
            createTable(req)
        } catch (ex: ResourceInUseException) {
            // todo, log?
        }
    }
}
