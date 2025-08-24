package com.ordermanagement.streamprocess.infrastructure.messaging

import com.ordermanagement.streamprocess.application.port.in.ProcessInventoryUpdatePort
import com.ordermanagement.streamprocess.domain.entity.InventoryUpdate
import com.ordermanagement.streamprocess.domain.valueobject.LocationId
import com.ordermanagement.streamprocess.domain.valueobject.ProductId
import com.ordermanagement.streamprocess.domain.valueobject.Quantity
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KafkaConsumer(
    private val processInventoryUpdatePort: ProcessInventoryUpdatePort
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${app.kafka.topics.inventory-updates}"],
        groupId = "\${spring.kafka.consumer.group-id}"
    )
    suspend fun consumeInventoryUpdate(message: InventoryStreamMessage) {
        logger.info("Received inventory update message: {}", message)
        
        try {
            val inventoryUpdate = InventoryUpdate(
                productId = ProductId(message.productId),
                quantity = Quantity(message.quantity),
                locationId = LocationId(message.locationId)
            )
            
            val result = processInventoryUpdatePort.processInventoryUpdate(inventoryUpdate)
            
            if (result.isSuccess) {
                logger.info("Successfully processed inventory update for product {}: {}", 
                    result.productId, result.message)
            } else {
                logger.error("Failed to process inventory update for product {}: {}", 
                    result.productId, result.message)
            }
        } catch (e: Exception) {
            logger.error("Error processing inventory update message: {}", e.message, e)
        }
    }
}

data class InventoryStreamMessage(
    val productId: Long,
    val quantity: Int,
    val locationId: Long
) 