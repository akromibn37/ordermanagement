package com.ordermanagement.streamprocess.domain.entity

import com.ordermanagement.streamprocess.domain.valueobject.LocationId
import com.ordermanagement.streamprocess.domain.valueobject.ProductId
import com.ordermanagement.streamprocess.domain.valueobject.Quantity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("InventoryUpdate Entity Tests")
class InventoryUpdateTest {

    @Test
    @DisplayName("Should create valid inventory update")
    fun `should create valid inventory update`() {
        // Given
        val productId = ProductId(123456789L)
        val quantity = Quantity(100)
        val locationId = LocationId(987654321L)

        // When
        val inventoryUpdate = InventoryUpdate(productId, quantity, locationId)

        // Then
        assertEquals(productId, inventoryUpdate.productId)
        assertEquals(quantity, inventoryUpdate.quantity)
        assertEquals(locationId, inventoryUpdate.locationId)
    }

    @Test
    @DisplayName("Should convert to Shopify inventory request correctly")
    fun `should convert to Shopify inventory request correctly`() {
        // Given
        val inventoryUpdate =
                InventoryUpdate(
                        productId = ProductId(123456789L),
                        quantity = Quantity(95),
                        locationId = LocationId(987654321L)
                )

        // When
        val shopifyRequest = inventoryUpdate.toShopifyInventoryRequest()

        // Then
        assertEquals(987654321L, shopifyRequest.locationId)
        assertEquals(123456789L, shopifyRequest.inventoryItemId)
        assertEquals(95, shopifyRequest.available)
    }

    @Test
    @DisplayName("Should convert to Shopify inventory request with zero quantity")
    fun `should convert to Shopify inventory request with zero quantity`() {
        // Given
        val inventoryUpdate =
                InventoryUpdate(
                        productId = ProductId(555666777L),
                        quantity = Quantity(0),
                        locationId = LocationId(111222333L)
                )

        // When
        val shopifyRequest = inventoryUpdate.toShopifyInventoryRequest()

        // Then
        assertEquals(111222333L, shopifyRequest.locationId)
        assertEquals(555666777L, shopifyRequest.inventoryItemId)
        assertEquals(0, shopifyRequest.available)
    }

    @Test
    @DisplayName("Should convert to Shopify inventory request with large values")
    fun `should convert to Shopify inventory request with large values`() {
        // Given
        val inventoryUpdate =
                InventoryUpdate(
                        productId = ProductId(999999999999L),
                        quantity = Quantity(999999),
                        locationId = LocationId(888888888888L)
                )

        // When
        val shopifyRequest = inventoryUpdate.toShopifyInventoryRequest()

        // Then
        assertEquals(888888888888L, shopifyRequest.locationId)
        assertEquals(999999999999L, shopifyRequest.inventoryItemId)
        assertEquals(999999, shopifyRequest.available)
    }

    @Test
    @DisplayName("Should handle copy operations correctly")
    fun `should handle copy operations correctly`() {
        // Given
        val original =
                InventoryUpdate(
                        productId = ProductId(123456789L),
                        quantity = Quantity(100),
                        locationId = LocationId(987654321L)
                )

        // When
        val modified = original.copy(quantity = Quantity(50), locationId = LocationId(111222333L))

        // Then
        assertEquals(original.productId, modified.productId)
        assertEquals(Quantity(50), modified.quantity)
        assertEquals(LocationId(111222333L), modified.locationId)

        // Original should remain unchanged
        assertEquals(Quantity(100), original.quantity)
        assertEquals(LocationId(987654321L), original.locationId)
    }

    @Test
    @DisplayName("Should create ShopifyInventoryRequest with correct structure")
    fun `should create ShopifyInventoryRequest with correct structure`() {
        // Given
        val inventoryUpdate =
                InventoryUpdate(
                        productId = ProductId(123456789L),
                        quantity = Quantity(75),
                        locationId = LocationId(987654321L)
                )

        // When
        val shopifyRequest = inventoryUpdate.toShopifyInventoryRequest()

        // Then
        assertNotNull(shopifyRequest)
        assertTrue(shopifyRequest is ShopifyInventoryRequest)
        assertEquals(987654321L, shopifyRequest.locationId)
        assertEquals(123456789L, shopifyRequest.inventoryItemId)
        assertEquals(75, shopifyRequest.available)
    }

    @Test
    @DisplayName("Should create ShopifyInventoryResponse with correct structure")
    fun `should create ShopifyInventoryResponse with correct structure`() {
        // Given
        val inventoryLevel =
                InventoryLevel(
                        inventoryItemId = 123456789L,
                        locationId = 987654321L,
                        available = 100,
                        updatedAt = "2024-01-15T10:00:00Z"
                )

        // When
        val response = ShopifyInventoryResponse(inventoryLevel)

        // Then
        assertNotNull(response)
        assertNotNull(response.inventoryLevel)
        assertEquals(123456789L, response.inventoryLevel.inventoryItemId)
        assertEquals(987654321L, response.inventoryLevel.locationId)
        assertEquals(100, response.inventoryLevel.available)
        assertEquals("2024-01-15T10:00:00Z", response.inventoryLevel.updatedAt)
    }
}
