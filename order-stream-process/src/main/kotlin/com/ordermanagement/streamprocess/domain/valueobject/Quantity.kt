package com.ordermanagement.streamprocess.domain.valueobject

@JvmInline
value class Quantity(val value: Int) {
    init {
        require(value >= 0) { "Quantity must be non-negative" }
    }
}
