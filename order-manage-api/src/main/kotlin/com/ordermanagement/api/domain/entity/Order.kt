package com.ordermanagement.api.domain.entity

import java.math.BigDecimal
import java.time.LocalDateTime

data class Order(
        val id: OrderId,
        val orderNumber: String,
        val customer: Customer,
        val lineItems: List<OrderLineItem>,
        val shippingAddress: Address,
        val billingAddress: Address,
        val totalPrice: Money,
        val subtotalPrice: Money,
        val totalTax: Money,
        val currency: Currency,
        val financialStatus: FinancialStatus,
        val fulfillmentStatus: FulfillmentStatus?,
        val tags: String,
        val note: String,
        val sourceName: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
        val processedAt: LocalDateTime?,
        val status: OrderStatus = OrderStatus.PENDING
) {
    fun isPaid(): Boolean {
        return financialStatus == FinancialStatus.PAID

    }

    fun markAsProcessing(): Order {
        return copy(status = OrderStatus.PROCESSING)
    }

    fun markAsFulfilled(): Order {
        return copy(status = OrderStatus.FULFILLED)
    }

    fun markAsError(): Order {
        return copy(status = OrderStatus.ERROR)
    }
}

data class OrderId(val value: Long) {
    companion object {
        fun generate(): OrderId = OrderId(System.currentTimeMillis())
    }
}

data class Customer(
        val id: Long,
        val email: String,
        val firstName: String,
        val lastName: String,
        val phone: String
)

data class OrderLineItem(
        val id: Long,
        val productId: Long,
        val variantId: Long,
        val quantity: Int,
        val title: String,
        val sku: String,
        val price: Money,
        val totalDiscount: Money,
        val variantTitle: String
)

data class Address(
        val firstName: String,
        val lastName: String,
        val address1: String,
        val city: String,
        val province: String,
        val country: String,
        val zip: String
)

data class Money(val amount: BigDecimal, val currency: Currency) {
    fun isPositive(): Boolean = amount > BigDecimal.ZERO
    fun isZero(): Boolean = amount == BigDecimal.ZERO
}

enum class Currency {
    USD,
    EUR,
    GBP,
    CAD
}

enum class FinancialStatus {
    PENDING,
    PAID,
    REFUNDED,
    PARTIALLY_REFUNDED
}

enum class FulfillmentStatus {
    UNFULFILLED,
    PARTIALLY_FULFILLED,
    FULFILLED
}

enum class OrderStatus {
    PENDING,
    PROCESSING,
    FULFILLED,
    CANCELLED,
    ERROR
}
