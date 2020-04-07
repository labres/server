package com.healthmetrix.labres.persistence

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableDynamoDBRepositories(basePackages = ["com.healthmetrix.labres.persistence"])
class DynamoDbConfig {
    @Bean
    fun amazonDynamoDB(): AmazonDynamoDB {
        // todo yaml config
        val db = AmazonDynamoDBClientBuilder.standard()
            .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "eu-central-1"))
            .build()

        val mapper = DynamoDBMapper(db)

        val createTableRequest = mapper.generateCreateTableRequest(RawOrderInformation::class.java)
        // probably not necessary
        createTableRequest.provisionedThroughput = ProvisionedThroughput(1, 1)

        try {
            db.createTable(createTableRequest)
        } catch (ex: ResourceInUseException) {
            // all good
        }

        return db
    }
}
