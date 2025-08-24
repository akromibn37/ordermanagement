package com.ordermanagement.dataapi.application.dto

data class OrderUpdateRequestDto(
        val id: Long,
        val orderNumber: Int,
        val customerId: Long,
        val createdAt: String,
        val updatedAt: String,
        val processedAt: String?,
        val lineItems: List<OrderLineItemDto>,
        val totalPrice: String,
        val currency: String
)

data class OrderLineItemDto(
        val id: Long,
        val productId: Long,
        val quantity: Int,
        val title: String,
        val sku: String,
        val price: String,
        val totalDiscount: String
)

data class OrderUpdateResponseDto(val isSuccess: Boolean, val message: String)
