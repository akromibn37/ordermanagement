package com.ordermanagement.dataapi.application.usecase

import com.ordermanagement.dataapi.application.dto.OrderLineItemDto
import com.ordermanagement.dataapi.application.dto.OrderUpdateRequestDto
import com.ordermanagement.dataapi.domain.entity.*
import com.ordermanagement.dataapi.domain.repository.InventoryRepository
import com.ordermanagement.dataapi.domain.repository.OrderRepository
import io.mockk.*
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("OrderUpdateUseCase Tests")
class OrderUpdateUseCaseTest {

    private lateinit var orderRepository: OrderRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var orderUpdateUseCase: OrderUpdateUseCase

    @BeforeEach
    fun setUp() {
        orderRepository = mockk()
        inventoryRepository = mockk()
        orderUpdateUseCase = OrderUpdateUseCase(orderRepository, inventoryRepository)
    }

    @Test
    @DisplayName("Should update order successfully when all inventory is available")
    fun `should update order successfully when all inventory is available`() {
        // Given
        val request = createSampleOrderUpdateRequest()
        val inventory = createSampleInventory(10)

        every { inventoryRepository.findByProductId(123456789L) } returns inventory
        every { inventoryRepository.findByProductId(987654321L) } returns
                inventory.copy(productId = 987654321L)
        every { inventoryRepository.save(any()) } returns inventory
        every { orderRepository.save(any()) } returns createSampleOrder()

        // When
        val result = orderUpdateUseCase.updateOrder(request)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("Order updated successfully", result.message)

        verify {
            inventoryRepository.findByProductId(123456789L)
            inventoryRepository.findByProductId(987654321L)
            inventoryRepository.save(any())
            orderRepository.save(any())
        }
    }

    @Test
    @DisplayName("Should return failure when product is not found")
    fun `should return failure when product is not found`() {
        // Given
        val request = createSampleOrderUpdateRequest()
        every { inventoryRepository.findByProductId(123456789L) } returns null

        // When
        val result = orderUpdateUseCase.updateOrder(request)

        // Then
        assertFalse(result.isSuccess)
        assertTrue(result.message?.contains("Product 123456789 not found") == true)

        verify { inventoryRepository.findByProductId(123456789L) }
        verify(exactly = 0) { orderRepository.save(any()) }
    }

    @Test
    @DisplayName("Should return failure when insufficient stock")
    fun `should return failure when insufficient stock`() {
        // Given
        val request = createSampleOrderUpdateRequest()
        val inventory = createSampleInventory(1) // Only 1 available, but requesting 2

        every { inventoryRepository.findByProductId(123456789L) } returns inventory

        // When
        val result = orderUpdateUseCase.updateOrder(request)

        // Then
        assertFalse(result.isSuccess)
        assertTrue(result.message?.contains("Insufficient stock for product 123456789") == true)

        verify { inventoryRepository.findByProductId(123456789L) }
        verify(exactly = 0) { orderRepository.save(any()) }
    }

    @Test
    @DisplayName("Should handle exception gracefully")
    fun `should handle exception gracefully`() {
        // Given
        val request = createSampleOrderUpdateRequest()
        every { inventoryRepository.findByProductId(any()) } throws
                RuntimeException("Database connection failed")

        // When
        val result = orderUpdateUseCase.updateOrder(request)

        // Then
        assertFalse(result.isSuccess)
        assertTrue(result.message?.contains("Failed to create order") == true)
        assertTrue(result.message?.contains("Database connection failed") == true)
    }

    @Test
    @DisplayName("Should create order with correct details")
    fun `should create order with correct details`() {
        // Given
        val request = createSampleOrderUpdateRequest()
        val inventory = createSampleInventory(10)
        val orderSlot = slot<Order>()

        every { inventoryRepository.findByProductId(any()) } returns inventory
        every { inventoryRepository.save(any()) } returns inventory
        every { orderRepository.save(capture(orderSlot)) } returns createSampleOrder()

        // When
        orderUpdateUseCase.updateOrder(request)

        // Then
        val capturedOrder = orderSlot.captured
        assertEquals(1001, capturedOrder.orderNumber)
        assertEquals("1", capturedOrder.customerId)
        assertEquals(2, capturedOrder.productTypeCount)
        assertEquals("39.98", capturedOrder.totalPrice)
        assertEquals("PROCESSING", capturedOrder.orderStatus)
        assertEquals("system", capturedOrder.createBy)
        assertEquals("system", capturedOrder.updateBy)
        assertEquals(2, capturedOrder.orderDetails.size)
    }

