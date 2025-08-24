package com.ordermanagement.dataapi.infrastructure.persistence

import com.ordermanagement.dataapi.domain.entity.Order
import com.ordermanagement.dataapi.domain.repository.OrderRepository
import com.ordermanagement.dataapi.infrastructure.persistence.mapper.OrderMapper
import com.ordermanagement.dataapi.infrastructure.persistence.repository.OrderJpaRepository
import org.springframework.stereotype.Repository

@Repository
class JpaOrderRepository(
        private val orderJpaRepository: OrderJpaRepository,
        private val orderMapper: OrderMapper
) : OrderRepository {

    override fun save(order: Order): Order {
        val orderEntity = orderMapper.mapOrderToEntity(order)
        val savedEntity = orderJpaRepository.save(orderEntity)
        return orderMapper.mapEntityToOrder(savedEntity)
    }

    override fun findById(id: Int): Order? {
        return orderJpaRepository.findById(id).map { orderMapper.mapEntityToOrder(it) }.orElse(null)
    }

    override fun findByOrderNumber(orderNumber: Int): Order? {
        return orderJpaRepository.findByOrderNumber(orderNumber)?.let {
            orderMapper.mapEntityToOrder(it)
        }
    }

    override fun existsByOrderNumber(orderNumber: Int): Boolean {
        return orderJpaRepository.existsByOrderNumber(orderNumber)
    }
}
