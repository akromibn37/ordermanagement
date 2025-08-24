package com.ordermanagement.streamprocess.domain.entity

import com.ordermanagement.streamprocess.domain.valueobject.LocationId
import com.ordermanagement.streamprocess.domain.valueobject.ProductId
import com.ordermanagement.streamprocess.domain.valueobject.Quantity

data class InventoryUpdate(
        val productId: ProductId,
        val quantity: Quantity,
        val locationId: LocationId
) {
        fun toShopifyInventoryRequest(): ShopifyInventoryRequest {
                return ShopifyInventoryRequest(
                        locationId = locationId.value,
                        inventoryItemId = productId.value,
                        available = quantity.value
                )
        }
}

data class ShopifyInventoryRequest(
        val locationId: Long,
        val inventoryItemId: Long,
        val available: Int
)

data class ShopifyInventoryResponse(val inventoryLevel: InventoryLevel)

data class InventoryLevel(
        val inventoryItemId: Long,
        val locationId: Long,
        val available: Int,
        val updatedAt: String
)
