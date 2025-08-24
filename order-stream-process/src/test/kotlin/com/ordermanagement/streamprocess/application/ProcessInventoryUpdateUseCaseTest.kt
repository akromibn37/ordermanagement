package com.ordermanagement.streamprocess.application

import com.ordermanagement.streamprocess.application.port.out.ShopifyApiPort
import com.ordermanagement.streamprocess.application.port.out.ShopifyApiResult
import com.ordermanagement.streamprocess.domain.entity.InventoryLevel
import com.ordermanagement.streamprocess.domain.entity.InventoryUpdate
import com.ordermanagement.streamprocess.domain.entity.ShopifyInventoryResponse
import com.ordermanagement.streamprocess.domain.service.InventoryProcessingService
import com.ordermanagement.streamprocess.domain.valueobject.LocationId
import com.ordermanagement.streamprocess.domain.valueobject.ProductId
import com.ordermanagement.streamprocess.domain.valueobject.Quantity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ProcessInventoryUpdateUseCase Tests")
class ProcessInventoryUpdateUseCaseTest {

    private lateinit var inventoryProcessingService: InventoryProcessingService
    private lateinit var shopifyApiPort: ShopifyApiPort
    private lateinit var useCase: ProcessInventoryUpdateUseCase

    @BeforeEach
    fun setUp() {
        inventoryProcessingService = mockk()
        shopifyApiPort = mockk()
        useCase = ProcessInventoryUpdateUseCase(inventoryProcessingService, shopifyApiPort)
    }

