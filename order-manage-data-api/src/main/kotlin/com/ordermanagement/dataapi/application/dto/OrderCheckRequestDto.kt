package com.ordermanagement.dataapi.application.dto

data class OrderCheckRequestDto(
        val orderId: String,
        val productIds: String, // Comma-separated product IDs
        val quantity: String // Comma-separated quantities
)

data class OrderCheckResponseDto(
        val isContinue: Boolean,
        val description: String,
        val orderId: Int,
        val products: List<OrderProductInfoDto>
)

data class OrderProductInfoDto(
        val productId: Long,
        val sku: String,
        val title: String,
        val requestedQuantity: Int,
        val availableQuantity: Int,
        val remainQuantity: Int,
        val status: String
)
