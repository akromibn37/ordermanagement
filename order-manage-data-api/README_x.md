# Order Management Data API - Clean Architecture with DDD

## Overview

This is the **Order Management Data API** service that handles data persistence and business logic for orders and inventory. It implements a clean architecture with Domain-Driven Design (DDD) principles and provides the database layer that the stateless `order-manage-api` service communicates with.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Web Layer                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              OrderController                            │   │
│  │              (REST API Endpoints)                       │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Application Layer                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              OrderUpdateUseCase                         │   │
│  │              (Application Services)                     │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Domain Layer                             │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Order, OrderDetail, Inventory (Entities)              │   │
│  │  OrderCheckService (Domain Services)                   │   │
│  │  Business Logic & Validation                           │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Infrastructure Layer                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  JPA Entities, Repositories, Mappers,                  │   │
│  │  Database Operations                                    │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Database                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  PostgreSQL with JPA/Hibernate                         │   │
│  │  Transaction Management                                 │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Database Schema

### Core Tables

```sql
-- Orders table
CREATE TABLE orders (
    order_id SERIAL PRIMARY KEY,
    order_number INT UNIQUE NOT NULL,
    customer_id VARCHAR(20) NOT NULL,
    product_type_count INT NOT NULL,
    total_price VARCHAR(20) NOT NULL,
    order_status VARCHAR(10) NOT NULL,
    create_date TIMESTAMP NOT NULL,
    create_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL,
    update_by VARCHAR(50) NOT NULL
);

-- Order detail table
CREATE TABLE order_detail (
    id SERIAL PRIMARY KEY,
    order_id INT NOT NULL REFERENCES orders(order_id),
    product_id VARCHAR(100) NOT NULL,
    price VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    create_date TIMESTAMP NOT NULL,
    create_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL,
    update_by VARCHAR(50) NOT NULL
);

-- Inventory table
CREATE TABLE inventory (
    product_id SERIAL PRIMARY KEY,
    sku VARCHAR(20) UNIQUE NOT NULL,
    product_title VARCHAR(10) NOT NULL,
    product_price VARCHAR(20) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    available_quantity INT NOT NULL,
    create_date TIMESTAMP NOT NULL,
    create_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL,
    update_by VARCHAR(50) NOT NULL
);
```

## API Endpoints

### 1. GET /api/order/check

**Purpose**: Check order status and inventory availability

**Query Parameters**:
- `orderId`: The order identifier (e.g., "10001")
- `productIds`: Comma-separated list of product IDs (e.g., "123456789,987654321,555666777")

**Logic**:
1. Check if order already exists and is completed
   - If order exists → response: "order already success"
2. Check inventory for all products
   - If all products availableQuantity > requestedQuantity → response: isContinue = true
   - Else → response: isContinue = false

**Response**:
```json
{
    "isContinue": true,
    "description": "success/not enough inventory/order already exist",
    "orderId": 1001,
    "products": [
        {
            "productId": 1000001,
            "sku": "TSHIRT-001",
            "title": "Popular T-Shirt",
            "requestedQuantity": 2,
            "availableQuantity": 95,
            "remainQuantity": 93,
            "status": "available"
        },
        {
            "productId": 100002,
            "sku": "TSHIRT-002",
            "title": "Popular T-Shirt Blue",
            "requestedQuantity": 1,
            "availableQuantity": 50,
            "remainQuantity": 49,
            "status": "available"
        }
    ]
}
```

### 2. POST /api/order/update

**Purpose**: Create or update orders with inventory allocation

**Request Body**:
```json
{
    "id": 123456789,
    "order_number": 1001,
    "customer_id": 123456789,
    "created_at": "2024-01-15T10:30:00Z",
    "updated_at": "2024-01-15T10:35:00Z",
    "processed_at": "2024-01-15T10:35:00Z",
    "line_items": [
        {
            "id": 123456789,
            "product_id": 123456789,
            "quantity": 2,
            "title": "Popular T-Shirt",
            "sku": "TSHIRT-001",
            "price": "19.99",
            "total_discount": "0.00"
        }
    ],
    "total_price": "39.98",
    "currency": "USD"
}
```

**Logic**:
1. **Open Transaction**
   - Check availableQuantity in table inventory
   - If availableQuantity > requestedQuantity:
     - Update availableQuantity = availableQuantity - requestedQuantity
   - Else: Rollback transaction
   - Commit transaction
