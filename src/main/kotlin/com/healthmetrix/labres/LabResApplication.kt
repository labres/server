package com.healthmetrix.labres

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties
class LabResApplication

fun main(args: Array<String>) {
    runApplication<LabResApplication>(*args)
}
