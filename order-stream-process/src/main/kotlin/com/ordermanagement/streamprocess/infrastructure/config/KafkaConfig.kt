package com.ordermanagement.streamprocess.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.support.serializer.JsonDeserializer

@Configuration
class KafkaConfig {

    @Bean
    fun objectMapper(): ObjectMapper {
        return createConfiguredObjectMapper()
    }

    @Bean
    fun jsonDeserializer(): JsonDeserializer<Any> {
        return createTrustedJsonDeserializer()
    }

    private fun createConfiguredObjectMapper(): ObjectMapper {
        return ObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    private fun createTrustedJsonDeserializer(): JsonDeserializer<Any> {
        val deserializer = JsonDeserializer<Any>()
        deserializer.addTrustedPackages(ALL_PACKAGES_WILDCARD)
        return deserializer
    }

    companion object {
        private const val ALL_PACKAGES_WILDCARD = "*"
    }
}
