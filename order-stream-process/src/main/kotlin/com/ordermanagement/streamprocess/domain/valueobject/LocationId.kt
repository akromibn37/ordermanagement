package com.ordermanagement.streamprocess.domain.valueobject

@JvmInline
value class LocationId(val value: Long) {
    init {
        require(value > 0) { "Location ID must be positive" }
    }
}
