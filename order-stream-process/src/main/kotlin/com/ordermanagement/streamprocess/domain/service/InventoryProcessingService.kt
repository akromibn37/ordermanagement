package com.ordermanagement.streamprocess.domain.service

import com.ordermanagement.streamprocess.domain.entity.InventoryUpdate
import com.ordermanagement.streamprocess.domain.entity.ShopifyInventoryRequest
import org.springframework.stereotype.Service

@Service
class InventoryProcessingService {

    fun validateInventoryUpdate(inventoryUpdate: InventoryUpdate): Boolean {
        return isProductIdValid(inventoryUpdate.productId.value) &&
                isQuantityValid(inventoryUpdate.quantity.value) &&
                isLocationIdValid(inventoryUpdate.locationId.value)
    }

    fun createShopifyInventoryRequest(inventoryUpdate: InventoryUpdate): ShopifyInventoryRequest {
        return inventoryUpdate.toShopifyInventoryRequest()
    }

    private fun isProductIdValid(productId: Long): Boolean {
        return productId > MINIMUM_PRODUCT_ID_VALUE
    }

    private fun isQuantityValid(quantity: Int): Boolean {
        return quantity >= MINIMUM_QUANTITY_VALUE
    }

    private fun isLocationIdValid(locationId: Long): Boolean {
        return locationId > MINIMUM_LOCATION_ID_VALUE
    }

    companion object {
        private const val MINIMUM_PRODUCT_ID_VALUE = 0L
        private const val MINIMUM_QUANTITY_VALUE = 0
        private const val MINIMUM_LOCATION_ID_VALUE = 0L
    }
}
