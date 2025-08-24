package com.ordermanagement.api.application.port.out

import com.ordermanagement.api.domain.entity.Order

interface OrderDataApiPort {
        suspend fun checkOrder(
                orderId: String,
                productIds: List<String>,
                quantities: List<Int>
        ): OrderCheckResult
        suspend fun updateOrder(order: Order): OrderUpdateResult
}

data class OrderCheckResult(
        val isContinue: Boolean,
        val description: String,
        val orderId: Int,
        val products: List<OrderProductInfo>
)

data class OrderProductInfo(
        val productId: Long,
        val sku: String,
        val title: String,
        val requestedQuantity: Int,
        val availableQuantity: Int,
        val remainQuantity: Int,
        val status: String
)

data class OrderUpdateResult(val isSuccess: Boolean, val message: String)
