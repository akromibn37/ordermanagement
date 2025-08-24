package com.ordermanagement.api.infrastructure.external

import com.ordermanagement.api.application.port.out.WmsApiPort
import com.ordermanagement.api.application.port.out.WmsFulfillmentResult
import com.ordermanagement.api.application.port.out.WmsInventoryResponse
import com.ordermanagement.api.domain.entity.Order
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Component
class WmsApiAdapter(private val webClient: WebClient) : WmsApiPort {

        @Value("\${app.wms.base-url}") private lateinit var baseUrl: String

        @Value("\${app.wms.api-key}") private lateinit var apiKey: String

        override suspend fun getProductInventory(productId: Long): WmsInventoryResponse {
                return try {
                        println("WmsApiAdapter: Calling WMS API for product $productId")
                        println("WmsApiAdapter: baseUrl = $baseUrl")
                        println("WmsApiAdapter: apiKey = $apiKey")

                        val response =
                                webClient
                                        .get()
                                        .uri("$baseUrl/api/v1/products/$productId/inventory")
                                        .header("Authorization", "Bearer $apiKey")
                                        .retrieve()
                                        .awaitBody<WmsInventoryApiResponse>()

                        println("WmsApiAdapter: Received response: $response")

                        WmsInventoryResponse(
                                productId = response.productId,
                                availableQuantity = response.availableQuantity,
                                totalQuantity = response.totalQuantity,
                                reservedQuantity = response.reservedQuantity,
                                sku = response.sku,
                                locationId = response.locationId,
                                lastUpdated = response.lastUpdated
                        )
                } catch (e: Exception) {
                        // Return default values in case of error
                        println("WmsApiAdapter: Exception occurred: ${e.message}")
                        e.printStackTrace()
                        WmsInventoryResponse(
                                productId = productId,
                                availableQuantity = 0,
                                totalQuantity = 0,
                                reservedQuantity = 0,
                                sku = "",
                                locationId = 0,
                                lastUpdated = ""
                        )
                }
        }

        override suspend fun createFulfillmentOrder(order: Order): WmsFulfillmentResult {
                return try {
                        val request =
                                WmsFulfillmentRequest(
                                        referenceId = order.orderNumber,
                                        items =
                                                order.lineItems.map { lineItem ->
                                                        WmsFulfillmentItem(
                                                                productId = lineItem.productId,
                                                                quantity = lineItem.quantity,
                                                                sku = lineItem.sku
                                                        )
                                                },
                                        shippingAddress =
                                                WmsShippingAddress(
                                                        firstName = order.shippingAddress.firstName,
                                                        lastName = order.shippingAddress.lastName,
                                                        address1 = order.shippingAddress.address1,
                                                        city = order.shippingAddress.city,
                                                        state = order.shippingAddress.province,
                                                        zipCode = order.shippingAddress.zip,
                                                        country = order.shippingAddress.country
                                                ),
                                        shippingMethod = "standard",
                                        customerEmail = order.customer.email
                                )

                        val response =
                                webClient
                                        .post()
                                        .uri("$baseUrl/api/v1/fulfillment-orders")
                                        .header("Authorization", "Bearer $apiKey")
                                        .bodyValue(request)
                                        .retrieve()
                                        .awaitBody<WmsFulfillmentResponse>()

                        WmsFulfillmentResult(
                                isSuccess =
                                        response.status == "pending" ||
                                                response.status == "success",
                                fulfillmentId = response.fulfillmentOrderId.toString(),
                                message =
                                        "Fulfillment order created with status: ${response.status}"
                        )
                } catch (e: Exception) {
                        WmsFulfillmentResult(
                                isSuccess = false,
                                message = "Failed to create fulfillment order: ${e.message}"
                        )
                }
        }
}

data class WmsInventoryApiResponse(
        val productId: Long,
        val sku: String,
        val availableQuantity: Int,
        val reservedQuantity: Int,
        val totalQuantity: Int,
        val locationId: Long,
        val lastUpdated: String
)

data class WmsFulfillmentRequest(
        val referenceId: String,
        val items: List<WmsFulfillmentItem>,
        val shippingAddress: WmsShippingAddress,
        val shippingMethod: String,
        val customerEmail: String
)

data class WmsFulfillmentItem(val productId: Long, val quantity: Int, val sku: String)

data class WmsShippingAddress(
        val firstName: String,
        val lastName: String,
        val address1: String,
        val city: String,
        val state: String,
        val zipCode: String,
        val country: String
)

data class WmsFulfillmentResponse(
        val fulfillmentOrderId: Long,
        val status: String,
        val referenceId: String,
        val estimatedShipDate: String,
        val trackingNumber: String?
)
