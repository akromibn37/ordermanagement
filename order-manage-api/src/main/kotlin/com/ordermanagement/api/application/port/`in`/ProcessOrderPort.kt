package com.ordermanagement.api.application.port.`in`

import com.ordermanagement.api.application.dto.ShopifyOrderDto
import com.ordermanagement.api.application.dto.ShopifyOrderResponseDto

interface ProcessOrderPort {
    suspend fun processOrder(shopifyOrderDto: ShopifyOrderDto): ShopifyOrderResponseDto
} 