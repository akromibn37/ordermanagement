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
            val url = buildShopifyApiUrl()
            logger.info("Calling Shopify API: {} with request: {}", url, request)

            val response = executeShopifyApiCall(url, request)
            handleShopifyApiResponse(response)
        } catch (e: Exception) {
            logAndReturnApiError(e)
        }
    }

    private fun buildShopifyApiUrl(): String {
        return "$baseUrl/admin/api/$apiVersion/inventory_levels/set.json"
    }

    private suspend fun executeShopifyApiCall(
            url: String,
            request: ShopifyInventoryRequest
    ): org.springframework.web.reactive.function.client.ClientResponse {
        return webClient
                .post()
                .uri(url)
                .header(SHOPIFY_ACCESS_TOKEN_HEADER, accessToken)
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_CONTENT_TYPE)
                .bodyValue(request)
                .awaitExchange()
    }

    private suspend fun handleShopifyApiResponse(
            response: org.springframework.web.reactive.function.client.ClientResponse
    ): ShopifyApiResult {
        return if (response.statusCode().is2xxSuccessful) {
            handleSuccessfulResponse(response)
        } else {
            handleErrorResponse(response)
        }
    }

    private suspend fun handleSuccessfulResponse(
            response: org.springframework.web.reactive.function.client.ClientResponse
    ): ShopifyApiResult {
        val responseBody = response.awaitBody<ShopifyInventoryResponse>()
        logger.info("Successfully updated inventory: {}", responseBody)
        return ShopifyApiResult.Success(responseBody)
    }

    private suspend fun handleErrorResponse(
            response: org.springframework.web.reactive.function.client.ClientResponse
    ): ShopifyApiResult {
        val errorBody = response.awaitBody<String>()
        logger.error(
                "Failed to update inventory. Status: {}, Body: {}",
                response.statusCode(),
                errorBody
        )
        return ShopifyApiResult.Error(
                message = "Shopify API returned status ${response.statusCode()}: $errorBody",
                statusCode = response.statusCode().value()
        )
    }

    private fun logAndReturnApiError(exception: Exception): ShopifyApiResult {
        logger.error("Error calling Shopify API: {}", exception.message, exception)
        return ShopifyApiResult.Error(message = "Error calling Shopify API: ${exception.message}")
    }

    companion object {
        private const val SHOPIFY_ACCESS_TOKEN_HEADER = "X-Shopify-Access-Token"
        private const val CONTENT_TYPE_HEADER = "Content-Type"
        private const val APPLICATION_JSON_CONTENT_TYPE = "application/json"
    }
}
