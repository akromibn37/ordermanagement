package com.ordermanagement.api.infrastructure.external

import com.ordermanagement.api.application.port.out.*
import com.ordermanagement.api.domain.entity.Order
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Component
class OrderDataApiAdapter(private val webClient: WebClient) : OrderDataApiPort {

        @Value("\${app.order-data-api.base-url}") private lateinit var baseUrl: String

        override suspend fun checkOrder(
                orderId: String,
                productIds: List<String>,
                quantities: List<Int>
        ): OrderCheckResult {
                return try {
                        val productIdsParam = productIds.joinToString(",")
                        val quantitiesParam = quantities.joinToString(",")
                        val response =
                                webClient
                                        .get()
                                        .uri(
                                                "$baseUrl/api/order/check?orderId=$orderId&productIds=$productIdsParam&quantity=$quantitiesParam"
                                        )
                                        .retrieve()
                                        .awaitBody<OrderCheckResponse>()

                        OrderCheckResult(
                                isContinue = response.isContinue,
                                description = response.description,
                                orderId = response.orderId,
                                products =
                                        response.products.map { product ->
                                                OrderProductInfo(
                                                        productId = product.productId,
                                                        sku = product.sku,
                                                        title = product.title,
                                                        requestedQuantity =
                                                                product.requestedQuantity,
                                                        availableQuantity =
                                                                product.availableQuantity,
                                                        remainQuantity = product.remainQuantity,
                                                        status = product.status
                                                )
                                        }
                        )
                } catch (e: Exception) {
                        OrderCheckResult(
                                isContinue = false,
                                description = "Failed to check order: ${e.message}",
                                orderId = orderId.toIntOrNull() ?: 0,
                                products = emptyList()
                        )
                }
        }

        override suspend fun updateOrder(order: Order): OrderUpdateResult {
                return try {
                        val orderNumberInt =
                                try {
                                        order.orderNumber.toInt()
                                } catch (e: NumberFormatException) {
                                        // Extract numeric part from order number like "TEST-123" ->
                                        // 123
                                        val numericPart =
                                                order.orderNumber.replace("[^0-9]".toRegex(), "")
                                        if (numericPart.isNotEmpty()) {
                                                numericPart.toInt()
                                        } else {
                                                // Use hash code as fallback for non-numeric order
                                                // numbers
                                                Math.abs(order.orderNumber.hashCode()) % 1000000
                                        }
                                }

                        val request =
                                OrderUpdateRequest(
                                        id = order.id.value.toString(),
                                        orderNumber = orderNumberInt,
                                        customerId = order.customer.id.toString(),
                                        createdAt = order.createdAt.toString(),
                                        updatedAt = order.updatedAt.toString(),
                                        processedAt = order.processedAt?.toString(),
                                        lineItems =
                                                order.lineItems.map { lineItem ->
                                                        OrderLineItemDto(
                                                                id = lineItem.id,
                                                                productId = lineItem.productId,
                                                                quantity = lineItem.quantity,
                                                                title = lineItem.title,
                                                                sku = lineItem.sku,
                                                                price =
                                                                        lineItem.price.amount
                                                                                .toString(),
                                                                totalDiscount =
                                                                        lineItem.totalDiscount
                                                                                .amount.toString()
                                                        )
                                                },
                                        totalPrice = order.totalPrice.amount.toString(),
                                        currency = order.currency.name
                                )

                        val response =
                                webClient
                                        .post()
                                        .uri("$baseUrl/api/order/update")
                                        .bodyValue(request)
                                        .retrieve()
                                        .awaitBody<OrderUpdateResponse>()

                        OrderUpdateResult(
                                isSuccess = response.isSuccess,
                                message = response.message
                        )
                } catch (e: Exception) {
                        OrderUpdateResult(
                                isSuccess = false,
                                message = "Failed to update order: ${e.message}"
                        )
                }
        }
}

data class OrderCheckResponse(
        val isContinue: Boolean,
        val description: String,
        val orderId: Int,
        val products: List<OrderProductResponse>
)

data class OrderProductResponse(
        val productId: Long,
        val sku: String,
        val title: String,
        val requestedQuantity: Int,
        val availableQuantity: Int,
        val remainQuantity: Int,
        val status: String
)

data class OrderUpdateRequest(
        val id: String,
        val orderNumber: Int,
        val customerId: String,
        val createdAt: String,
        val updatedAt: String,
        val processedAt: String?,
        val lineItems: List<OrderLineItemDto>,
        val totalPrice: String,
        val currency: String
)

data class OrderLineItemDto(
        val id: Long,
        val productId: Long,
        val quantity: Int,
        val title: String,
        val sku: String,
        val price: String,
        val totalDiscount: String
)

data class OrderUpdateResponse(val isSuccess: Boolean, val message: String)
