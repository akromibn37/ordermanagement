package com.ordermanagement.streamprocess.infrastructure.external

import com.ordermanagement.streamprocess.application.port.out.ShopifyApiPort
import com.ordermanagement.streamprocess.application.port.out.ShopifyApiResult
import com.ordermanagement.streamprocess.domain.entity.ShopifyInventoryRequest
import com.ordermanagement.streamprocess.domain.entity.ShopifyInventoryResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange

@Component
class ShopifyApiAdapter(private val webClient: WebClient) : ShopifyApiPort {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${app.shopify.base-url}") private lateinit var baseUrl: String

    @Value("\${app.shopify.admin-api-version}") private lateinit var apiVersion: String

    @Value("\${app.shopify.access-token}") private lateinit var accessToken: String

    override suspend fun updateInventory(request: ShopifyInventoryRequest): ShopifyApiResult {
        return try {
            val url = "$baseUrl/admin/api/$apiVersion/inventory_levels/set.json"

            logger.info("Calling Shopify API: {} with request: {}", url, request)

            val response =
                    webClient
                            .post()
                            .uri(url)
                            .header("X-Shopify-Access-Token", accessToken)
                            .header("Content-Type", "application/json")
                            .bodyValue(request)
                            .awaitExchange()

            if (response.statusCode().is2xxSuccessful) {
                val responseBody = response.awaitBody<ShopifyInventoryResponse>()
                logger.info("Successfully updated inventory: {}", responseBody)
                ShopifyApiResult.Success(responseBody)
            } else {
                val errorBody = response.awaitBody<String>()
                logger.error(
                        "Failed to update inventory. Status: {}, Body: {}",
                        response.statusCode(),
                        errorBody
                )
                ShopifyApiResult.Error(
                        message =
                                "Shopify API returned status ${response.statusCode()}: $errorBody",
                        statusCode = response.statusCode().value()
                )
            }
        } catch (e: Exception) {
            logger.error("Error calling Shopify API: {}", e.message, e)
            ShopifyApiResult.Error(message = "Error calling Shopify API: ${e.message}")
        }
    }
}
