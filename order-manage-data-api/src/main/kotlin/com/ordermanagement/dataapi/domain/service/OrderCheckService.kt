package com.ordermanagement.dataapi.domain.service

import com.ordermanagement.dataapi.domain.entity.Inventory
import com.ordermanagement.dataapi.domain.entity.Order
import com.ordermanagement.dataapi.domain.repository.InventoryRepository
import com.ordermanagement.dataapi.domain.repository.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderCheckService(
        private val orderRepository: OrderRepository,
        private val inventoryRepository: InventoryRepository
) {

        @Transactional(readOnly = true)
        fun checkOrderAndInventory(
                orderId: String,
                productIds: List<Long>,
                quantities: List<Int>
        ): OrderCheckResult {
                val existingOrder = findExistingOrder(orderId)
                if (isOrderAlreadyCompleted(existingOrder)) {
                        return createOrderAlreadyCompletedResult(orderId)
                }

                val inventoryCheckResults =
                        performInventoryChecksForProducts(productIds, quantities)
                val allProductsAvailable = areAllProductsAvailable(inventoryCheckResults)
                val description = createResultDescription(allProductsAvailable)

                return createOrderCheckResult(
                        orderId,
                        allProductsAvailable,
                        description,
                        inventoryCheckResults
                )
        }

        private fun findExistingOrder(orderId: String): Order? {
                val orderNumber = orderId.toIntOrNull()
                return if (orderNumber != null) {
                        orderRepository.findByOrderNumber(orderNumber)
                } else {
                        null
                }
        }

        private fun isOrderAlreadyCompleted(order: Order?) = order?.isCompleted() == true

        private fun createOrderAlreadyCompletedResult(orderId: String): OrderCheckResult {
                return OrderCheckResult(
                        isContinue = false,
                        description = ORDER_ALREADY_COMPLETED_MESSAGE,
                        orderId = orderId,
                        products = emptyList()
                )
        }

        private fun performInventoryChecksForProducts(
                productIds: List<Long>,
                quantities: List<Int>
        ): List<InventoryCheckResult> {
                val inventoryCheckResults = mutableListOf<InventoryCheckResult>()

                for (i in productIds.indices) {
                        val productId = productIds[i]
                        val requestedQuantity = quantities[i]
                        val checkResult =
                                performSingleProductInventoryCheck(productId, requestedQuantity)
                        inventoryCheckResults.add(checkResult)
                }

                return inventoryCheckResults
        }

        private fun performSingleProductInventoryCheck(
                productId: Long,
                requestedQuantity: Int
        ): InventoryCheckResult {
                val inventory = inventoryRepository.findByProductId(productId)

                return if (inventory == null) {
                        createProductNotFoundResult(productId, requestedQuantity)
                } else {
                        createProductInventoryResult(inventory, requestedQuantity)
                }
        }

        private fun createProductNotFoundResult(
                productId: Long,
                requestedQuantity: Int
        ): InventoryCheckResult {
                return InventoryCheckResult(
                        productId = productId,
                        sku = PRODUCT_NOT_AVAILABLE_SKU,
                        title = PRODUCT_NOT_FOUND_TITLE,
                        requestedQuantity = requestedQuantity,
                        availableQuantity = 0,
                        remainQuantity = 0,
                        status = PRODUCT_STATUS_NOT_FOUND
                )
        }

        private fun createProductInventoryResult(
                inventory: Inventory,
                requestedQuantity: Int
        ): InventoryCheckResult {
                val isAvailable = inventory.hasAvailableStock(requestedQuantity)
                val remainQuantity =
                        calculateRemainingQuantity(inventory, requestedQuantity, isAvailable)
                val status = determineProductStatus(isAvailable)

                return InventoryCheckResult(
                        productId = inventory.productId!!.toLong(),
                        sku = inventory.sku,
                        title = inventory.productTitle,
                        requestedQuantity = requestedQuantity,
                        availableQuantity = inventory.availableQuantity,
                        remainQuantity = remainQuantity,
                        status = status
                )
        }

        private fun calculateRemainingQuantity(
                inventory: Inventory,
                requestedQuantity: Int,
                isAvailable: Boolean
        ): Int {
                return if (isAvailable) {
                        inventory.availableQuantity - requestedQuantity
                } else {
                        inventory.availableQuantity
                }
        }

        private fun determineProductStatus(isAvailable: Boolean): String {
                return if (isAvailable) PRODUCT_STATUS_AVAILABLE else PRODUCT_STATUS_INSUFFICIENT
        }

        private fun areAllProductsAvailable(
                inventoryCheckResults: List<InventoryCheckResult>
        ): Boolean {
                return inventoryCheckResults.none {
                        it.status == PRODUCT_STATUS_NOT_FOUND ||
                                it.status == PRODUCT_STATUS_INSUFFICIENT
                }
        }

        private fun createResultDescription(allProductsAvailable: Boolean): String {
                return if (allProductsAvailable) {
                        INVENTORY_CHECK_SUCCESS_MESSAGE
                } else {
                        INVENTORY_CHECK_FAILURE_MESSAGE
                }
        }

        private fun createOrderCheckResult(
                orderId: String,
                allProductsAvailable: Boolean,
                description: String,
                inventoryCheckResults: List<InventoryCheckResult>
        ): OrderCheckResult {
                return OrderCheckResult(
                        isContinue = allProductsAvailable,
                        description = description,
                        orderId = orderId,
                        products = inventoryCheckResults
                )
        }

        companion object {
                private const val ORDER_ALREADY_COMPLETED_MESSAGE =
                        "Order already exists and completed"
                private const val PRODUCT_NOT_AVAILABLE_SKU = "N/A"
                private const val PRODUCT_NOT_FOUND_TITLE = "Product not found"
                private const val PRODUCT_STATUS_NOT_FOUND = "not_found"
                private const val PRODUCT_STATUS_AVAILABLE = "available"
                private const val PRODUCT_STATUS_INSUFFICIENT = "insufficient"
                private const val INVENTORY_CHECK_SUCCESS_MESSAGE = "success"
                private const val INVENTORY_CHECK_FAILURE_MESSAGE = "not enough inventory"
        }
}

data class OrderCheckResult(
        val isContinue: Boolean,
        val description: String,
        val orderId: String,
        val products: List<InventoryCheckResult>
)

data class InventoryCheckResult(
        val productId: Long,
        val sku: String,
        val title: String,
        val requestedQuantity: Int,
        val availableQuantity: Int,
        val remainQuantity: Int,
        val status: String
)
