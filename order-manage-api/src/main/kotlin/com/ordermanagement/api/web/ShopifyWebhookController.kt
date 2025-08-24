package com.ordermanagement.api.web

import com.ordermanagement.api.application.dto.ShopifyOrderDto
import com.ordermanagement.api.application.dto.ShopifyOrderResponseDto
import com.ordermanagement.api.application.port.`in`.ProcessOrderPort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/shopify/webhooks")
class ShopifyWebhookController(private val processOrderPort: ProcessOrderPort) {

    @PostMapping("/orders")
    suspend fun handleOrderWebhook(
            @RequestBody shopifyOrderDto: ShopifyOrderDto
    ): ResponseEntity<ShopifyOrderResponseDto> {
        val result = processOrderPort.processOrder(shopifyOrderDto)
        return createResponseEntity(result)
    }

    private fun createResponseEntity(
            result: ShopifyOrderResponseDto
    ): ResponseEntity<ShopifyOrderResponseDto> {
        return if (result.status == ORDER_STATUS_SUCCESS) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    companion object {
        private const val ORDER_STATUS_SUCCESS = "success"
    }
}
