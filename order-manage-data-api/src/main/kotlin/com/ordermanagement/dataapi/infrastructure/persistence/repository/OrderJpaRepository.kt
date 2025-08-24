package com.ordermanagement.dataapi.infrastructure.persistence.repository

import com.ordermanagement.dataapi.infrastructure.persistence.entity.OrderEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderJpaRepository : JpaRepository<OrderEntity, Int> {
    fun findByOrderNumber(orderNumber: Int): OrderEntity?
    fun existsByOrderNumber(orderNumber: Int): Boolean
}
