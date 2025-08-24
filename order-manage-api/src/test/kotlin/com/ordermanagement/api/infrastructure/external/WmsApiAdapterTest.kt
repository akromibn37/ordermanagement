package com.ordermanagement.api.infrastructure.external

import com.ordermanagement.api.domain.entity.*
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockk.every
import org.mockk.mockk
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec

class WmsApiAdapterTest {

        private lateinit var webClient: WebClient
        private lateinit var wmsApiAdapter: WmsApiAdapter

        @BeforeEach
        fun setUp() {
                webClient = mockk()
                wmsApiAdapter = WmsApiAdapter(webClient)
        }

        @Test
        fun `should create fulfillment order successfully`() {
                // Given
                val order = createSampleOrder()
                val mockRequestHeadersUriSpec = mockk<RequestBodyUriSpec<*>>()
                val mockResponseSpec = mockk<ResponseSpec>()

                val fulfillmentResponse =
                        WmsFulfillmentResponse(
                                fulfillmentOrderId = 98765,
                                status = "pending",
                                referenceId = "1001",
                                estimatedShipDate = "2024-01-16T10:00:00Z",
                                trackingNumber = null
                        )

                every { webClient.post() } returns mockRequestHeadersUriSpec
                every { mockRequestHeadersUriSpec.uri("/api/v1/fulfillment-orders") } returns
                        mockRequestHeadersUriSpec
                every { mockRequestHeadersUriSpec.header("Authorization", "Bearer null") } returns
                        mockRequestHeadersUriSpec
                every { mockRequestHeadersUriSpec.bodyValue(any<WmsFulfillmentRequest>()) } returns
                        mockRequestHeadersUriSpec
                every { mockRequestHeadersUriSpec.retrieve() } returns mockResponseSpec
                every { mockResponseSpec.awaitBody<WmsFulfillmentResponse>() } returns
                        fulfillmentResponse

                // When
                val result = wmsApiAdapter.createFulfillmentOrder(order)

                // Then
                assertTrue(result.isSuccess)
                assertEquals("98765", result.fulfillmentId)
                assertTrue(result.message?.contains("pending") == true)
        }

        @Test
        fun `should handle fulfillment order creation failure`() {
                // Given
                val order = createSampleOrder()
                val mockRequestHeadersUriSpec = mockk<RequestBodyUriSpec<*>>()
                val mockResponseSpec = mockk<ResponseSpec>()

                val fulfillmentResponse =
                        WmsFulfillmentResponse(
                                fulfillmentOrderId = 98765,
                                status = "failed",
                                referenceId = "1001",
                                estimatedShipDate = "2024-01-16T10:00:00Z",
                                trackingNumber = null
                        )

                every { webClient.post() } returns mockRequestHeadersUriSpec
                every { mockRequestHeadersUriSpec.uri("/api/v1/fulfillment-orders") } returns
                        mockRequestHeadersUriSpec
                every { mockRequestHeadersUriSpec.header("Authorization", "Bearer null") } returns
                        mockRequestHeadersUriSpec
                every { mockRequestHeadersUriSpec.bodyValue(any<WmsFulfillmentRequest>()) } returns
                        mockRequestHeadersUriSpec
                every { mockRequestHeadersUriSpec.retrieve() } returns mockResponseSpec
                every { mockResponseSpec.awaitBody<WmsFulfillmentResponse>() } returns
                        fulfillmentResponse

                // When
                val result = wmsApiAdapter.createFulfillmentOrder(order)

                // Then
                assertFalse(result.isSuccess)
                assertEquals("98765", result.fulfillmentId)
                assertTrue(result.message?.contains("failed") == true)
        }

        @Test
        fun `should get product inventory successfully`() {
                // Given
                val productId = 12345L
                val mockRequestHeadersUriSpec = mockk<RequestHeadersUriSpec<*>>()
                val mockResponseSpec = mockk<ResponseSpec>()

                val inventoryResponse =
                        WmsInventoryApiResponse(
                                productId = 12345,
                                sku = "TSHIRT-001",
                                availableQuantity = 95,
                                reservedQuantity = 5,
                                totalQuantity = 100,
                                locationId = 67890,
                                lastUpdated = "2024-01-15T10:30:00Z"
                        )

                every { webClient.get() } returns mockRequestHeadersUriSpec
                every {
                        mockRequestHeadersUriSpec.uri("/api/v1/products/$productId/inventory")
                } returns mockRequestHeadersUriSpec
                every { mockRequestHeadersUriSpec.header("Authorization", "Bearer null") } returns
                        mockRequestHeadersUriSpec
                every { mockRequestHeadersUriSpec.retrieve() } returns mockResponseSpec
                every { mockResponseSpec.awaitBody<WmsInventoryApiResponse>() } returns
                        inventoryResponse

                // When
                val result = wmsApiAdapter.getProductInventory(productId)

                // Then
                assertEquals(12345L, result.productId)
                assertEquals(95, result.availableQuantity)
                assertEquals(100, result.totalQuantity)
                assertEquals(5, result.reservedQuantity)
                assertEquals("TSHIRT-001", result.sku)
                assertEquals(67890L, result.locationId)
                assertEquals("2024-01-15T10:30:00Z", result.lastUpdated)
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
