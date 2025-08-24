package com.ordermanagement.streamprocess.infrastructure.messaging

import com.ordermanagement.streamprocess.application.ProcessInventoryUpdatePort
import com.ordermanagement.streamprocess.application.ProcessResult
import com.ordermanagement.streamprocess.domain.entity.InventoryUpdate
import com.ordermanagement.streamprocess.domain.valueobject.LocationId
import com.ordermanagement.streamprocess.domain.valueobject.ProductId
import com.ordermanagement.streamprocess.domain.valueobject.Quantity
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KafkaConsumer(private val processInventoryUpdatePort: ProcessInventoryUpdatePort) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
            topics = ["\${app.kafka.topics.inventory-updates}"],
            groupId = "\${spring.kafka.consumer.group-id}"
    )
    suspend fun consumeInventoryUpdate(message: InventoryStreamMessage) {
        logger.info("Received inventory update message: {}", message)

        try {
            val inventoryUpdate = createInventoryUpdateFromMessage(message)
            val result = processInventoryUpdate(inventoryUpdate)
            logProcessingResult(result)
        } catch (e: Exception) {
            logProcessingError(e)
        }
    }

    private fun createInventoryUpdateFromMessage(message: InventoryStreamMessage): InventoryUpdate {
        return InventoryUpdate(
                productId = ProductId(message.productId),
                quantity = Quantity(message.quantity),
                locationId = LocationId(message.locationId)
        )
    }

    private suspend fun processInventoryUpdate(inventoryUpdate: InventoryUpdate): ProcessResult {
        return processInventoryUpdatePort.processInventoryUpdate(inventoryUpdate)
    }

    private fun logProcessingResult(result: ProcessResult) {
        if (result.isSuccess) {
            logger.info(
                    "Successfully processed inventory update for product {}: {}",
                    result.productId,
                    result.message
            )
        } else {
            logger.error(
                    "Failed to process inventory update for product {}: {}",
                    result.productId,
                    result.message
            )
        }
    }

    private fun logProcessingError(exception: Exception) {
        logger.error("Error processing inventory update message: {}", exception.message, exception)
    }
}

data class InventoryStreamMessage(val productId: Long, val quantity: Int, val locationId: Long)