    @Test
    @DisplayName("Should create order details correctly")
    fun `should create order details correctly`() {
        // Given
        val request = createSampleOrderUpdateRequest()
        val inventory = createSampleInventory(10)
        val orderSlot = slot<Order>()

        every { inventoryRepository.findByProductId(any()) } returns inventory
        every { inventoryRepository.save(any()) } returns inventory
        every { orderRepository.save(capture(orderSlot)) } returns createSampleOrder()

        // When
        orderUpdateUseCase.updateOrder(request)

        // Then
        val capturedOrder = orderSlot.captured
        val firstDetail = capturedOrder.orderDetails[0]
        val secondDetail = capturedOrder.orderDetails[1]

        assertEquals(123456789L, firstDetail.productId)
        assertEquals("19.99", firstDetail.price)
        assertEquals(2, firstDetail.quantity)
        assertEquals("system", firstDetail.createBy)
        assertEquals("system", firstDetail.updateBy)

        assertEquals(987654321L, secondDetail.productId)
        assertEquals("19.99", secondDetail.price)
        assertEquals(1, secondDetail.quantity)
    }

    @Test
    @DisplayName("Should allocate inventory correctly")
    fun `should allocate inventory correctly`() {
        // Given
        val request = createSampleOrderUpdateRequest()
        val inventory = createSampleInventory(10)

        every { inventoryRepository.findByProductId(any()) } returns inventory
        every { inventoryRepository.save(any()) } returns inventory
        every { orderRepository.save(any()) } returns createSampleOrder()

        // When
        orderUpdateUseCase.updateOrder(request)

        // Then
        // Should be called twice (once for each line item)
        verify(exactly = 2) { inventoryRepository.save(any()) }

        // Verify that inventory was saved with reduced quantities
        verify {
            inventoryRepository.save(
                    match { it.availableQuantity == 8 }
            ) // 10 - 2 = 8 for first item
            inventoryRepository.save(
                    match { it.availableQuantity == 9 }
            ) // 10 - 1 = 9 for second item
        }
    }

    @Test
    @DisplayName("Should handle single line item order")
    fun `should handle single line item order`() {
        // Given
        val request =
                createSampleOrderUpdateRequest()
                        .copy(
                                lineItems =
                                        listOf(
                                                OrderLineItemDto(
                                                        id = 1L,
                                                        productId = 123456789L,
                                                        price = "19.99",
                                                        quantity = 1,
                                                        title = "Popular T-Shirt",
                                                        sku = "TSHIRT-001",
                                                        totalDiscount = "0.00"
                                                )
                                        )
                        )
        val inventory = createSampleInventory(5)

        every { inventoryRepository.findByProductId(123456789L) } returns inventory
        every { inventoryRepository.save(any()) } returns inventory
        every { orderRepository.save(any()) } returns createSampleOrder()

        // When
        val result = orderUpdateUseCase.updateOrder(request)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("Order updated successfully", result.message)

        verify { inventoryRepository.findByProductId(123456789L) }
        verify(exactly = 1) { inventoryRepository.save(any()) }
        verify { orderRepository.save(any()) }
    }

    @Test
    @DisplayName("Should handle empty line items")
    fun `should handle empty line items`() {
        // Given
        val request = createSampleOrderUpdateRequest().copy(lineItems = emptyList())

        every { orderRepository.save(any()) } returns createSampleOrder()

        // When
        val result = orderUpdateUseCase.updateOrder(request)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("Order updated successfully", result.message)

        verify(exactly = 0) { inventoryRepository.findByProductId(any()) }
        verify(exactly = 0) { inventoryRepository.save(any()) }
        verify { orderRepository.save(any()) }
    }

    private fun createSampleOrderUpdateRequest(): OrderUpdateRequestDto {
        return OrderUpdateRequestDto(
                id = 1L,
                orderNumber = 1001,
                customerId = 1L,
                totalPrice = "39.98",
                currency = "USD",
                createdAt = "2024-01-15T10:00:00",
                updatedAt = "2024-01-15T10:00:00",
                processedAt = null,
                lineItems =
                        listOf(
                                OrderLineItemDto(
                                        id = 1L,
                                        productId = 123456789L,
                                        price = "19.99",
                                        quantity = 2,
                                        title = "Popular T-Shirt",
                                        sku = "TSHIRT-001",
                                        totalDiscount = "0.00"
                                ),
                                OrderLineItemDto(
                                        id = 2L,
                                        productId = 987654321L,
                                        price = "19.99",
                                        quantity = 1,
                                        title = "Popular Jeans",
                                        sku = "JEANS-001",
                                        totalDiscount = "0.00"
                                )
                        )
        )
    }

    private fun createSampleOrder(): Order {
        return Order(
                orderId = 1,
                orderNumber = 1001,
                customerId = "CUST-001",
                productTypeCount = 2,
                totalPrice = "39.98",
                orderStatus = "PROCESSING",
                createDate = LocalDateTime.now(),
                createBy = "system",
                updateDate = LocalDateTime.now(),
                updateBy = "system"
        )
    }

    private fun createSampleInventory(availableQuantity: Int = 10): Inventory {
        return Inventory(
                productId = 123456789,
                sku = "TSHIRT-001",
                productTitle = "Popular T-Shirt",
                productPrice = "19.99",
                currency = "USD",
                availableQuantity = availableQuantity,
                createDate = LocalDateTime.now(),
                createBy = "system",
                updateDate = LocalDateTime.now(),
                updateBy = "system"
        )
    }
}