2. **Insert to orders table**
3. **Insert to order_detail table**

**Response**:
```json
{
    "isSuccess": true,
    "message": "Order updated successfully"
}
```

## Business Logic

### Order Checking Service

The `OrderCheckService` provides:
- **Order existence validation**: Prevents duplicate order processing
- **Inventory availability check**: Ensures stock is available before processing
- **Real-time inventory status**: Returns current stock levels and availability

### Order Update Service

The `OrderUpdateUseCase` provides:
- **Transaction management**: Ensures data consistency
- **Inventory allocation**: Automatically reduces stock when orders are created
- **Order creation**: Persists order and line item data
- **Error handling**: Rollback on failures

### Domain Entities

#### Order
- Rich business logic for order management
- Status tracking and validation
- Relationship management with order details

#### Inventory
- Stock availability checking
- Safe stock allocation with validation
- Audit trail maintenance

#### OrderDetail
- Line item management
- Price and quantity tracking
- Audit information

## Data Consistency & Transaction Management

### ACID Properties
- **Atomicity**: All operations within a transaction succeed or fail together
- **Consistency**: Database remains in a valid state
- **Isolation**: Concurrent transactions don't interfere
- **Durability**: Committed changes are permanent

### Inventory Allocation Strategy
1. **Check Availability**: Verify stock before allocation
2. **Atomic Update**: Update inventory within transaction
3. **Rollback on Failure**: Revert changes if any step fails
4. **Audit Trail**: Track all changes with timestamps

## Error Handling & Resiliency

### Validation Errors
- Invalid product IDs
- Insufficient inventory
- Duplicate orders
- Invalid data formats

### Database Errors
- Connection failures
- Constraint violations
- Transaction timeouts

### Recovery Mechanisms
- Automatic rollback on failures
- Detailed error messages
- Audit logging for debugging

## Implementation Details

### Domain Layer
- **Entities**: Rich business objects with validation logic
- **Services**: Business rule enforcement and coordination
- **Repositories**: Data access contracts

### Application Layer
- **Use Cases**: Orchestration of business processes
- **DTOs**: Data transfer objects for API communication
- **Transaction Management**: Application-level transaction control

### Infrastructure Layer
- **JPA Entities**: Database mapping objects
- **Repositories**: Data access implementations
- **Mappers**: Domain-Entity conversion

### Web Layer
- **Controllers**: REST API endpoints
- **Request/Response Handling**: Input validation and output formatting
- **Error Handling**: HTTP status codes and error messages

## Testing Strategy

### Unit Tests
- Domain entities and business logic
- Application use cases
- Repository implementations

### Integration Tests
- API endpoints
- Database operations
- Transaction management

### End-to-End Tests
- Complete order processing workflow
- Error scenarios and recovery
- Database consistency validation

## Running the Application

### Prerequisites
- Java 17+
- PostgreSQL database
- Maven or Gradle

### Configuration
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/order_management
    username: postgres
    password: password
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

server:
  port: 8082
```

### Database Setup
```sql
-- Create database
CREATE DATABASE order_management;

-- Connect to database
\c order_management;

-- Tables will be created automatically by Hibernate
```

### Build and Run
```bash
# Build
./gradlew build

# Run
./gradlew bootRun
```

## Integration with Order Management API

This service is designed to work seamlessly with the stateless `order-manage-api`:

1. **order-manage-api** receives Shopify webhooks
2. **order-manage-api** calls this service to check orders and inventory
3. **order-manage-api** calls this service to create/update orders
4. **order-manage-api** publishes events to Kafka for inventory synchronization

## Future Enhancements

1. **Caching**: Redis for frequently accessed inventory data
2. **Event Sourcing**: Complete audit trail of all changes
3. **CQRS**: Separate read/write models for better performance
4. **Monitoring**: Metrics, tracing, and alerting
5. **Data Archival**: Historical data management
6. **Multi-tenancy**: Support for multiple merchants
7. **API Versioning**: Backward compatibility management

## Conclusion

This service provides a robust, scalable data layer for order management with:

- **Clean Architecture**: Clear separation of concerns
- **DDD Principles**: Rich domain models and business logic
- **Transaction Safety**: ACID compliance for data consistency
- **API Design**: RESTful endpoints following best practices
- **Error Handling**: Comprehensive error management and recovery
- **Scalability**: Designed for high-throughput order processing

The service acts as the **single source of truth** for order and inventory data, ensuring consistency across the entire order management ecosystem. 