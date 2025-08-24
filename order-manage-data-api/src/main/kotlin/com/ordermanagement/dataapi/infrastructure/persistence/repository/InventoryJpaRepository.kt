package com.ordermanagement.dataapi.infrastructure.persistence.repository

import com.ordermanagement.dataapi.infrastructure.persistence.entity.InventoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InventoryJpaRepository : JpaRepository<InventoryEntity, Long> {
    fun findByProductId(productId: Long): InventoryEntity?
    fun findBySku(sku: String): InventoryEntity?
}
