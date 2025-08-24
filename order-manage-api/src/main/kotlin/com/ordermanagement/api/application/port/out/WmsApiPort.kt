package com.ordermanagement.api.application.port.out

import com.ordermanagement.api.domain.entity.Order

interface WmsApiPort {
        suspend fun getProductInventory(productId: Long): WmsInventoryResponse
        suspend fun createFulfillmentOrder(order: Order): WmsFulfillmentResult
}

data class WmsInventoryResponse(
        val productId: Long,
        val availableQuantity: Int,
        val totalQuantity: Int,
        val reservedQuantity: Int = 0,
        val sku: String = "",
        val locationId: Long = 0,
        val lastUpdated: String = ""
)

data class WmsFulfillmentResult(
        val isSuccess: Boolean,
        val fulfillmentId: String? = null,
        val message: String? = null
)
