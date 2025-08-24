package com.ordermanagement.dataapi.domain.service

import com.ordermanagement.dataapi.domain.entity.*
import com.ordermanagement.dataapi.domain.repository.InventoryRepository
import com.ordermanagement.dataapi.domain.repository.OrderRepository
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OrderCheckServiceTest {

    private lateinit var orderRepository: OrderRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var orderCheckService: OrderCheckService

    @BeforeEach
    fun setUp() {
        orderRepository = mockk()
        inventoryRepository = mockk()
        orderCheckService = OrderCheckService(orderRepository, inventoryRepository)
    }

    @Test
    fun `should return isContinue false when order already exists and completed`() {
        // Given
        val existingOrder = createSampleOrder().copy(orderStatus = "COMPLETED")
        every { orderRepository.findByOrderNumber("ORDER-1001") } returns existingOrder

        // When
        val result =
                orderCheckService.checkOrderAndInventory(
                        "ORDER-1001",
                        listOf(123456789L),
                        listOf(2)
                )

        // Then
        assertFalse(result.isContinue)
        assertEquals("Order already exists and completed", result.description)
        assertEquals("ORDER-1001", result.orderId)
        assertTrue(result.products.isEmpty())
    }

    @Test
    fun `should return isContinue true when all products are available`() {
        // Given
        every { orderRepository.findByOrderNumber("ORDER-1001") } returns null
        every { inventoryRepository.findByProductId(123456789L) } returns createSampleInventory()

        // When
        val result =
                orderCheckService.checkOrderAndInventory(
                        "ORDER-1001",
                        listOf(123456789L),
                        listOf(2)
                )

        // Then
        assertTrue(result.isContinue)
        assertEquals("success", result.description)
        assertEquals("ORDER-1001", result.orderId)
        assertEquals(1, result.products.size)
        assertEquals("available", result.products[0].status)
    }

    @Test
    fun `should return isContinue false when product is not available`() {
        // Given
        every { orderRepository.findByOrderNumber("ORDER-1001") } returns null
        every { inventoryRepository.findByProductId(123456789L) } returns
                createSampleInventory().copy(availableQuantity = 1)

        // When
        val result =
                orderCheckService.checkOrderAndInventory(
                        "ORDER-1001",
                        listOf(123456789L),
                        listOf(2)
                )

        // Then
        assertFalse(result.isContinue)
        assertEquals("not enough inventory", result.description)
        assertEquals("ORDER-1001", result.orderId)
        assertEquals(1, result.products.size)
        assertEquals("insufficient", result.products[0].status)
    }

    @Test
    fun `should return isContinue false when product is not found`() {
        // Given
        every { orderRepository.findByOrderNumber("ORDER-1001") } returns null
        every { inventoryRepository.findByProductId(123456789L) } returns null

        // When
        val result =
                orderCheckService.checkOrderAndInventory(
                        "ORDER-1001",
                        listOf(123456789L),
                        listOf(2)
                )

        // Then
        assertFalse(result.isContinue)
        assertEquals("not enough inventory", result.description)
        assertEquals("ORDER-1001", result.orderId)
        assertEquals(1, result.products.size)
        assertEquals("not_found", result.products[0].status)
    }

    private fun createSampleOrder(): Order {
        return Order(
                orderId = 1,
                orderNumber = 1001,
                customerId = "CUST-001",
                productTypeCount = 1,
                totalPrice = "19.99",
                orderStatus = "PENDING",
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
