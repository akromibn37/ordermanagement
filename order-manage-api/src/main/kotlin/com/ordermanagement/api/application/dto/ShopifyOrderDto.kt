package com.ordermanagement.api.application.dto

data class ShopifyOrderDto(
        val id: String,
        val orderNumber: String,
        val name: String,
        val email: String,
        val phone: String,
        val createdAt: String,
        val updatedAt: String,
        val processedAt: String?,
        val customer: ShopifyCustomerDto,
        val lineItems: List<ShopifyLineItemDto>,
        val shippingAddress: ShopifyAddressDto,
        val billingAddress: ShopifyAddressDto,
        val totalPrice: String,
        val subtotalPrice: String,
        val totalTax: String,
        val currency: String,
        val financialStatus: String,
        val fulfillmentStatus: String?,
        val tags: String,
        val note: String,
        val sourceName: String
)

data class ShopifyCustomerDto(
        val id: String,
        val email: String,
        val firstName: String,
        val lastName: String,
        val phone: String
)

data class ShopifyLineItemDto(
        val id: Long,
        val productId: Long,
        val variantId: Long,
        val quantity: Int,
        val title: String,
        val sku: String,
        val price: String,
        val totalDiscount: String,
        val variantTitle: String
)

data class ShopifyAddressDto(
        val firstName: String,
        val lastName: String,
        val address1: String,
        val city: String,
        val province: String,
        val country: String,
        val zip: String
)

data class ShopifyOrderResponseDto(val status: String, val message: String, val orderId: String)
