package com.ordermanagement.dataapi.application.usecase

import com.ordermanagement.dataapi.application.dto.OrderLineItemDto
import com.ordermanagement.dataapi.application.dto.OrderUpdateRequestDto
import com.ordermanagement.dataapi.application.dto.OrderUpdateResponseDto
import com.ordermanagement.dataapi.domain.entity.*
import com.ordermanagement.dataapi.domain.repository.InventoryRepository
import com.ordermanagement.dataapi.domain.repository.OrderRepository
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderUpdateUseCase(
        private val orderRepository: OrderRepository,
        private val inventoryRepository: InventoryRepository
) {

        @Transactional
        fun updateOrder(request: OrderUpdateRequestDto): OrderUpdateResponseDto {
                return try {
                        val allocationResult = allocateInventoryForLineItems(request.lineItems)
                        if (!allocationResult.isSuccess) {
                                val errorMessage =
                                        extractInventoryAllocationErrorMessage(allocationResult)
                                return createFailureResponse(errorMessage)
                        }

                        val order = createOrderFromRequest(request)
                        addOrderDetailsToOrder(order, request)
                        saveOrder(order)

                        createSuccessResponse()
                } catch (e: Exception) {
                        createExceptionResponse(e)
                }
        }

        private fun allocateInventoryForLineItems(
                lineItems: List<OrderLineItemDto>
        ): InventoryAllocationResult {
                for (lineItem in lineItems) {
                        val inventory =
                                findInventoryForProduct(lineItem.productId)
                                        ?: return createProductNotFoundFailure(lineItem.productId)

                        if (!inventory.canAllocate(lineItem.quantity)) {
                                return createInsufficientStockFailure(lineItem.productId)
                        }

                        allocateAndSaveInventory(inventory, lineItem.quantity)
                }

                return InventoryAllocationResult.Success
        }

        private fun findInventoryForProduct(productId: Long): Inventory? {
                return inventoryRepository.findByProductId(productId)
        }

        private fun createProductNotFoundFailure(
                productId: Long
        ): InventoryAllocationResult.Failure {
                return InventoryAllocationResult.Failure("Product $productId not found")
        }

        private fun createInsufficientStockFailure(
                productId: Long
        ): InventoryAllocationResult.Failure {
                return InventoryAllocationResult.Failure(
                        "Insufficient stock for product $productId"
                )
        }

        private fun allocateAndSaveInventory(inventory: Inventory, quantity: Int) {
                val updatedInventory = inventory.allocateStock(quantity)
                inventoryRepository.save(updatedInventory)
        }

        private fun createOrderFromRequest(request: OrderUpdateRequestDto): Order {
                return Order(
                        orderNumber = request.orderNumber,
                        customerId = request.customerId.toString(),
                        productTypeCount = request.lineItems.size,
                        totalPrice = request.totalPrice,
                        orderStatus = ORDER_STATUS_PROCESSING,
                        createDate = LocalDateTime.parse(request.createdAt),
                        createBy = SYSTEM_USER,
                        updateDate = LocalDateTime.parse(request.updatedAt),
                        updateBy = SYSTEM_USER
                )
        }

        private fun addOrderDetailsToOrder(order: Order, request: OrderUpdateRequestDto) {
                for (lineItem in request.lineItems) {
                        val orderDetail = createOrderDetailFromLineItem(lineItem, request)
                        order.addOrderDetail(orderDetail)
                }
        }

        private fun createOrderDetailFromLineItem(
                lineItem: OrderLineItemDto,
                request: OrderUpdateRequestDto
        ): OrderDetail {
                return OrderDetail(
                        productId = lineItem.productId,
                        price = lineItem.price,
                        quantity = lineItem.quantity,
                        createDate = LocalDateTime.parse(request.createdAt),
                        createBy = SYSTEM_USER,
                        updateDate = LocalDateTime.parse(request.updatedAt),
                        updateBy = SYSTEM_USER
                )
        }

        private fun saveOrder(order: Order) {
                orderRepository.save(order)
        }

        private fun extractInventoryAllocationErrorMessage(
                allocationResult: InventoryAllocationResult
        ): String {
                return when (allocationResult) {
                        is InventoryAllocationResult.Failure -> allocationResult.message
                        else -> INVENTORY_ALLOCATION_FAILED_MESSAGE
                }
        }

        private fun createSuccessResponse(): OrderUpdateResponseDto {
                return OrderUpdateResponseDto(
                        isSuccess = true,
                        message = ORDER_UPDATE_SUCCESS_MESSAGE
                )
        }

        private fun createFailureResponse(errorMessage: String): OrderUpdateResponseDto {
                return OrderUpdateResponseDto(isSuccess = false, message = errorMessage)
        }

        private fun createExceptionResponse(exception: Exception): OrderUpdateResponseDto {
                return OrderUpdateResponseDto(
                        isSuccess = false,
                        message = "Failed to create order: ${exception.message}"
                )
        }

        companion object {
                private const val ORDER_STATUS_PROCESSING = "PROCESSING"
                private const val SYSTEM_USER = "system"
                private const val INVENTORY_ALLOCATION_FAILED_MESSAGE =
                        "Inventory allocation failed"
                private const val ORDER_UPDATE_SUCCESS_MESSAGE = "Order updated successfully"
        }
}

sealed class InventoryAllocationResult {
        object Success : InventoryAllocationResult()
        data class Failure(val message: String) : InventoryAllocationResult()

        val isSuccess: Boolean
                get() = this is Success
}
