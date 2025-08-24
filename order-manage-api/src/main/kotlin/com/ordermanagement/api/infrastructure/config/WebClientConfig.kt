package com.ordermanagement.api.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {

    @Bean
    fun webClient(): WebClient {
        return WebClient.builder()
                .codecs { configurer ->
                    configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE_BYTES)
                }
                .build()
    }

    companion object {
        private const val MAX_IN_MEMORY_SIZE_BYTES = 2 * 1024 * 1024 // 2MB
    }
}
