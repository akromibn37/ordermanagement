package com.ordermanagement.streamprocess.application

import com.ordermanagement.streamprocess.domain.entity.InventoryUpdate

interface ProcessInventoryUpdatePort {
    suspend fun processInventoryUpdate(inventoryUpdate: InventoryUpdate): ProcessResult
}

data class ProcessResult(val isSuccess: Boolean, val message: String, val productId: Long? = null)
