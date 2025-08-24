package com.ordermanagement.api.domain.service

import com.ordermanagement.api.domain.entity.*
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OrderProcessingServiceTest {

        private lateinit var orderProcessingService: OrderProcessingService

        @BeforeEach
        fun setUp() {
                orderProcessingService = OrderProcessingService()
        }

        @Test
        fun `should validate order successfully when all data is valid`() {
                // Given
                val order = createSampleOrder()

                // When
                val result = orderProcessingService.validateOrder(order)

                // Then
                assertTrue(result is OrderValidationResult.Valid)
                val validResult = result as OrderValidationResult.Valid
                assertEquals(order, validResult.order)
        }

        @Test
        fun `should return error when order cannot be fulfilled`() {
                // Given
                val order = createSampleOrder().copy(financialStatus = FinancialStatus.PENDING)

                // When
                val result = orderProcessingService.validateOrder(order)

                // Then
                assertTrue(result is OrderValidationResult.Error)
                val errorResult = result as OrderValidationResult.Error
                assertEquals("Order cannot be processed", errorResult.message)
        }

        @Test
        fun `should return error when customer information is incomplete`() {
                // Given
                val order =
                        createSampleOrder()
                                .copy(
                                        customer =
                                                createSampleOrder()
                                                        .customer
                                                        .copy(email = "", firstName = "")
                                )

                // When
                val result = orderProcessingService.validateOrder(order)

                // Then
                assertTrue(result is OrderValidationResult.Error)
                val errorResult = result as OrderValidationResult.Error
                assertTrue(errorResult.message.contains("Customer information is incomplete"))
        }

        @Test
        fun `should return error when line items are empty`() {
                // Given
                val order = createSampleOrder().copy(lineItems = emptyList())

                // When
                val result = orderProcessingService.validateOrder(order)

                // Then
                assertTrue(result is OrderValidationResult.Error)
                val errorResult = result as OrderValidationResult.Error
                assertEquals("Order must have at least one line item", errorResult.message)
        }

        @Test
        fun `should return error when line item quantity is invalid`() {
                // Given
                val order =
                        createSampleOrder()
                                .copy(
                                        lineItems =
                                                listOf(
                                                        createSampleOrder()
                                                                .lineItems
                                                                .first()
                                                                .copy(quantity = 0)
                                                )
                                )

                // When
                val result = orderProcessingService.validateOrder(order)

                // Then
                assertTrue(result is OrderValidationResult.Error)
                val errorResult = result as OrderValidationResult.Error
                assertTrue(errorResult.message.contains("Line item quantity must be positive"))
        }

        @Test
        fun `should return error when shipping address is incomplete`() {
                // Given
                val order =
                        createSampleOrder()
                                .copy(
                                        shippingAddress =
                                                createSampleOrder()
                                                        .shippingAddress
                                                        .copy(address1 = "", city = "")
                                )

                // When
                val result = orderProcessingService.validateOrder(order)

                // Then
                assertTrue(result is OrderValidationResult.Error)
                val errorResult = result as OrderValidationResult.Error
                assertTrue(errorResult.message.contains("Shipping address is incomplete"))
        }

        private fun createSampleOrder(): Order {
                return Order(
                        id = OrderId(123456789),
                        orderNumber = "1001",
                        customer =
                                Customer(
                                        id = 123456789,
                                        email = "customer@example.com",
                                        firstName = "John",
                                        lastName = "Doe",
                                        phone = "+1234567890"
                                ),
                        lineItems =
                                listOf(
                                        OrderLineItem(
                                                id = 123456789,
                                                productId = 123456789,
                                                variantId = 123456789,
                                                quantity = 2,
                                                title = "Popular T-Shirt",
                                                sku = "TSHIRT-001",
                                                price = Money(BigDecimal("19.99"), Currency.USD),
                                                totalDiscount =
                                                        Money(BigDecimal("0.00"), Currency.USD),
                                                variantTitle = "Medium / Blue"
                                        )
                                ),
                        shippingAddress =
                                Address(
                                        firstName = "John",
                                        lastName = "Doe",
                                        address1 = "123 Main St",
                                        city = "New York",
                                        province = "NY",
                                        country = "United States",
                                        zip = "10001"
                                ),
                        billingAddress =
                                Address(
                                        firstName = "John",
                                        lastName = "Doe",
                                        address1 = "123 Main St",
                                        city = "New York",
                                        province = "NY",
                                        country = "United States",
                                        zip = "10001"
                                ),
                        totalPrice = Money(BigDecimal("39.98"), Currency.USD),
                        subtotalPrice = Money(BigDecimal("39.98"), Currency.USD),
                        totalTax = Money(BigDecimal("0.00"), Currency.USD),
                        currency = Currency.USD,
                        financialStatus = FinancialStatus.PAID,
                        fulfillmentStatus = null,
                        tags = "",
                        note = "",
                        sourceName = "web",
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now(),
                        processedAt = null,
                        status = OrderStatus.PENDING
                )
        }
}
