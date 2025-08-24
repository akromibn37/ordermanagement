# Order Stream Process Service

A microservice that processes inventory update messages from Kafka and synchronizes them with Shopify's inventory system.

## Architecture

This service follows Clean Architecture and Domain-Driven Design (DDD) principles:

### Domain Layer
- **Entities**: `InventoryUpdate` - represents inventory update data
- **Value Objects**: `ProductId`, `Quantity`, `LocationId` - immutable identifiers and values
- **Domain Services**: `InventoryProcessingService` - business logic for inventory processing

### Application Layer
- **Use Cases**: `ProcessInventoryUpdateUseCase` - orchestrates the inventory update process
- **Ports**: 
  - `ProcessInventoryUpdatePort` (in) - interface for processing inventory updates
  - `ShopifyApiPort` (out) - interface for Shopify API communication

### Infrastructure Layer
- **Kafka Consumer**: `KafkaConsumer` - consumes messages from `inventory-updates` topic
- **Shopify API Adapter**: `ShopifyApiAdapter` - communicates with Shopify Admin API
- **Configuration**: `WebClientConfig`, `KafkaConfig` - Spring configuration

## Features

1. **Kafka Message Consumption**
   - Listens to `inventory-updates` topic
   - Processes messages in format:
     ```json
     {
       "productId": 123456789,
       "quantity": 2,
       "locationId": 123456789
     }
     ```

2. **Shopify Inventory Synchronization**
   - Calls Shopify Admin API: `POST /admin/api/2023-10/inventory_levels/set.json`
   - Request format:
     ```json
     {
       "location_id": 123456789,
       "inventory_item_id": 987654321,
       "available": 95
     }
     ```
   - Response format:
     ```json
     {
       "inventory_level": {
         "inventory_item_id": 987654321,
         "location_id": 123456789,
         "available": 95,
         "updated_at": "2024-01-15T10:35:00Z"
       }
     }
     ```

## Configuration

### Environment Variables
- `SHOPIFY_ACCESS_TOKEN`: Shopify Admin API access token

### Application Properties
- Server port: 8083
- Kafka bootstrap servers: localhost:9092
- Kafka topic: inventory-updates
- Shopify base URL: https://your-shop.myshopify.com
- Shopify API version: 2023-10

## Running the Service

1. Ensure Kafka is running on localhost:9092
2. Set `SHOPIFY_ACCESS_TOKEN` environment variable
3. Run the Spring Boot application
4. The service will start consuming messages from the `inventory-updates` topic

## Message Flow

1. **Kafka Consumer** receives inventory update message
2. **Domain Service** validates the message data
3. **Use Case** orchestrates the process
4. **Shopify API Adapter** calls Shopify inventory API
5. **Result** is logged and processed

## Error Handling

- Invalid message format is logged and skipped
- Shopify API failures are logged with details
- Network errors are handled gracefully
- All errors include product ID for traceability 