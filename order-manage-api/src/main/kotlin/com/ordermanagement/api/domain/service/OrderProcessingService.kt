package com.ordermanagement.api.domain.service

import com.ordermanagement.api.domain.entity.Address
import com.ordermanagement.api.domain.entity.Customer
import com.ordermanagement.api.domain.entity.Order
import com.ordermanagement.api.domain.entity.OrderLineItem
import org.springframework.stereotype.Service

@Service
class OrderProcessingService {

    fun validateOrder(order: Order): OrderValidationResult {
        if (!order.isPaid()) {
            return OrderValidationResult.Error(ORDER_NOT_PAID_MESSAGE)
        }

        val validationResult = validateOrderData(order)
        if (!validationResult.isValid) {
            val errorMessage = extractValidationErrorMessage(validationResult)
            return OrderValidationResult.Error(errorMessage)
        }

        return OrderValidationResult.Valid(order)
    }

    private fun validateOrderData(order: Order): OrderDataValidationResult {
        if (isCustomerInformationIncomplete(order.customer)) {
            return OrderDataValidationResult.Invalid(CUSTOMER_INFO_INCOMPLETE_MESSAGE)
        }

        if (order.lineItems.isEmpty()) {
            return OrderDataValidationResult.Invalid(ORDER_MUST_HAVE_LINE_ITEMS_MESSAGE)
        }

        val lineItemValidationResult = validateLineItems(order.lineItems)
        if (!lineItemValidationResult.isValid) {
            return lineItemValidationResult
        }

        if (isShippingAddressIncomplete(order.shippingAddress)) {
            return OrderDataValidationResult.Invalid(SHIPPING_ADDRESS_INCOMPLETE_MESSAGE)
        }

        if (isBillingAddressIncomplete(order.billingAddress)) {
            return OrderDataValidationResult.Invalid(BILLING_ADDRESS_INCOMPLETE_MESSAGE)
        }

        return OrderDataValidationResult.Valid
    }

    private fun isCustomerInformationIncomplete(customer: Customer): Boolean {
        return customer.email.isBlank() || customer.firstName.isBlank()
    }

    private fun validateLineItems(lineItems: List<OrderLineItem>): OrderDataValidationResult {
        for (lineItem in lineItems) {
            if (lineItem.quantity <= 0) {
                return OrderDataValidationResult.Invalid(
                        LINE_ITEM_QUANTITY_MUST_BE_POSITIVE_MESSAGE
                )
            }
            if (lineItem.sku.isBlank()) {
                return OrderDataValidationResult.Invalid(LINE_ITEM_SKU_REQUIRED_MESSAGE)
            }
        }
        return OrderDataValidationResult.Valid
    }

    private fun isShippingAddressIncomplete(address: Address): Boolean {
        return address.address1.isBlank() || address.city.isBlank()
    }

    private fun isBillingAddressIncomplete(address: Address): Boolean {
        return address.address1.isBlank() || address.city.isBlank()
    }

    private fun extractValidationErrorMessage(validationResult: OrderDataValidationResult): String {
        return when (validationResult) {
            is OrderDataValidationResult.Invalid -> validationResult.message
            else -> ORDER_DATA_VALIDATION_FAILED_MESSAGE
        }
    }

    companion object {
        private const val ORDER_NOT_PAID_MESSAGE = "Order cannot be processed"
        private const val CUSTOMER_INFO_INCOMPLETE_MESSAGE = "Customer information is incomplete"
        private const val ORDER_MUST_HAVE_LINE_ITEMS_MESSAGE =
                "Order must have at least one line item"
        private const val LINE_ITEM_QUANTITY_MUST_BE_POSITIVE_MESSAGE =
                "Line item quantity must be positive"
        private const val LINE_ITEM_SKU_REQUIRED_MESSAGE = "Line item SKU is required"
        private const val SHIPPING_ADDRESS_INCOMPLETE_MESSAGE = "Shipping address is incomplete"
        private const val BILLING_ADDRESS_INCOMPLETE_MESSAGE = "Billing address is incomplete"
        private const val ORDER_DATA_VALIDATION_FAILED_MESSAGE = "Order data validation failed"
    }
}

sealed class OrderValidationResult {
    data class Valid(val order: Order) : OrderValidationResult()
    data class Error(val message: String) : OrderValidationResult()
}

sealed class OrderDataValidationResult {
    object Valid : OrderDataValidationResult()
    data class Invalid(val message: String) : OrderDataValidationResult()

    val isValid: Boolean
        get() = this is Valid
}
