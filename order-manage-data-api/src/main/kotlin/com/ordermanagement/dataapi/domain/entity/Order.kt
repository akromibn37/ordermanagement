package com.ordermanagement.dataapi.domain.entity

import java.math.BigDecimal
import java.time.LocalDateTime

data class Order(
        val orderId: Int? = null,
        val orderNumber: Int,
        val customerId: String,
        var productTypeCount: Int,
        val totalPrice: String,
        val orderStatus: String,
        val createDate: LocalDateTime,
        val createBy: String,
        val updateDate: LocalDateTime,
        val updateBy: String,
        val orderDetails: MutableList<OrderDetail> = mutableListOf()
) {
    fun addOrderDetail(detail: OrderDetail) {
        orderDetails.add(detail)
        productTypeCount = orderDetails.size
    }

    fun calculateTotalPrice(): BigDecimal {
        return orderDetails.sumOf { detail ->
            BigDecimal(detail.price) * BigDecimal(detail.quantity)
        }
    }

    fun isCompleted(): Boolean {
        return orderStatus == "COMPLETED" || orderStatus == "SUCCESS"
    }

    fun canBeProcessed(): Boolean {
        return orderStatus == "PENDING" || orderStatus == "PROCESSING"
    }
}

data class OrderDetail(
        val orderId: Int? = null,
        val productId: Long,
        val price: String,
        val quantity: Int,
        val createDate: LocalDateTime,
        val createBy: String,
        val updateDate: LocalDateTime,
        val updateBy: String
)

data class Inventory(
        val productId: Long? = null,
        val sku: String,
        val productTitle: String,
        val productPrice: String,
        val currency: String,
        val availableQuantity: Int,
        val createDate: LocalDateTime,
        val createBy: String,
        val updateDate: LocalDateTime,
        val updateBy: String
) {
    fun hasAvailableStock(requestedQuantity: Int): Boolean {
        return availableQuantity >= requestedQuantity
    }

    fun canAllocate(requestedQuantity: Int): Boolean {
        return availableQuantity >= requestedQuantity
    }

    fun allocateStock(quantity: Int): Inventory {
        if (!canAllocate(quantity)) {
            throw IllegalStateException("Insufficient stock to allocate $quantity units")
        }
        return copy(
                availableQuantity = availableQuantity - quantity,
                updateDate = LocalDateTime.now()
        )
    }
}
