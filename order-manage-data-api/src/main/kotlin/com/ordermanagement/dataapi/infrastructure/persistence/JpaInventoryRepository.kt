package com.ordermanagement.dataapi.infrastructure.persistence

import com.ordermanagement.dataapi.domain.entity.Inventory
import com.ordermanagement.dataapi.domain.repository.InventoryRepository
import com.ordermanagement.dataapi.infrastructure.persistence.mapper.InventoryMapper
import com.ordermanagement.dataapi.infrastructure.persistence.repository.InventoryJpaRepository
import org.springframework.stereotype.Repository

@Repository
class JpaInventoryRepository(
        private val inventoryJpaRepository: InventoryJpaRepository,
        private val inventoryMapper: InventoryMapper
) : InventoryRepository {

    override fun findById(id: Long): Inventory? {
        return inventoryJpaRepository
                .findById(id)
                .map { inventoryMapper.mapEntityToInventory(it) }
                .orElse(null)
    }

    override fun findByProductId(productId: Long): Inventory? {
        return inventoryJpaRepository.findByProductId(productId)?.let {
            inventoryMapper.mapEntityToInventory(it)
        }
    }

    override fun findBySku(sku: String): Inventory? {
        return inventoryJpaRepository.findBySku(sku)?.let {
            inventoryMapper.mapEntityToInventory(it)
        }
    }

    override fun save(inventory: Inventory): Inventory {
        val inventoryEntity = inventoryMapper.mapInventoryToEntity(inventory)
        val savedEntity = inventoryJpaRepository.save(inventoryEntity)
        return inventoryMapper.mapEntityToInventory(savedEntity)
    }
}
