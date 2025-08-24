package com.ordermanagement.streamprocess.domain.service

import com.ordermanagement.streamprocess.domain.entity.InventoryUpdate
import com.ordermanagement.streamprocess.domain.valueobject.LocationId
import com.ordermanagement.streamprocess.domain.valueobject.ProductId
import com.ordermanagement.streamprocess.domain.valueobject.Quantity
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("InventoryProcessingService Tests")
class InventoryProcessingServiceTest {

        private lateinit var service: InventoryProcessingService

        @BeforeEach
        fun setUp() {
                service = InventoryProcessingService()
        }

        @Test
        @DisplayName("Should validate valid inventory update with positive values")
        fun `should validate valid inventory update with positive values`() {
                // Given
                val inventoryUpdate =
                        InventoryUpdate(
                                productId = ProductId(123456789L),
                                quantity = Quantity(10),
                                locationId = LocationId(987654321L)
                        )

                // When
                val isValid = service.validateInventoryUpdate(inventoryUpdate)

                // Then
                assertTrue(isValid)
        }

        @Test
        @DisplayName("Should validate valid inventory update with zero quantity")
        fun `should validate valid inventory update with zero quantity`() {
                // Given
                val inventoryUpdate =
                        InventoryUpdate(
                                productId = ProductId(123456789L),
                                quantity = Quantity(0),
                                locationId = LocationId(987654321L)
                        )

                // When
                val isValid = service.validateInventoryUpdate(inventoryUpdate)

                // Then
                assertTrue(isValid)
        }

        @Test
        @DisplayName("Should validate inventory update with minimum valid values")
        fun `should validate inventory update with minimum valid values`() {
                // Given
                val inventoryUpdate =
                        InventoryUpdate(
                                productId = ProductId(1L), // Minimum valid product ID
                                quantity = Quantity(0), // Minimum valid quantity
                                locationId = LocationId(1L) // Minimum valid location ID
                        )

                // When
                val isValid = service.validateInventoryUpdate(inventoryUpdate)

                // Then
                assertTrue(isValid)
        }

        @Test
        @DisplayName("Should validate inventory update with large values")
        fun `should validate inventory update with large values`() {
                // Given
                val inventoryUpdate =
                        InventoryUpdate(
                                productId = ProductId(999999999999L),
                                quantity = Quantity(999999),
                                locationId = LocationId(888888888888L)
                        )

                // When
                val isValid = service.validateInventoryUpdate(inventoryUpdate)

                // Then
                assertTrue(isValid)
        }

        @Test
        @DisplayName("Should create valid Shopify inventory request")
        fun `should create valid Shopify inventory request`() {
                // Given
                val inventoryUpdate =
                        InventoryUpdate(
                                productId = ProductId(123456789L),
                                quantity = Quantity(95),
                                locationId = LocationId(987654321L)
                        )

                // When
                val shopifyRequest = service.createShopifyInventoryRequest(inventoryUpdate)

                // Then
                assertEquals(987654321L, shopifyRequest.locationId)
                assertEquals(123456789L, shopifyRequest.inventoryItemId)
                assertEquals(95, shopifyRequest.available)
        }

        @Test
        @DisplayName("Should create Shopify inventory request with zero quantity")
        fun `should create Shopify inventory request with zero quantity`() {
                // Given
                val inventoryUpdate =
                        InventoryUpdate(
                                productId = ProductId(555666777L),
                                quantity = Quantity(0),
                                locationId = LocationId(111222333L)
                        )

                // When
                val shopifyRequest = service.createShopifyInventoryRequest(inventoryUpdate)

                // Then
                assertEquals(111222333L, shopifyRequest.locationId)
                assertEquals(555666777L, shopifyRequest.inventoryItemId)
                assertEquals(0, shopifyRequest.available)
        }

        @Test
        @DisplayName("Should create Shopify inventory request with large values")
        fun `should create Shopify inventory request with large values`() {
                // Given
                val inventoryUpdate =
                        InventoryUpdate(
                                productId = ProductId(999999999999L),
                                quantity = Quantity(999999),
                                locationId = LocationId(888888888888L)
                        )

                // When
                val shopifyRequest = service.createShopifyInventoryRequest(inventoryUpdate)

                // Then
                assertEquals(888888888888L, shopifyRequest.locationId)
                assertEquals(999999999999L, shopifyRequest.inventoryItemId)
                assertEquals(999999, shopifyRequest.available)
        }

        @Test
        @DisplayName("Should throw exception for invalid product id")
        fun `should throw exception for invalid product id`() {
                // Then
                assertThrows<IllegalArgumentException> { ProductId(-1L) }
                assertThrows<IllegalArgumentException> { ProductId(0L) }
        }

        @Test
        @DisplayName("Should throw exception for negative quantity")
        fun `should throw exception for negative quantity`() {
                // Then
                assertThrows<IllegalArgumentException> { Quantity(-1) }
                assertThrows<IllegalArgumentException> { Quantity(-100) }
        }

        @Test
        @DisplayName("Should throw exception for invalid location id")
        fun `should throw exception for invalid location id`() {
                // Then
                assertThrows<IllegalArgumentException> { LocationId(0L) }
                assertThrows<IllegalArgumentException> { LocationId(-1L) }
        }

        @Test
        @DisplayName("Should accept valid value objects")
        fun `should accept valid value objects`() {
                // Then
                val productId = ProductId(1L)
                val quantity = Quantity(0)
                val locationId = LocationId(1L)

                assertEquals(1L, productId.value)
                assertEquals(0, quantity.value)
                assertEquals(1L, locationId.value)
        }

        @Test
        @DisplayName("Should handle boundary values correctly")
        fun `should handle boundary values correctly`() {
                // Given
                val inventoryUpdate =
                        InventoryUpdate(
                                productId = ProductId(1L), // Minimum valid product ID
                                quantity = Quantity(0), // Minimum valid quantity
                                locationId = LocationId(1L) // Minimum valid location ID
                        )

                // When
                val isValid = service.validateInventoryUpdate(inventoryUpdate)

                // Then
                assertTrue(isValid)
        }
}
