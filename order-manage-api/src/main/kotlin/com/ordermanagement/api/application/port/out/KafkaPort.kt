package com.ordermanagement.api.application.port.out

import com.ordermanagement.api.domain.entity.Order

interface KafkaPort {
    suspend fun publishInventoryUpdate(order: Order): KafkaPublishResult
}

data class KafkaPublishResult(val isSuccess: Boolean, val message: String? = null)
