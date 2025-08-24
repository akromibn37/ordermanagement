package com.ordermanagement.dataapi.domain.service

import com.ordermanagement.dataapi.domain.entity.*
import com.ordermanagement.dataapi.domain.repository.InventoryRepository
import com.ordermanagement.dataapi.domain.repository.OrderRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("OrderCheckService Tests")
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
        @DisplayName("Should return isContinue false when order already exists and completed")
        fun `should return isContinue false when order already exists and completed`() {
                // Given
                val existingOrder = createSampleOrder().copy(orderStatus = "COMPLETED")
                every { orderRepository.findByOrderNumber(1001) } returns existingOrder

                // When
                val result =
                        orderCheckService.checkOrderAndInventory(
                                "1001",
                                listOf(123456789L),
                                listOf(2)
                        )

                // Then
                assertFalse(result.isContinue)
                assertEquals("Order already exists and completed", result.description)
                assertEquals("1001", result.orderId)
                assertTrue(result.products.isEmpty())

                verify { orderRepository.findByOrderNumber(1001) }
        }

        @Test
        @DisplayName("Should return isContinue false when order already exists and successful")
        fun `should return isContinue false when order already exists and successful`() {
                // Given
                val existingOrder = createSampleOrder().copy(orderStatus = "SUCCESS")
                every { orderRepository.findByOrderNumber(1001) } returns existingOrder

                // When
                val result =
                        orderCheckService.checkOrderAndInventory(
                                "1001",
                                listOf(123456789L),
                                listOf(2)
                        )

                // Then
                assertFalse(result.isContinue)
                assertEquals("Order already exists and completed", result.description)
                assertEquals("1001", result.orderId)
                assertTrue(result.products.isEmpty())
        }

        @Test
        @DisplayName("Should return isContinue true when all products are available")
        fun `should return isContinue true when all products are available`() {
                // Given
                every { orderRepository.findByOrderNumber(1001) } returns null
                every { inventoryRepository.findByProductId(123456789L) } returns
                        createSampleInventory()

                // When
                val result =
                        orderCheckService.checkOrderAndInventory(
                                "1001",
                                listOf(123456789L),
                                listOf(2)
                        )

                // Then
                assertTrue(result.isContinue)
                assertEquals("success", result.description)
                assertEquals("1001", result.orderId)
                assertEquals(1, result.products.size)
                assertEquals("available", result.products[0].status)
                assertEquals(123456789L, result.products[0].productId)
                assertEquals("TSHIRT-001", result.products[0].sku)
                assertEquals("Popular T-Shirt", result.products[0].title)
                assertEquals(2, result.products[0].requestedQuantity)
                assertEquals(10, result.products[0].availableQuantity)
                assertEquals(8, result.products[0].remainQuantity)

                verify { orderRepository.findByOrderNumber(1001) }
                verify { inventoryRepository.findByProductId(123456789L) }
        }

        @Test
        @DisplayName("Should return isContinue false when product is not available")
        fun `should return isContinue false when product is not available`() {
                // Given
                every { orderRepository.findByOrderNumber(1001) } returns null
                every { inventoryRepository.findByProductId(123456789L) } returns
                        createSampleInventory().copy(availableQuantity = 1)

                // When
                val result =
                        orderCheckService.checkOrderAndInventory(
                                "1001",
                                listOf(123456789L),
                                listOf(2)
                        )

                // Then
                assertFalse(result.isContinue)
                assertEquals("not enough inventory", result.description)
                assertEquals("1001", result.orderId)
                assertEquals(1, result.products.size)
                assertEquals("insufficient", result.products[0].status)
                assertEquals(1, result.products[0].availableQuantity)
                assertEquals(1, result.products[0].remainQuantity)
        }

        @Test
        @DisplayName("Should return isContinue false when product is not found")
        fun `should return isContinue false when product is not found`() {
                // Given
                every { orderRepository.findByOrderNumber(1001) } returns null
                every { inventoryRepository.findByProductId(123456789L) } returns null

                // When
                val result =
                        orderCheckService.checkOrderAndInventory(
                                "1001",
                                listOf(123456789L),
                                listOf(2)
                        )

                // Then
                assertFalse(result.isContinue)
                assertEquals("not enough inventory", result.description)
                assertEquals("1001", result.orderId)
                assertEquals(1, result.products.size)
                assertEquals("not_found", result.products[0].status)
                assertEquals("N/A", result.products[0].sku)
                assertEquals("Product not found", result.products[0].title)
                assertEquals(0, result.products[0].availableQuantity)
                assertEquals(0, result.products[0].remainQuantity)
        }

        @Test
        @DisplayName("Should handle multiple products with mixed availability")
        fun `should handle multiple products with mixed availability`() {
                // Given
                val productIds = listOf(123456789L, 987654321L, 555666777L)
                val quantities = listOf(2, 1, 3)

                every { orderRepository.findByOrderNumber(1001) } returns null
                every { inventoryRepository.findByProductId(123456789L) } returns
                        createSampleInventory(10, 123456789L) // Available
                every { inventoryRepository.findByProductId(987654321L) } returns
                        createSampleInventory(0, 987654321L) // Insufficient
                every { inventoryRepository.findByProductId(555666777L) } returns null // Not found

                // When
                val result =
                        orderCheckService.checkOrderAndInventory("1001", productIds, quantities)

                // Then
                assertFalse(result.isContinue)
                assertEquals("not enough inventory", result.description)
                assertEquals(3, result.products.size)

                // First product - available
                assertEquals("available", result.products[0].status)
                assertEquals(123456789L, result.products[0].productId)
                assertEquals(8, result.products[0].remainQuantity)

                // Second product - insufficient
                assertEquals("insufficient", result.products[1].status)
                assertEquals(987654321L, result.products[1].productId)
                assertEquals(0, result.products[1].remainQuantity)

                // Third product - not found
                assertEquals("not_found", result.products[2].status)
                assertEquals(555666777L, result.products[2].productId)
                assertEquals(0, result.products[2].remainQuantity)
        }

        @Test
        @DisplayName("Should handle invalid order ID format gracefully")
        fun `should handle invalid order ID format gracefully`() {
                // Given
                val invalidOrderId = "INVALID-ORDER-ID"
                every { orderRepository.findByOrderNumber(any()) } returns null
                every { inventoryRepository.findByProductId(123456789L) } returns
                        createSampleInventory()

                // When
                val result =
                        orderCheckService.checkOrderAndInventory(
                                invalidOrderId,
                                listOf(123456789L),
                                listOf(2)
                        )

                // Then
                assertTrue(result.isContinue)
                assertEquals("success", result.description)
                assertEquals(invalidOrderId, result.orderId)
                assertEquals(1, result.products.size)
                assertEquals("available", result.products[0].status)
        }

        @Test
        @DisplayName("Should handle empty product lists")
        fun `should handle empty product lists`() {
                // Given
                every { orderRepository.findByOrderNumber(1001) } returns null

                // When
                val result =
                        orderCheckService.checkOrderAndInventory("1001", emptyList(), emptyList())

                // Then
                assertTrue(result.isContinue)
                assertEquals("success", result.description)
                assertEquals("1001", result.orderId)
                assertTrue(result.products.isEmpty())
        }

        @Test
        @DisplayName("Should handle order with PENDING status")
        fun `should handle order with PENDING status`() {
                // Given
                val existingOrder = createSampleOrder().copy(orderStatus = "PENDING")
                every { orderRepository.findByOrderNumber(1001) } returns existingOrder
                every { inventoryRepository.findByProductId(123456789L) } returns
                        createSampleInventory()

                // When
                val result =
                        orderCheckService.checkOrderAndInventory(
                                "1001",
                                listOf(123456789L),
                                listOf(2)
                        )

                // Then
                assertTrue(result.isContinue)
                assertEquals("success", result.description)
                assertEquals("1001", result.orderId)
                assertEquals(1, result.products.size)
        }

        @Test
        @DisplayName("Should handle order with PROCESSING status")
        fun `should handle order with PROCESSING status`() {
                // Given
                val existingOrder = createSampleOrder().copy(orderStatus = "PROCESSING")
                every { orderRepository.findByOrderNumber(1001) } returns existingOrder
                every { inventoryRepository.findByProductId(123456789L) } returns
                        createSampleInventory()

                // When
                val result =
                        orderCheckService.checkOrderAndInventory(
                                "1001",
                                listOf(123456789L),
                                listOf(2)
                        )

                // Then
                assertTrue(result.isContinue)
                assertEquals("success", result.description)
                assertEquals("1001", result.orderId)
                assertEquals(1, result.products.size)
        }

        @Test
        @DisplayName("Should handle exact quantity match")
        fun `should handle exact quantity match`() {
                // Given
                every { orderRepository.findByOrderNumber(1001) } returns null
                every { inventoryRepository.findByProductId(123456789L) } returns
                        createSampleInventory(5)

                // When
                val result =
                        orderCheckService.checkOrderAndInventory(
                                "1001",
                                listOf(123456789L),
                                listOf(5)
                        )

                // Then
                assertTrue(result.isContinue)
                assertEquals("success", result.description)
                assertEquals(1, result.products.size)
                assertEquals("available", result.products[0].status)
                assertEquals(5, result.products[0].requestedQuantity)
                assertEquals(5, result.products[0].availableQuantity)
                assertEquals(0, result.products[0].remainQuantity)
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

        private fun createSampleInventory(
                availableQuantity: Int = 10,
                productId: Long = 123456789
        ): Inventory {
                return Inventory(
                        productId = productId,
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
