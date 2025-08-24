package com.ordermanagement.dataapi.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "orders")
data class OrderEntity(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val orderId: Int? = null,
        @Column(name = "order_number", unique = true, nullable = false) val orderNumber: Int,
        @Column(name = "customer_id", nullable = false, length = 20) val customerId: String,
        @Column(name = "product_type_count", nullable = false) val productTypeCount: Int,
        @Column(name = "total_price", nullable = false, length = 20) val totalPrice: String,
        @Column(name = "order_status", nullable = false, length = 10) val orderStatus: String,
        @Column(name = "create_date", nullable = false) val createDate: LocalDateTime,
        @Column(name = "create_by", nullable = false, length = 50) val createBy: String,
        @Column(name = "update_date", nullable = false) val updateDate: LocalDateTime,
        @Column(name = "update_by", nullable = false, length = 50) val updateBy: String,
        @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
        val orderDetails: MutableList<OrderDetailEntity> = mutableListOf()
)

@Entity
@Table(name = "order_detail")
data class OrderDetailEntity(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Int? = null,
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "order_id", nullable = false)
        val order: OrderEntity,
        @Column(name = "product_id", nullable = false) val productId: Long,
        @Column(name = "price", nullable = false, length = 20) val price: String,
        @Column(name = "quantity", nullable = false) val quantity: Int,
        @Column(name = "create_date", nullable = false) val createDate: LocalDateTime,
        @Column(name = "create_by", nullable = false, length = 50) val createBy: String,
        @Column(name = "update_date", nullable = false) val updateDate: LocalDateTime,
        @Column(name = "update_by", nullable = false, length = 50) val updateBy: String
)

@Entity
@Table(name = "inventory")
data class InventoryEntity(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val productId: Long? = null,
        @Column(name = "sku", unique = true, nullable = false, length = 20) val sku: String,
        @Column(name = "product_title", nullable = false, length = 10) val productTitle: String,
        @Column(name = "product_price", nullable = false, length = 20) val productPrice: String,
        @Column(name = "currency", nullable = false, length = 10) val currency: String,
        @Column(name = "available_quantity", nullable = false) val availableQuantity: Int,
        @Column(name = "create_date", nullable = false) val createDate: LocalDateTime,
        @Column(name = "create_by", nullable = false, length = 50) val createBy: String,
        @Column(name = "update_date", nullable = false) val updateDate: LocalDateTime,
        @Column(name = "update_by", nullable = false, length = 50) val updateBy: String
)
