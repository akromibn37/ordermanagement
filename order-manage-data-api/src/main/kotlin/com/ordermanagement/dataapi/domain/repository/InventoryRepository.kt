package com.ordermanagement.dataapi.domain.repository

import com.ordermanagement.dataapi.domain.entity.Inventory

interface InventoryRepository {
    fun findById(id: Long): Inventory?
    fun findByProductId(productId: Long): Inventory?
    fun findBySku(sku: String): Inventory?
    fun save(inventory: Inventory): Inventory
}
