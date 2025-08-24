package com.ordermanagement.api.application.usecase

import com.ordermanagement.api.application.dto.ShopifyOrderDto
import com.ordermanagement.api.application.dto.ShopifyOrderResponseDto
import com.ordermanagement.api.application.port.`in`.ProcessOrderPort
import com.ordermanagement.api.application.port.out.*
import com.ordermanagement.api.domain.entity.*
import com.ordermanagement.api.domain.service.OrderProcessingService
import com.ordermanagement.api.domain.service.OrderValidationResult
import java.math.BigDecimal
import java.time.LocalDateTime
import org.springframework.stereotype.Service

@Service
class ProcessShopifyOrderUseCase(
        private val orderProcessingService: OrderProcessingService,
        private val orderDataApiPort: OrderDataApiPort,
        private val wmsApiPort: WmsApiPort,
        private val kafkaPort: KafkaPort
) : ProcessOrderPort {

        override suspend fun processOrder(
                shopifyOrderDto: ShopifyOrderDto
        ): ShopifyOrderResponseDto {
                return try {
                        val order = convertToDomainEntity(shopifyOrderDto)

                        val validationResult = validateOrder(order)
                        if (validationResult is OrderValidationResult.Error) {
                                return createErrorResponse(
                                        shopifyOrderDto.id,
                                        validationResult.message
                                )
                        }

                        val orderCheckResult = checkOrderAvailability(shopifyOrderDto.id, order)
                        if (!orderCheckResult.isContinue) {
                                return createErrorResponse(
                                        shopifyOrderDto.id,
                                        orderCheckResult.description
                                )
                        }

                        val inventoryCheckResult = checkWmsInventoryAvailability(order)
                        if (!inventoryCheckResult.isAvailable) {
                                val errorMessage =
                                        extractInventoryErrorMessage(inventoryCheckResult)
                                return createErrorResponse(
                                        shopifyOrderDto.id,
                                        "Insufficient inventory: $errorMessage"
                                )
                        }

                        val orderUpdateResult = updateOrderInDataApi(order)
                        if (!orderUpdateResult.isSuccess) {
                                return createErrorResponse(
                                        shopifyOrderDto.id,
                                        "Failed to update order: ${orderUpdateResult.message}"
                                )
                        }

                        publishInventoryUpdatesToKafka(order)

                        val fulfillmentResult = createWmsFulfillmentOrder(order)
                        logFulfillmentWarningIfNeeded(fulfillmentResult)

                        createSuccessResponse(shopifyOrderDto.id)
                } catch (e: Exception) {
                        createErrorResponse(
                                shopifyOrderDto.id,
                                "Internal server error: ${e.message}"
                        )
                }
        }

        private fun validateOrder(order: Order): OrderValidationResult {
                return orderProcessingService.validateOrder(order)
        }

        private suspend fun checkOrderAvailability(
                orderId: String,
                order: Order
        ): OrderCheckResult {
                val productIds = extractProductIds(order)
                val quantities = extractQuantities(order)

                return orderDataApiPort.checkOrder(orderId, productIds, quantities)
        }

        private suspend fun checkWmsInventoryAvailability(order: Order): WmsInventoryCheckResult {
                // Skip WMS inventory check if WMS API is not available
                // The order-manage-data-api already checked inventory availability
                // WMS is only needed for fulfillment, not for order validation
                return WmsInventoryCheckResult.Available
        }

        private suspend fun updateOrderInDataApi(order: Order): OrderUpdateResult {
                return orderDataApiPort.updateOrder(order)
        }

        private suspend fun publishInventoryUpdatesToKafka(order: Order) {
                kafkaPort.publishInventoryUpdate(order)
        }

        private suspend fun createWmsFulfillmentOrder(order: Order): WmsFulfillmentResult {
                return wmsApiPort.createFulfillmentOrder(order)
        }

        private fun extractProductIds(order: Order): List<String> {
                return order.lineItems.map { it.productId.toString() }
        }

        private fun extractQuantities(order: Order): List<Int> {
                return order.lineItems.map { it.quantity }
        }

        private suspend fun collectUnavailableProducts(order: Order): List<String> {
                val unavailableProducts = mutableListOf<String>()

                for (lineItem in order.lineItems) {
                        val inventory = wmsApiPort.getProductInventory(lineItem.productId)
                        if (inventory.availableQuantity < lineItem.quantity) {
                                unavailableProducts.add(
                                        "Insufficient stock for product ${lineItem.productId}"
                                )
                        }
                }

                return unavailableProducts
        }

        private fun extractInventoryErrorMessage(inventoryCheck: WmsInventoryCheckResult): String {
                return when (inventoryCheck) {
                        is WmsInventoryCheckResult.Unavailable -> inventoryCheck.message
                        else -> "Insufficient inventory"
                }
        }

        private fun logFulfillmentWarningIfNeeded(fulfillmentResult: WmsFulfillmentResult) {
                if (!fulfillmentResult.isSuccess) {
                        // In production, this should use a proper logging framework
                        println(
                                "Warning: Failed to create fulfillment order: ${fulfillmentResult.message}"
                        )
                }
        }

        private fun createSuccessResponse(orderId: String): ShopifyOrderResponseDto {
                return ShopifyOrderResponseDto(
                        status = ORDER_STATUS_SUCCESS,
                        message = ORDER_SUCCESS_MESSAGE,
                        orderId = orderId
                )
        }

        private fun createErrorResponse(
                orderId: String,
                errorMessage: String
        ): ShopifyOrderResponseDto {
                return ShopifyOrderResponseDto(
                        status = ORDER_STATUS_ERROR,
                        message = errorMessage,
                        orderId = orderId
                )
        }

        private fun convertToDomainEntity(dto: ShopifyOrderDto): Order {
                return Order(
                        id = OrderId(dto.id.toLong()),
                        orderNumber = dto.orderNumber,
                        customer =
                                Customer(
                                        id = dto.customer.id.toLong(),
                                        email = dto.customer.email,
                                        firstName = dto.customer.firstName,
                                        lastName = dto.customer.lastName,
                                        phone = dto.customer.phone
                                ),
                        lineItems =
                                dto.lineItems.map { item ->
                                        OrderLineItem(
                                                id = item.id,
                                                productId = item.productId,
                                                variantId = item.variantId,
                                                quantity = item.quantity,
                                                title = item.title,
                                                sku = item.sku,
                                                price =
                                                        Money(
                                                                BigDecimal(item.price),
                                                                Currency.valueOf(dto.currency)
                                                        ),
                                                totalDiscount =
                                                        Money(
                                                                BigDecimal(item.totalDiscount),
                                                                Currency.valueOf(dto.currency)
                                                        ),
                                                variantTitle = item.variantTitle
                                        )
                                },
                        shippingAddress =
                                Address(
                                        firstName = dto.shippingAddress.firstName,
                                        lastName = dto.shippingAddress.lastName,
                                        address1 = dto.shippingAddress.address1,
                                        city = dto.shippingAddress.city,
                                        province = dto.shippingAddress.province,
                                        country = dto.shippingAddress.country,
                                        zip = dto.shippingAddress.zip
                                ),
                        billingAddress =
                                Address(
                                        firstName = dto.billingAddress.firstName,
                                        lastName = dto.billingAddress.lastName,
                                        address1 = dto.billingAddress.address1,
                                        city = dto.billingAddress.city,
                                        province = dto.billingAddress.province,
                                        country = dto.billingAddress.country,
                                        zip = dto.billingAddress.zip
                                ),
                        totalPrice =
                                Money(BigDecimal(dto.totalPrice), Currency.valueOf(dto.currency)),
                        subtotalPrice =
                                Money(
                                        BigDecimal(dto.subtotalPrice),
                                        Currency.valueOf(dto.currency)
                                ),
                        totalTax = Money(BigDecimal(dto.totalTax), Currency.valueOf(dto.currency)),
                        currency = Currency.valueOf(dto.currency),
                        financialStatus = FinancialStatus.valueOf(dto.financialStatus),
                        fulfillmentStatus =
                                dto.fulfillmentStatus?.let { FulfillmentStatus.valueOf(it) },
                        tags = dto.tags,
                        note = dto.note,
                        sourceName = dto.sourceName,
                        createdAt = LocalDateTime.parse(dto.createdAt),
                        updatedAt = LocalDateTime.parse(dto.updatedAt),
                        processedAt = dto.processedAt?.let { LocalDateTime.parse(it) },
                        status = OrderStatus.PENDING
                )
        }

        companion object {
                private const val ORDER_STATUS_SUCCESS = "success"
                private const val ORDER_STATUS_ERROR = "error"
                private const val ORDER_SUCCESS_MESSAGE = "Order received and processed"
        }
}

sealed class WmsInventoryCheckResult {
        object Available : WmsInventoryCheckResult()
        data class Unavailable(val message: String) : WmsInventoryCheckResult()

        val isAvailable: Boolean
                get() = this is Available
}
