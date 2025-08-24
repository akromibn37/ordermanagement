package com.ordermanagement.streamprocess.application.port.out

import com.ordermanagement.streamprocess.domain.entity.ShopifyInventoryRequest
import com.ordermanagement.streamprocess.domain.entity.ShopifyInventoryResponse

interface ShopifyApiPort {
    suspend fun updateInventory(request: ShopifyInventoryRequest): ShopifyApiResult
}

sealed class ShopifyApiResult {
    data class Success(val response: ShopifyInventoryResponse) : ShopifyApiResult()
    data class Error(val message: String, val statusCode: Int? = null) : ShopifyApiResult()
}
