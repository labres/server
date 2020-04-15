package com.healthmetrix.labres.persistence

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException
import com.healthmetrix.labres.logger
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment

@Configuration
@EnableDynamoDBRepositories(
    basePackages = ["com.healthmetrix.labres.persistence"],
    dynamoDBMapperConfigRef = "dynamoDBMapperConfig"
)
@Profile("dynamo")
class DynamoDbConfig {

    // WARNING this function must have this name
    @Bean
    fun amazonDynamoDB(
        env: Environment,
        @Value("\${dynamo.local-endpoint}")
        serviceEndpoint: String,
        config: DynamoDBMapperConfig
    ): AmazonDynamoDB {
        val useLocalDb = serviceEndpoint.isNotBlank()

        val db = AmazonDynamoDBClientBuilder.standard().apply {
            if (useLocalDb)
                withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(serviceEndpoint, "eu-central-1"))
        }.build()

        if (useLocalDb)
            db.ensureTable(RawOrderInformation::class.java, config)

        return db
    }

    private fun AmazonDynamoDB.ensureTable(clazz: Class<*>, config: DynamoDBMapperConfig) {
        val req = DynamoDBMapper(this, config)
            .generateCreateTableRequest(clazz)
            .apply {
                provisionedThroughput = ProvisionedThroughput(1, 1)
                globalSecondaryIndexes.forEach {
                    it.provisionedThroughput = ProvisionedThroughput(1, 1)
                }
            }

        try {
            createTable(req)
        } catch (ex: ResourceInUseException) {
            logger.info("Table already exists")
        }
    }

    @Bean
    fun dynamoDBMapperConfig(
        @Value("\${dynamo.table-name}")
        tableName: String
    ): DynamoDBMapperConfig = DynamoDBMapperConfig.Builder().apply {
        tableNameOverride = DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement(tableName)
    }.build()
}
