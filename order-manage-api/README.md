# Order Management API - Clean Architecture with DDD (Stateless)

## Overview

This is a comprehensive implementation of an Order Management System (OMS) that handles Shopify webhook notifications and processes orders through a clean architecture with Domain-Driven Design (DDD) principles. The system is designed as a **stateless service** that coordinates with external services for data persistence and inventory management.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Web Layer                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              ShopifyWebhookController                   │   │
│  │              (REST API Endpoints)                       │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Application Layer                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              ProcessShopifyOrderUseCase                 │   │
│  │              (Application Services)                     │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Domain Layer                             │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Order, Customer (Entities)                             │   │
│  │  OrderProcessingService (Domain Services)              │   │
│  │  Business Logic & Validation                           │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Infrastructure Layer                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  External API Adapters, Kafka Messaging,               │   │
│  │  Configuration & HTTP Clients                           │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Service Boundaries & Communication

### Microservices Decomposition

1. **Order Management API** (this service - **Stateless**)
   - Handles Shopify webhooks
   - Validates order data
   - Coordinates with external services
   - **No data persistence** - delegates to external services

2. **Order Data API** (external)
   - Manages order data persistence
   - Provides order validation logic
   - Handles order updates

3. **WMS** (external)
   - Manages warehouse inventory
   - Handles fulfillment requests
   - Provides real-time stock information

### Communication Protocols

- **Synchronous**: REST APIs for immediate responses (order validation, inventory checks)
- **Asynchronous**: Kafka messaging for inventory updates across channels
- **Protocols**: 
  - REST for external API communication
  - Kafka for event-driven messaging
  - **No database operations** - pure external service coordination

## API Design and Data Mapping

### Shopify Webhook DTO

The incoming webhook includes essential fields:
- **Core Order Info**: ID, order number, timestamps
- **Customer Details**: Contact information, shipping/billing addresses
- **Line Items**: Product details, quantities, pricing
- **Financial Status**: Payment status, totals, currency

**Fields Omitted**: 
- Complex Shopify-specific metadata
- Historical data not needed for processing
- UI-specific formatting

### WMS API Contracts

#### 1. Inventory Check API
```
GET /api/v1/products/{productId}/inventory
Authorization: Bearer {api-key}

Response:
{
    "productId": 12345,
    "sku": "TSHIRT-001",
    "availableQuantity": 95,
    "reservedQuantity": 5,
    "totalQuantity": 100,
    "locationId": 67890,
    "lastUpdated": "2024-01-15T10:30:00Z"
}
```

#### 2. Fulfillment Order Creation API
```
POST /api/v1/fulfillment-orders
Authorization: Bearer {api-key}

Request Body:
{
    "referenceId": "ORDER-1001",
    "items": [
        {
            "productId": 12345,
            "quantity": 2,
            "sku": "TSHIRT-001"
        }
    ],
    "shippingAddress": {
        "firstName": "John",
        "lastName": "Doe",
        "address1": "123 Main St",
        "city": "New York",
        "state": "NY",
        "zipCode": "10001",
        "country": "US"
    },
    "shippingMethod": "standard",
    "customerEmail": "customer@example.com"
}

Response:
{
    "fulfillmentOrderId": 98765,
    "status": "pending",
    "referenceId": "ORDER-1001",
    "estimatedShipDate": "2024-01-16T10:00:00Z",
    "trackingNumber": null
}
```

## Data Consistency & Race Conditions

### Overselling Prevention Strategy

1. **Real-time Inventory Check**: Verify stock availability with WMS before processing
2. **External Service Coordination**: Delegate data persistence to specialized services
3. **Event Publishing**: Asynchronous updates to other channels via Kafka
4. **Stateless Processing**: No local state to maintain consistency

### Trade-offs

- **Pros**: 
  - No database maintenance required
  - Horizontal scalability
  - Clear service boundaries
  - Real-time inventory from WMS
- **Cons**: 
  - Dependency on external service availability
  - Network latency for data operations
  - Requires robust external service design

## Architecture Benefits (Stateless Design)

