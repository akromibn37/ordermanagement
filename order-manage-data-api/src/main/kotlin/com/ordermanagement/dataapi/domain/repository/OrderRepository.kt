package com.ordermanagement.dataapi.domain.repository

import com.ordermanagement.dataapi.domain.entity.Order

interface OrderRepository {
    fun save(order: Order): Order
    fun findById(id: Int): Order?
    fun findByOrderNumber(orderNumber: Int): Order?
    fun existsByOrderNumber(orderNumber: Int): Boolean
}
