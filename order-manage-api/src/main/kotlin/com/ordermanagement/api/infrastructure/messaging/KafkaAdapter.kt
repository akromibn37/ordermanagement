package com.ordermanagement.api.infrastructure.messaging

import com.ordermanagement.api.application.port.out.KafkaPort
import com.ordermanagement.api.application.port.out.KafkaPublishResult
import com.ordermanagement.api.domain.entity.Order
import com.ordermanagement.api.domain.entity.OrderLineItem
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaAdapter(private val kafkaTemplate: KafkaTemplate<String, InventoryStreamMessage>) :
        KafkaPort {

        @Value("\${app.kafka.topics.inventory-queue}") private lateinit var topic: String

        override suspend fun publishInventoryUpdate(order: Order): KafkaPublishResult {
                return try {
                        publishInventoryMessagesForOrder(order)
                        createSuccessResult()
                } catch (e: Exception) {
                        createFailureResult(e.message)
                }
        }

        private fun publishInventoryMessagesForOrder(order: Order) {
                order.lineItems.forEach { lineItem ->
                        val message = createInventoryStreamMessage(lineItem)
                        val messageKey = lineItem.productId.toString()
                        kafkaTemplate.send(topic, messageKey, message)
                }
        }

        private fun createInventoryStreamMessage(lineItem: OrderLineItem): InventoryStreamMessage {
                return InventoryStreamMessage(
                        productId = lineItem.productId,
                        quantity = lineItem.quantity,
                        locationId = DEFAULT_LOCATION_ID
                )
        }

        private fun createSuccessResult(): KafkaPublishResult {
                return KafkaPublishResult(
                        isSuccess = true,
                        message = INVENTORY_MESSAGES_PUBLISHED_SUCCESS_MESSAGE
                )
        }

        private fun createFailureResult(errorMessage: String?): KafkaPublishResult {
                return KafkaPublishResult(
                        isSuccess = false,
                        message = "Failed to publish inventory stream messages: $errorMessage"
                )
        }

        companion object {
                private const val DEFAULT_LOCATION_ID = 123456789L
                private const val INVENTORY_MESSAGES_PUBLISHED_SUCCESS_MESSAGE =
                        "Inventory stream messages published successfully"
        }
}

data class InventoryStreamMessage(val productId: Long, val quantity: Int, val locationId: Long)