### 1. **Horizontal Scalability**
- Multiple instances can run simultaneously
- No shared state to manage
- Load balancer friendly

### 2. **Simplified Deployment**
- No database setup required
- No data migration concerns
- Easy containerization

### 3. **Clear Service Boundaries**
- Each service has a single responsibility
- Easy to test and maintain
- Clear API contracts

### 4. **Resilience**
- No local data corruption risks
- External service failures are isolated
- Easy to restart without data loss

## Error Handling & Resiliency

### Retry Strategy

1. **Exponential Backoff**: For transient failures
2. **Circuit Breaker**: For external service failures
3. **Dead Letter Queue**: For failed Kafka messages
4. **Graceful Degradation**: Continue processing when possible

### Recovery Mechanisms

- **WMS Unavailable**: Queue fulfillment requests for later processing
- **Order Data API Failures**: Retry with exponential backoff
- **Kafka Failures**: Store events locally and retry
- **Service Unavailable**: Return appropriate error responses

## Implementation Details

### Domain Layer

- **Entities**: Order, Customer with rich business logic
- **Value Objects**: Money, Address, OrderId for type safety
- **Domain Services**: OrderProcessingService for validation logic
- **No Repositories**: Business logic only, no data access

### Application Layer

- **Use Cases**: ProcessShopifyOrderUseCase orchestrating the workflow
- **Ports**: Input/Output interfaces for dependency inversion
- **DTOs**: Data transfer objects for external communication

### Infrastructure Layer

- **External APIs**: Adapters for WMS and Order Data API
- **Messaging**: Kafka producer for inventory events
- **Configuration**: WebClient setup for HTTP communication
- **No Persistence**: Pure external service coordination

## Testing Strategy

### Unit Tests
- Domain entities and business logic
- Application use cases
- Validation logic

### Integration Tests
- API endpoints
- External service communication
- Kafka message publishing

### End-to-End Tests
- Complete order processing workflow
- Error scenarios and recovery
- External service integration

## Running the Application

### Prerequisites
- Java 17+
- Kafka
- External services (WMS, Order Data API)

### Configuration
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092

app:
  wms:
    base-url: http://localhost:8081
    api-key: wms-api-key
  
  order-data-api:
    base-url: http://localhost:8082
  
  kafka:
    topics:
      inventory-queue: inventory-updates

server:
  port: 8080
```

### API Endpoints
```
POST /api/shopify/webhooks/orders
Content-Type: application/json

{
  "id": 123456789,
  "order_number": "1001",
  "customer": { ... },
  "line_items": [ ... ],
  "shipping_address": { ... },
  "billing_address": { ... },
  "total_price": "39.98",
  "currency": "USD",
  "financial_status": "paid"
}
```

## Business Flow

1. **Receive Shopify Webhook** → Validate incoming data
2. **Convert DTO to Domain Entity** → Business object creation
3. **Validate Order** → Business rule validation (no database)
4. **Check Order Data API** → Verify if processing is allowed
5. **Check WMS Inventory** → Real-time stock verification
6. **Update Order Data API** → Persist order information
7. **Publish to Kafka** → Notify other channels
8. **Create Fulfillment** → Send to WMS for processing

## Future Enhancements

1. **Event Sourcing**: Complete audit trail of all order changes
2. **CQRS**: Separate read/write models for better performance
3. **Saga Pattern**: Distributed transaction management
4. **API Gateway**: Centralized routing and authentication
5. **Monitoring**: Metrics, tracing, and alerting
6. **Rate Limiting**: Protection against webhook spam
7. **Webhook Verification**: Shopify signature validation
8. **Health Checks**: External service availability monitoring

## Conclusion

This implementation demonstrates a robust, scalable **stateless architecture** that follows clean architecture principles and DDD patterns. By removing database dependencies, the system becomes:

- **Easier to deploy** and scale
- **More focused** on business logic coordination
- **Highly available** with no local state concerns
- **Simpler to maintain** with clear service boundaries

The separation of concerns, dependency inversion, and external service coordination make the system maintainable and testable while providing the business functionality required for order management without the complexity of local data persistence. 