    @Test
    @DisplayName("Should process valid inventory update successfully")
    fun `should process valid inventory update successfully`() = runBlocking {
        // Given
        val inventoryUpdate = createSampleInventoryUpdate()
        val shopifyRequest = inventoryUpdate.toShopifyInventoryRequest()
        val shopifyResult = ShopifyApiResult.Success(createSampleShopifyInventoryResponse())

        coEvery { inventoryProcessingService.validateInventoryUpdate(inventoryUpdate) } returns true
        coEvery {
            inventoryProcessingService.createShopifyInventoryRequest(inventoryUpdate)
        } returns shopifyRequest
        coEvery { shopifyApiPort.updateInventory(shopifyRequest) } returns shopifyResult

        // When
        val result = useCase.processInventoryUpdate(inventoryUpdate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("Inventory update processed successfully", result.message)
        assertEquals(123456789L, result.productId)
    }

    @Test
    @DisplayName("Should return validation failure for invalid inventory update")
    fun `should return validation failure for invalid inventory update`() = runBlocking {
        // Given
        val inventoryUpdate = createSampleInventoryUpdate()

        coEvery { inventoryProcessingService.validateInventoryUpdate(inventoryUpdate) } returns
                false

        // When
        val result = useCase.processInventoryUpdate(inventoryUpdate)

        // Then
        assertFalse(result.isSuccess)
        assertEquals("Invalid inventory update data", result.message)
        assertEquals(123456789L, result.productId)
    }

    @Test
    @DisplayName("Should handle Shopify API success response")
    fun `should handle Shopify API success response`() = runBlocking {
        // Given
        val inventoryUpdate = createSampleInventoryUpdate()
        val shopifyRequest = inventoryUpdate.toShopifyInventoryRequest()
        val shopifyResult = ShopifyApiResult.Success(createSampleShopifyInventoryResponse())

        coEvery { inventoryProcessingService.validateInventoryUpdate(inventoryUpdate) } returns true
        coEvery {
            inventoryProcessingService.createShopifyInventoryRequest(inventoryUpdate)
        } returns shopifyRequest
        coEvery { shopifyApiPort.updateInventory(shopifyRequest) } returns shopifyResult

        // When
        val result = useCase.processInventoryUpdate(inventoryUpdate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("Inventory update processed successfully", result.message)
        assertEquals(123456789L, result.productId)
    }

    @Test
    @DisplayName("Should handle Shopify API error response")
    fun `should handle Shopify API error response`() = runBlocking {
        // Given
        val inventoryUpdate = createSampleInventoryUpdate()
        val shopifyRequest = inventoryUpdate.toShopifyInventoryRequest()
        val shopifyResult = ShopifyApiResult.Error("API rate limit exceeded")

        coEvery { inventoryProcessingService.validateInventoryUpdate(inventoryUpdate) } returns true
        coEvery {
            inventoryProcessingService.createShopifyInventoryRequest(inventoryUpdate)
        } returns shopifyRequest
        coEvery { shopifyApiPort.updateInventory(shopifyRequest) } returns shopifyResult

        // When
        val result = useCase.processInventoryUpdate(inventoryUpdate)

        // Then
        assertFalse(result.isSuccess)
        assertEquals("Shopify API error: API rate limit exceeded", result.message)
        assertNull(result.productId)
    }

    @Test
    @DisplayName("Should handle Shopify API connection timeout")
    fun `should handle Shopify API connection timeout`() = runBlocking {
        // Given
        val inventoryUpdate = createSampleInventoryUpdate()
        val shopifyRequest = inventoryUpdate.toShopifyInventoryRequest()
        val shopifyResult = ShopifyApiResult.Error("Connection timeout")

        coEvery { inventoryProcessingService.validateInventoryUpdate(inventoryUpdate) } returns true
        coEvery {
            inventoryProcessingService.createShopifyInventoryRequest(inventoryUpdate)
        } returns shopifyRequest
        coEvery { shopifyApiPort.updateInventory(shopifyRequest) } returns shopifyResult

        // When
        val result = useCase.processInventoryUpdate(inventoryUpdate)

        // Then
        assertFalse(result.isSuccess)
        assertEquals("Shopify API error: Connection timeout", result.message)
        assertNull(result.productId)
    }

    @Test
    @DisplayName("Should handle exception during processing")
    fun `should handle exception during processing`() = runBlocking {
        // Given
        val inventoryUpdate = createSampleInventoryUpdate()
        val exception = RuntimeException("Database connection failed")

        coEvery { inventoryProcessingService.validateInventoryUpdate(inventoryUpdate) } throws
                exception

        // When
        val result = useCase.processInventoryUpdate(inventoryUpdate)

        // Then
        assertFalse(result.isSuccess)
        assertEquals(
                "Error processing inventory update: Database connection failed",
                result.message
        )
        assertEquals(123456789L, result.productId)
    }

    @Test
    @DisplayName("Should process inventory update with zero quantity")
    fun `should process inventory update with zero quantity`() = runBlocking {
        // Given
        val inventoryUpdate = createSampleInventoryUpdate().copy(quantity = Quantity(0))
        val shopifyRequest = inventoryUpdate.toShopifyInventoryRequest()
        val shopifyResult = ShopifyApiResult.Success(createSampleShopifyInventoryResponse())

        coEvery { inventoryProcessingService.validateInventoryUpdate(inventoryUpdate) } returns true
        coEvery {
            inventoryProcessingService.createShopifyInventoryRequest(inventoryUpdate)
        } returns shopifyRequest
        coEvery { shopifyApiPort.updateInventory(shopifyRequest) } returns shopifyResult

        // When
        val result = useCase.processInventoryUpdate(inventoryUpdate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("Inventory update processed successfully", result.message)
        assertEquals(0, shopifyRequest.available)
    }

    @Test
    @DisplayName("Should process inventory update with large values")
    fun `should process inventory update with large values`() = runBlocking {
        // Given
        val inventoryUpdate =
                createSampleInventoryUpdate()
                        .copy(
                                productId = ProductId(999999999999L),
                                quantity = Quantity(999999),
                                locationId = LocationId(888888888888L)
                        )
        val shopifyRequest = inventoryUpdate.toShopifyInventoryRequest()
        val shopifyResult = ShopifyApiResult.Success(createSampleShopifyInventoryResponse())

        coEvery { inventoryProcessingService.validateInventoryUpdate(inventoryUpdate) } returns true
        coEvery {
            inventoryProcessingService.createShopifyInventoryRequest(inventoryUpdate)
        } returns shopifyRequest
        coEvery { shopifyApiPort.updateInventory(shopifyRequest) } returns shopifyResult

        // When
        val result = useCase.processInventoryUpdate(inventoryUpdate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(999999999999L, shopifyRequest.inventoryItemId)
        assertEquals(888888888888L, shopifyRequest.locationId)
        assertEquals(999999, shopifyRequest.available)
    }

    @Test
    @DisplayName("Should handle validation failure for invalid inventory update")
    fun `should handle validation failure for invalid inventory update`() = runBlocking {
        // Given
        val inventoryUpdate = createSampleInventoryUpdate()

        coEvery { inventoryProcessingService.validateInventoryUpdate(inventoryUpdate) } returns
                false

        // When
        val result = useCase.processInventoryUpdate(inventoryUpdate)

        // Then
        assertFalse(result.isSuccess)
        assertEquals("Invalid inventory update data", result.message)
        assertEquals(123456789L, result.productId)
    }

    @Test
    @DisplayName("Should handle Shopify API error with empty message")
    fun `should handle Shopify API error with empty message`() = runBlocking {
        // Given
        val inventoryUpdate = createSampleInventoryUpdate()
        val shopifyRequest = inventoryUpdate.toShopifyInventoryRequest()
        val shopifyResult = ShopifyApiResult.Error("")

        coEvery { inventoryProcessingService.validateInventoryUpdate(inventoryUpdate) } returns true
        coEvery {
            inventoryProcessingService.createShopifyInventoryRequest(inventoryUpdate)
        } returns shopifyRequest
        coEvery { shopifyApiPort.updateInventory(shopifyRequest) } returns shopifyResult

        // When
        val result = useCase.processInventoryUpdate(inventoryUpdate)

        // Then
        assertFalse(result.isSuccess)
        assertEquals("Shopify API error: ", result.message)
        assertNull(result.productId)
    }

    private fun createSampleInventoryUpdate(): InventoryUpdate {
        return InventoryUpdate(
                productId = ProductId(123456789L),
                quantity = Quantity(100),
                locationId = LocationId(987654321L)
        )
    }

    private fun createSampleShopifyInventoryResponse(): ShopifyInventoryResponse {
        return ShopifyInventoryResponse(
                inventoryLevel =
                        InventoryLevel(
                                inventoryItemId = 123456789L,
                                locationId = 987654321L,
                                available = 100,
                                updatedAt = "2024-01-15T10:00:00Z"
                        )
        )
    }
}
