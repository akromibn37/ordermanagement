package com.ordermanagement.streamprocess.domain.valueobject

@JvmInline
value class ProductId(val value: Long) {
    init {
        require(value > 0) { "Product ID must be positive" }
    }
}
