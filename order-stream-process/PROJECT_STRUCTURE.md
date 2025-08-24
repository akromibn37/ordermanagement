# Order Stream Process - Project Structure

## Overview
This service follows Clean Architecture and DDD principles to process Kafka messages and synchronize inventory with Shopify.

## Directory Structure

```
order-stream-process/
├── build.gradle.kts                    # Gradle build configuration
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/ordermanagement/streamprocess/
│   │   │       ├── OrderStreamProcessApplication.kt    # Main Spring Boot app
│   │   │       ├── domain/                             # Domain Layer
│   │   │       │   ├── entity/
│   │   │       │   │   └── InventoryUpdate.kt         # Core domain entity
│   │   │       │   ├── valueobject/                    # Value Objects
│   │   │       │   │   ├── ProductId.kt
│   │   │       │   │   ├── Quantity.kt
│   │   │       │   │   └── LocationId.kt
│   │   │       │   └── service/
│   │   │       │       └── InventoryProcessingService.kt
│   │   │       ├── application/                        # Application Layer
│   │   │       │   ├── port/
│   │   │       │   │   ├── in/
│   │   │       │   │   │   └── ProcessInventoryUpdatePort.kt
│   │   │       │   │   └── out/
│   │   │       │   │       └── ShopifyApiPort.kt
│   │   │       │   └── usecase/
│   │   │       │       └── ProcessInventoryUpdateUseCase.kt
│   │   │       └── infrastructure/                     # Infrastructure Layer
│   │   │           ├── messaging/
│   │   │           │   └── KafkaConsumer.kt            # Kafka message consumer
│   │   │           ├── external/
│   │   │           │   └── ShopifyApiAdapter.kt        # Shopify API client
│   │   │           └── config/
│   │   │               ├── WebClientConfig.kt          # WebClient configuration
│   │   │               └── KafkaConfig.kt              # Kafka configuration
│   │   └── resources/
│   │       └── application.yml                         # Application configuration
│   └── test/
│       └── kotlin/
│           └── com/ordermanagement/streamprocess/
│               └── domain/
│                   └── service/
│                       └── InventoryProcessingServiceTest.kt
├── README.md                                           # Service documentation
└── PROJECT_STRUCTURE.md                                # This file
```

## Key Components

### 1. Domain Layer
- **InventoryUpdate**: Core entity representing inventory update data
- **Value Objects**: Immutable identifiers (ProductId, Quantity, LocationId)
- **InventoryProcessingService**: Business logic for validation and transformation

### 2. Application Layer
- **ProcessInventoryUpdateUseCase**: Main use case orchestrating the process
- **Ports**: Interfaces defining input/output contracts

### 3. Infrastructure Layer
- **KafkaConsumer**: Consumes messages from `inventory-updates` topic
- **ShopifyApiAdapter**: HTTP client for Shopify Admin API
- **Configuration**: Spring beans for WebClient and Kafka

## Data Flow

```
Kafka Message → KafkaConsumer → ProcessInventoryUpdateUseCase → ShopifyApiAdapter → Shopify API
     ↓              ↓                    ↓                        ↓              ↓
InventoryUpdate  Validation        Business Logic         HTTP Request    Response
```

## Message Formats

### Input (Kafka)
```json
{
  "productId": 123456789,
  "quantity": 2,
  "locationId": 123456789
}
```

### Output (Shopify API)
```json
{
  "location_id": 123456789,
  "inventory_item_id": 987654321,
  "available": 95
}
```

## Dependencies
- Spring Boot 3.2.0
- Spring Kafka
- Spring WebFlux
- Jackson for JSON processing
- Kotlin 1.9.20
- JUnit 5 for testing 