package com.ordermanagement.streamprocess.domain.service

import com.ordermanagement.streamprocess.domain.entity.InventoryUpdate
import com.ordermanagement.streamprocess.domain.valueobject.LocationId
import com.ordermanagement.streamprocess.domain.valueobject.ProductId
import com.ordermanagement.streamprocess.domain.valueobject.Quantity
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InventoryProcessingServiceTest {

    private val service = InventoryProcessingService()

    @Test
    fun `should validate valid inventory update`() {
        val inventoryUpdate =
                InventoryUpdate(
                        productId = ProductId(123456789L),
                        quantity = Quantity(10),
                        locationId = LocationId(987654321L)
                )

        val isValid = service.validateInventoryUpdate(inventoryUpdate)

        assertTrue(isValid)
    }

    @Test
    fun `should create valid Shopify inventory request`() {
        val inventoryUpdate =
                InventoryUpdate(
                        productId = ProductId(123456789L),
                        quantity = Quantity(95),
                        locationId = LocationId(987654321L)
                )

        val shopifyRequest = service.createShopifyInventoryRequest(inventoryUpdate)

        assertEquals(987654321L, shopifyRequest.locationId)
        assertEquals(123456789L, shopifyRequest.inventoryItemId)
        assertEquals(95, shopifyRequest.available)
    }

    @Test
    fun `should throw exception for invalid product id`() {
        assertThrows<IllegalArgumentException> { ProductId(-1L) }
    }

    @Test
    fun `should throw exception for negative quantity`() {
        assertThrows<IllegalArgumentException> { Quantity(-1) }
    }

    @Test
    fun `should throw exception for invalid location id`() {
        assertThrows<IllegalArgumentException> { LocationId(0L) }
    }
}
