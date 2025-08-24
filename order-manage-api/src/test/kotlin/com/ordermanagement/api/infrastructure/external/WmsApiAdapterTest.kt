package com.ordermanagement.api.infrastructure.external

import com.ordermanagement.api.domain.entity.*
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

@DisplayName("WmsApiAdapter Tests")
class WmsApiAdapterTest {

        private lateinit var webClient: WebClient
        private lateinit var wmsApiAdapter: WmsApiAdapter

        @BeforeEach
        fun setUp() {
                webClient = mockk()
                wmsApiAdapter = WmsApiAdapter(webClient)
        }

        @Test
        @DisplayName("Should create sample order correctly")
        fun `should create sample order correctly`() {
                // Given & When
                val order = createSampleOrder()

                // Then
                assertEquals("1001", order.orderNumber)
                assertEquals("customer@example.com", order.customer.email)
                assertEquals("John", order.customer.firstName)
                assertEquals("Doe", order.customer.lastName)
                assertEquals(1, order.lineItems.size)
                assertEquals("TSHIRT-001", order.lineItems.first().sku)
                assertEquals(2, order.lineItems.first().quantity)
                assertEquals("123 Main St", order.shippingAddress.address1)
                assertEquals("New York", order.shippingAddress.city)
                assertEquals(Currency.USD, order.currency)
                assertEquals(FinancialStatus.PAID, order.financialStatus)
        }

        @Test
        @DisplayName("Should create order with multiple line items")
        fun `should create order with multiple line items`() {
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

                // Then
                assertEquals(2, order.lineItems.size)
                assertEquals("TSHIRT-001", order.lineItems[0].sku)
                assertEquals("JEANS-001", order.lineItems[1].sku)
                assertEquals(2, order.lineItems[0].quantity)
                assertEquals(1, order.lineItems[1].quantity)
        }

        @Test
        @DisplayName("Should create order with different currency")
        fun `should create order with different currency`() {
                // Given
                val order =
                        createSampleOrder()
                                .copy(
                                        currency = Currency.EUR,
                                        totalPrice = Money(BigDecimal("35.50"), Currency.EUR),
                                        subtotalPrice = Money(BigDecimal("35.50"), Currency.EUR),
                                        totalTax = Money(BigDecimal("0.00"), Currency.EUR)
                                )

                // Then
                assertEquals(Currency.EUR, order.currency)
                assertEquals(BigDecimal("35.50"), order.totalPrice.amount)
                assertEquals(Currency.EUR, order.totalPrice.currency)
        }

        @Test
        @DisplayName("Should validate order structure")
        fun `should validate order structure`() {
                // Given
                val order = createSampleOrder()

                // Then
                assertNotNull(order.id)
                assertNotNull(order.customer)
                assertNotNull(order.shippingAddress)
                assertNotNull(order.billingAddress)
                assertFalse(order.lineItems.isEmpty())
                assertTrue(order.totalPrice.amount > BigDecimal.ZERO)
                assertTrue(order.customer.email.isNotBlank())
                assertTrue(order.customer.firstName.isNotBlank())
                assertTrue(order.shippingAddress.address1.isNotBlank())
                assertTrue(order.shippingAddress.city.isNotBlank())
        }

        @Test
        @DisplayName("Should create order with different statuses")
        fun `should create order with different statuses`() {
                // Given
                val pendingOrder = createSampleOrder().copy(status = OrderStatus.PENDING)
                val processingOrder = createSampleOrder().copy(status = OrderStatus.PROCESSING)
                val fulfilledOrder = createSampleOrder().copy(status = OrderStatus.FULFILLED)

                // Then
                assertEquals(OrderStatus.PENDING, pendingOrder.status)
                assertEquals(OrderStatus.PROCESSING, processingOrder.status)
                assertEquals(OrderStatus.FULFILLED, fulfilledOrder.status)
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
                                                productId = 12345,
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
