package com.ordermanagement.streamprocess.domain.valueobject

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Value Object Tests")
class ValueObjectTest {

    @DisplayName("ProductId Tests")
    @Nested
    inner class ProductIdTests {

        @Test
        @DisplayName("Should create valid product ID with positive value")
        fun `should create valid product ID with positive value`() {
            // Given & When
            val productId = ProductId(123456789L)

            // Then
            assertEquals(123456789L, productId.value)
        }

        @Test
        @DisplayName("Should create valid product ID with minimum value")
        fun `should create valid product ID with minimum value`() {
            // Given & When
            val productId = ProductId(1L)

            // Then
            assertEquals(1L, productId.value)
        }

        @Test
        @DisplayName("Should throw exception for zero product ID")
        fun `should throw exception for zero product ID`() {
            // Then
            val exception = assertThrows<IllegalArgumentException> { ProductId(0L) }
            assertEquals("Product ID must be positive", exception.message)
        }

        @Test
        @DisplayName("Should throw exception for negative product ID")
        fun `should throw exception for negative product ID`() {
            // Then
            val exception = assertThrows<IllegalArgumentException> { ProductId(-1L) }
            assertEquals("Product ID must be positive", exception.message)
        }

        @Test
        @DisplayName("Should throw exception for large negative product ID")
        fun `should throw exception for large negative product ID`() {
            // Then
            val exception = assertThrows<IllegalArgumentException> { ProductId(-999999999L) }
            assertEquals("Product ID must be positive", exception.message)
        }

        @Test
        @DisplayName("Should handle large positive product ID")
        fun `should handle large positive product ID`() {
            // Given & When
            val productId = ProductId(999999999999L)

            // Then
            assertEquals(999999999999L, productId.value)
        }
    }

    @DisplayName("Quantity Tests")
    @Nested
    inner class QuantityTests {

        @Test
        @DisplayName("Should create valid quantity with positive value")
        fun `should create valid quantity with positive value`() {
            // Given & When
            val quantity = Quantity(100)

            // Then
            assertEquals(100, quantity.value)
        }

        @Test
        @DisplayName("Should create valid quantity with zero value")
        fun `should create valid quantity with zero value`() {
            // Given & When
            val quantity = Quantity(0)

            // Then
            assertEquals(0, quantity.value)
        }

        @Test
        @DisplayName("Should create valid quantity with minimum value")
        fun `should create valid quantity with minimum value`() {
            // Given & When
            val quantity = Quantity(0)

            // Then
            assertEquals(0, quantity.value)
        }

        @Test
        @DisplayName("Should throw exception for negative quantity")
        fun `should throw exception for negative quantity`() {
            // Then
            val exception = assertThrows<IllegalArgumentException> { Quantity(-1) }
            assertEquals("Quantity must be non-negative", exception.message)
        }

        @Test
        @DisplayName("Should throw exception for large negative quantity")
        fun `should throw exception for large negative quantity`() {
            // Then
            val exception = assertThrows<IllegalArgumentException> { Quantity(-999999) }
            assertEquals("Quantity must be non-negative", exception.message)
        }

        @Test
        @DisplayName("Should handle large positive quantity")
        fun `should handle large positive quantity`() {
            // Given & When
            val quantity = Quantity(999999)

            // Then
            assertEquals(999999, quantity.value)
        }
    }

    @DisplayName("LocationId Tests")
    @Nested
    inner class LocationIdTests {

        @Test
        @DisplayName("Should create valid location ID with positive value")
        fun `should create valid location ID with positive value`() {
            // Given & When
            val locationId = LocationId(987654321L)

            // Then
            assertEquals(987654321L, locationId.value)
        }

        @Test
        @DisplayName("Should create valid location ID with minimum value")
        fun `should create valid location ID with minimum value`() {
            // Given & When
            val locationId = LocationId(1L)

            // Then
            assertEquals(1L, locationId.value)
        }

        @Test
        @DisplayName("Should throw exception for zero location ID")
        fun `should throw exception for zero location ID`() {
            // Then
            val exception = assertThrows<IllegalArgumentException> { LocationId(0L) }
            assertEquals("Location ID must be positive", exception.message)
        }

        @Test
        @DisplayName("Should throw exception for negative location ID")
        fun `should throw exception for negative location ID`() {
            // Then
            val exception = assertThrows<IllegalArgumentException> { LocationId(-1L) }
            assertEquals("Location ID must be positive", exception.message)
        }

        @Test
        @DisplayName("Should throw exception for large negative location ID")
        fun `should throw exception for large negative location ID`() {
            // Then
            val exception = assertThrows<IllegalArgumentException> { LocationId(-999999999L) }
            assertEquals("Location ID must be positive", exception.message)
        }

        @Test
        @DisplayName("Should handle large positive location ID")
        fun `should handle large positive location ID`() {
            // Given & When
            val locationId = LocationId(888888888888L)

            // Then
            assertEquals(888888888888L, locationId.value)
        }
    }

    @Test
    @DisplayName("Should create all valid value objects together")
    fun `should create all valid value objects together`() {
        // Given & When
        val productId = ProductId(123456789L)
        val quantity = Quantity(100)
        val locationId = LocationId(987654321L)

        // Then
        assertEquals(123456789L, productId.value)
        assertEquals(100, quantity.value)
        assertEquals(987654321L, locationId.value)
    }

    @Test
    @DisplayName("Should handle edge case values correctly")
    fun `should handle edge case values correctly`() {
        // Given & When
        val productId = ProductId(1L)
        val quantity = Quantity(0)
        val locationId = LocationId(1L)

        // Then
        assertEquals(1L, productId.value)
        assertEquals(0, quantity.value)
        assertEquals(1L, locationId.value)
    }
}
