package com.ordermanagement.streamprocess.application

import com.ordermanagement.streamprocess.application.port.out.ShopifyApiPort
import com.ordermanagement.streamprocess.application.port.out.ShopifyApiResult
import com.ordermanagement.streamprocess.domain.entity.InventoryUpdate
import com.ordermanagement.streamprocess.domain.entity.ShopifyInventoryRequest
import com.ordermanagement.streamprocess.domain.service.InventoryProcessingService
import org.springframework.stereotype.Service

@Service
class ProcessInventoryUpdateUseCase(
        private val inventoryProcessingService: InventoryProcessingService,
        private val shopifyApiPort: ShopifyApiPort
) : ProcessInventoryUpdatePort {

    override suspend fun processInventoryUpdate(inventoryUpdate: InventoryUpdate): ProcessResult {
        return try {
            if (!isInventoryUpdateValid(inventoryUpdate)) {
                return createValidationFailureResult(inventoryUpdate)
            }

            val shopifyRequest = createShopifyInventoryRequest(inventoryUpdate)
            val shopifyResult = updateShopifyInventory(shopifyRequest)

            handleShopifyApiResult(shopifyResult, inventoryUpdate)
        } catch (e: Exception) {
            createExceptionResult(inventoryUpdate, e)
        }
    }

    private fun isInventoryUpdateValid(inventoryUpdate: InventoryUpdate): Boolean {
        return inventoryProcessingService.validateInventoryUpdate(inventoryUpdate)
    }

    private fun createShopifyInventoryRequest(
            inventoryUpdate: InventoryUpdate
    ): ShopifyInventoryRequest {
        return inventoryProcessingService.createShopifyInventoryRequest(inventoryUpdate)
    }

    private suspend fun updateShopifyInventory(
            shopifyRequest: ShopifyInventoryRequest
    ): ShopifyApiResult {
        return shopifyApiPort.updateInventory(shopifyRequest)
    }

    private fun handleShopifyApiResult(
            shopifyResult: ShopifyApiResult,
            inventoryUpdate: InventoryUpdate
    ): ProcessResult {
        return when (shopifyResult) {
            is ShopifyApiResult.Success -> createSuccessResult(inventoryUpdate)
            is ShopifyApiResult.Error -> createShopifyApiFailureResult(shopifyResult)
        }
    }

    private fun createValidationFailureResult(inventoryUpdate: InventoryUpdate): ProcessResult {
        return ProcessResult(
                isSuccess = false,
                message = INVALID_INVENTORY_UPDATE_MESSAGE,
                productId = inventoryUpdate.productId.value
        )
    }

    private fun createSuccessResult(inventoryUpdate: InventoryUpdate): ProcessResult {
        return ProcessResult(
                isSuccess = true,
                message = INVENTORY_UPDATE_SUCCESS_MESSAGE,
                productId = inventoryUpdate.productId.value
        )
    }

    private fun createShopifyApiFailureResult(
            shopifyResult: ShopifyApiResult.Error
    ): ProcessResult {
        return ProcessResult(
                isSuccess = false,
                message = "Shopify API error: ${shopifyResult.message}",
                productId = null
        )
    }

    private fun createExceptionResult(
            inventoryUpdate: InventoryUpdate,
            exception: Exception
    ): ProcessResult {
        return ProcessResult(
                isSuccess = false,
                message = "Error processing inventory update: ${exception.message}",
                productId = inventoryUpdate.productId.value
        )
    }

    companion object {
        private const val INVALID_INVENTORY_UPDATE_MESSAGE = "Invalid inventory update data"
        private const val INVENTORY_UPDATE_SUCCESS_MESSAGE =
                "Inventory update processed successfully"
    }
}
