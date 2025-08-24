package com.ordermanagement.dataapi.web

import com.ordermanagement.dataapi.application.dto.*
import com.ordermanagement.dataapi.application.usecase.OrderUpdateUseCase
import com.ordermanagement.dataapi.domain.service.InventoryCheckResult
import com.ordermanagement.dataapi.domain.service.OrderCheckResult
import com.ordermanagement.dataapi.domain.service.OrderCheckService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/order")
class OrderController(
        private val orderCheckService: OrderCheckService,
        private val orderUpdateUseCase: OrderUpdateUseCase
) {

    @GetMapping("/check")
    fun checkOrder(
            @RequestParam orderId: String,
            @RequestParam productIds: String,
            @RequestParam quantity: String
    ): ResponseEntity<OrderCheckResponseDto> {
        val productIdList = parseProductIds(productIds)
        val quantityList = parseQuantities(quantity)
        val checkResult = performOrderAndInventoryCheck(orderId, productIdList, quantityList)
        val response = createOrderCheckResponse(checkResult)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/update")
    fun updateOrder(
            @RequestBody request: OrderUpdateRequestDto
    ): ResponseEntity<OrderUpdateResponseDto> {
        val result = orderUpdateUseCase.updateOrder(request)
        return createUpdateResponseEntity(result)
    }

    private fun parseProductIds(productIds: String): List<Long> {
        return productIds.split(PARAMETER_DELIMITER).map { it.trim().toLong() }
    }

    private fun parseQuantities(quantities: String): List<Int> {
        return quantities.split(PARAMETER_DELIMITER).map { it.trim().toInt() }
    }

    private fun performOrderAndInventoryCheck(
            orderId: String,
            productIds: List<Long>,
            quantities: List<Int>
    ): OrderCheckResult {
        return orderCheckService.checkOrderAndInventory(orderId, productIds, quantities)
    }

    private fun createOrderCheckResponse(result: OrderCheckResult): OrderCheckResponseDto {
        return OrderCheckResponseDto(
                isContinue = result.isContinue,
                description = result.description,
                orderId = result.orderId.toInt(),
                products = mapToOrderProductInfoDtos(result.products)
        )
    }

    private fun mapToOrderProductInfoDtos(
            products: List<InventoryCheckResult>
    ): List<OrderProductInfoDto> {
        return products.map { product ->
            OrderProductInfoDto(
                    productId = product.productId,
                    sku = product.sku,
                    title = product.title,
                    requestedQuantity = product.requestedQuantity,
                    availableQuantity = product.availableQuantity,
                    remainQuantity = product.remainQuantity,
                    status = product.status
            )
        }
    }

    private fun createUpdateResponseEntity(
            result: OrderUpdateResponseDto
    ): ResponseEntity<OrderUpdateResponseDto> {
        return if (result.isSuccess) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    companion object {
        private const val PARAMETER_DELIMITER = ","
    }
}
