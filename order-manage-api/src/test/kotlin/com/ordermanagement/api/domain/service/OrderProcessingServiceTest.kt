package com.ordermanagement.api.domain.service

import com.ordermanagement.api.domain.entity.*
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("OrderProcessingService Tests")
class OrderProcessingServiceTest {

        private lateinit var orderProcessingService: OrderProcessingService

        @BeforeEach
        fun setUp() {
                orderProcessingService = OrderProcessingService()
        }

        @Test
        @DisplayName("Should validate order successfully when all data is valid")
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
        @DisplayName("Should return error when order cannot be fulfilled (not paid)")
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
        @DisplayName("Should return error when customer information is incomplete")
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
        @DisplayName("Should return error when customer email is blank")
        fun `should return error when customer email is blank`() {
                // Given
                val order =
                        createSampleOrder()
                                .copy(customer = createSampleOrder().customer.copy(email = ""))

                // When
                val result = orderProcessingService.validateOrder(order)

                // Then
                assertTrue(result is OrderValidationResult.Error)
                val errorResult = result as OrderValidationResult.Error
                assertTrue(errorResult.message.contains("Customer information is incomplete"))
        }

        @Test
        @DisplayName("Should return error when customer first name is blank")
        fun `should return error when customer first name is blank`() {
                // Given
                val order =
                        createSampleOrder()
                                .copy(customer = createSampleOrder().customer.copy(firstName = ""))

                // When
                val result = orderProcessingService.validateOrder(order)

                // Then
                assertTrue(result is OrderValidationResult.Error)
                val errorResult = result as OrderValidationResult.Error
                assertTrue(errorResult.message.contains("Customer information is incomplete"))
        }

        @Test
        @DisplayName("Should return error when line items are empty")
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
        @DisplayName("Should return error when line item quantity is zero")
        fun `should return error when line item quantity is zero`() {
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
        @DisplayName("Should return error when line item quantity is negative")
        fun `should return error when line item quantity is negative`() {
                // Given
                val order =
                        createSampleOrder()
                                .copy(
                                        lineItems =
                                                listOf(
                                                        createSampleOrder()
                                                                .lineItems
                                                                .first()
                                                                .copy(quantity = -1)
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
        @DisplayName("Should return error when line item SKU is blank")
        fun `should return error when line item SKU is blank`() {
                // Given
                val order =
                        createSampleOrder()
                                .copy(
                                        lineItems =
                                                listOf(
                                                        createSampleOrder()
                                                                .lineItems
                                                                .first()
                                                                .copy(sku = "")
                                                )
                                )

                // When
                val result = orderProcessingService.validateOrder(order)

                // Then
                assertTrue(result is OrderValidationResult.Error)
                val errorResult = result as OrderValidationResult.Error
                assertTrue(errorResult.message.contains("Line item SKU is required"))
        }

        @Test
        @DisplayName("Should return error when shipping address is incomplete")
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

        @Test
        @DisplayName("Should return error when shipping address1 is blank")
        fun `should return error when shipping address1 is blank`() {
                // Given
                val order =
                        createSampleOrder()
                                .copy(
                                        shippingAddress =
                                                createSampleOrder()
                                                        .shippingAddress
                                                        .copy(address1 = "")
                                )

                // When
                val result = orderProcessingService.validateOrder(order)

                // Then
                assertTrue(result is OrderValidationResult.Error)
                val errorResult = result as OrderValidationResult.Error
                assertTrue(errorResult.message.contains("Shipping address is incomplete"))
        }

        @Test
        @DisplayName("Should return error when shipping city is blank")
        fun `should return error when shipping city is blank`() {
                // Given
                val order =
                        createSampleOrder()
                                .copy(
                                        shippingAddress =
                                                createSampleOrder().shippingAddress.copy(city = "")
                                )

                // When
                val result = orderProcessingService.validateOrder(order)

                // Then
                assertTrue(result is OrderValidationResult.Error)
                val errorResult = result as OrderValidationResult.Error
                assertTrue(errorResult.message.contains("Shipping address is incomplete"))
        }

        @Test
        @DisplayName("Should return error when billing address is incomplete")
        fun `should return error when billing address is incomplete`() {
                // Given
                val order =
                        createSampleOrder()
                                .copy(
                                        billingAddress =
                                                createSampleOrder()
                                                        .billingAddress
                                                        .copy(address1 = "", city = "")
                                )

                // When
                val result = orderProcessingService.validateOrder(order)

                // Then
                assertTrue(result is OrderValidationResult.Error)
                val errorResult = result as OrderValidationResult.Error
                assertTrue(errorResult.message.contains("Billing address is incomplete"))
        }

        @Test
        @DisplayName("Should validate order with multiple line items successfully")
        fun `should validate order with multiple line items successfully`() {
                // Given
                val order =
                        createSampleOrder()
                                .copy(
                                        lineItems =
                                                listOf(
                                                        createSampleOrder().lineItems.first(),
                                                        createSampleOrder()
                                                                .lineItems
                                                                .first()
                                                                .copy(
                                                                        id = 987654321,
                                                                        productId = 987654321,
                                                                        variantId = 987654321,
                                                                        quantity = 1,
                                                                        title = "Popular Jeans",
                                                                        sku = "JEANS-001",
                                                                        price =
                                                                                Money(
                                                                                        BigDecimal(
                                                                                                "49.99"
                                                                                        ),
                                                                                        Currency.USD
                                                                                ),
                                                                        variantTitle = "32 / Blue"
                                                                )
                                                )
                                )

                // When
                val result = orderProcessingService.validateOrder(order)

                // Then
                assertTrue(result is OrderValidationResult.Valid)
                val validResult = result as OrderValidationResult.Valid
                assertEquals(2, validResult.order.lineItems.size)
        }

        @Test
        @DisplayName("Should validate order with different currencies successfully")
        fun `should validate order with different currencies successfully`() {
                // Given
                val order =
                        createSampleOrder()
                                .copy(
                                        currency = Currency.EUR,
                                        totalPrice = Money(BigDecimal("35.50"), Currency.EUR),
                                        subtotalPrice = Money(BigDecimal("35.50"), Currency.EUR),
                                        totalTax = Money(BigDecimal("0.00"), Currency.EUR)
                                )

                // When
                val result = orderProcessingService.validateOrder(order)

                // Then
                assertTrue(result is OrderValidationResult.Valid)
                val validResult = result as OrderValidationResult.Valid
                assertEquals(Currency.EUR, validResult.order.currency)
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
