# Order Management System (OMS) - E-commerce Platform Integration

## 🏗️ High-Level Architecture (See Architecture.jpg)

The Order Management System is designed as a microservices architecture that integrates with Shopify e-commerce platforms and a centralized Warehouse Management System (WMS) to provide real-time inventory synchronization and reliable order processing.

```
┌─────────────────┐    ┌─────────────────────────────────────────────────────────────┐    ┌─────────────────┐
│                 │    │                    OMS (Order Management System)           │    │                 │
│                 │    │                                                             │    │                 │
│   Shopify       │    │  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────┐ │    │      WMS       │
│   (E-commerce)  │◄───┤ │ order-manage-api│    │order-manage-data│    │order-stream │ │    │ (Warehouse)    │
│                 │    │ │                 │    │     -api        │    │  -process   │ │    │                 │
│                 │    │ │ • Webhook       │    │ • Data Access   │    │ • Kafka     │ │    │                 │
│                 │    │ │ • Order         │    │ • Persistence   │    │   Consumer  │ │    │                 │
│                 │    │ │   Processing    │    │ • Transactions  │    │ • Inventory │ │    │                 │
│                 │    │ │ • WMS           │    │ • Inventory     │    │   Updates   │ │    │                 │
│                 │    │ │   Integration   │    │   Management    │    │ • Shopify   │ │    │                 │
│                 │    │ │ • Kafka         │    │                 │    │   Sync      │ │    │                 │
│                 │    │ │   Publishing    │    │                 │    │             │ │    │                 │
│                 │    │ └─────────────────┘    └─────────────────┘    └─────────────┘ │    │                 │
│                 │    │                                                             │    │                 │
│                 │    │  ┌─────────────────┐    ┌─────────────────┐                 │    │                 │
│                 │    │  │   PostgreSQL    │    │     Kafka      │                 │    │                 │
│                 │    │  │   Database      │    │   Message      │                 │    │                 │
│                 │    │  │                 │    │   Queue        │                 │    │                 │
│                 │    │  │ • Orders        │    │ • Inventory    │                 │    │                 │
│                 │    │  │ • Order Details │    │   Updates      │                 │    │                 │
│                 │    │  │ • Inventory     │    │ • Async        │                 │    │                 │
│                 │    │  │                 │    │   Processing   │                 │    │                 │
│                 │    │  └─────────────────┘    └─────────────────┘                 │    │                 │
│                 │    └─────────────────────────────────────────────────────────────┘    │                 │
└─────────────────┘                                                                      └─────────────────┘
```

## 🔄 Order Processing Flow

```
1. Customer places order on Shopify
   ↓
2. Shopify sends webhook to order-manage-api
   ↓
3. order-manage-api validates order and checks inventory via order-manage-data-api
   ↓
4. If inventory available:
   - Update database (order-manage-data-api)
   - Publish inventory update to Kafka
   - Send fulfillment request to WMS
   ↓
5. order-stream-process consumes Kafka message
   ↓
6. Update inventory on other Shopify stores
```

## 🏛️ Service Boundaries & Communication

### 1. **order-manage-api** (Port 8080) - Business Logic Orchestrator
**Responsibilities:**
- Receive and process Shopify webhooks
- Orchestrate order validation and inventory checks
- Communicate with WMS for fulfillment
- Publish inventory updates to Kafka
- Handle business logic and error scenarios

**Communication:**
- **Synchronous**: REST API calls to order-manage-data-api and WMS
- **Asynchronous**: Kafka message publishing for inventory updates

### 2. **order-manage-data-api** (Port 8081) - Data Access Layer
**Responsibilities:**
- Database operations (CRUD for orders, inventory)
- Transaction management with pessimistic locking
- Inventory availability checks
- Order status management

**Communication:**
- **Synchronous**: REST API endpoints for order operations
- **Database**: Direct PostgreSQL connections with JPA

### 3. **order-stream-process** (Port 8083) - Stream Processing Service
**Responsibilities:**
- Consume inventory update messages from Kafka
- Update inventory levels on Shopify stores
- Handle asynchronous inventory synchronization

**Communication:**
- **Asynchronous**: Kafka message consumption
- **Synchronous**: REST API calls to Shopify Admin API

### Communication Protocol Choices

| Scenario | Protocol | Justification |
|----------|----------|---------------|
| **Shopify Webhooks** | REST (HTTP) | Standard webhook protocol, easy integration |
| **Internal Service Calls** | REST (HTTP) | Simple, stateless, easy debugging |
| **WMS Integration** | REST (HTTP) | Industry standard, vendor compatibility |
| **Inventory Updates** | Kafka (Message Queue) | Asynchronous, decoupled, reliable |
| **Database Access** | JDBC/JPA | Direct data access, ACID compliance |

## 🎯 API Design and Data Mapping

### Shopify Webhook DTO Design

**Incoming Webhook Fields (Selected):**
```kotlin
data class ShopifyOrderDto(
    val id: String,                    // ✅ Order identifier
    val orderNumber: Int,              // ✅ Order number for tracking
    val customer: CustomerDto,          // ❌ Out of scope (not used)
    val lineItems: List<LineItemDto>,  // ✅ Product details
    val shippingAddress: AddressDto,   // ❌ Out of scope (not used)
    val billingAddress: AddressDto,    // ❌ Out of scope (not used)
    val totalPrice: String,            // ✅ Total order value
    val currency: String,              // ✅ Currency for pricing
    val createdAt: String,             // ✅ Order timestamp
    val financialStatus: String,       // ✅ Payment status
    val fulfillmentStatus: String?     // ✅ Fulfillment tracking
)
```

**Omitted Fields & Reasons:**
- `customer`, `shippingAddress`, `billingAddress`: Out of scope for inventory management
- `totalTax`, `subtotalPrice`: Not required for order processing
- `tags`, `note`, `sourceName`: Business metadata not needed

### WMS Fulfillment API Contract

**Prototype Requirement**: This WMS API integration is designed based on **ShipBob's API specifications** as the reference prototype. ShipBob provides comprehensive warehouse management solutions with real-time inventory tracking, automated fulfillment workflows, and multi-channel e-commerce integration.

**Request:**
```json
POST /api/v1/fulfillment-orders
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
```

**Response:**
```json
{
    "fulfillmentOrderId": 98765,
    "status": "pending",
    "referenceId": "ORDER-1001",
    "estimatedShipDate": "2024-01-16T10:00:00Z",
    "trackingNumber": null
}
```

**Reference**: [ShipBob Developer Portal](https://developer.shipbob.com/introduction)

## 🔒 Data Consistency & Race Conditions

### Overselling Prevention Strategy

**1. Pessimistic Locking in Database**
```kotlin
@Transactional
@Lock(LockModeType.PESSIMISTIC_WRITE)
fun updateInventory(productId: String, requestedQuantity: Int): Boolean {
    val inventory = inventoryRepository.findByProductId(productId)
    if (inventory.availableQuantity >= requestedQuantity) {
        inventory.availableQuantity -= requestedQuantity
        inventoryRepository.save(inventory)
        return true
    }
    return false
}
```

**2. Idempotent API Design**
- Check order ID before processing
- Reject duplicate orders with same ID
- Maintain order state consistency

**3. Transaction Management**
- Single transaction for order + inventory updates
- Rollback on any failure
- Atomic operations prevent partial updates

**Trade-offs:**
- **Pros**: Strong consistency, prevents overselling, simple to understand
- **Cons**: Potential performance impact under high concurrency, blocking behavior

## 🗄️ Data Modeling

### PostgreSQL Schema Design

```sql
-- Orders table (1:1 relationship with order details)
CREATE TABLE orders (
    order_id SERIAL PRIMARY KEY,
    order_number INTEGER UNIQUE NOT NULL,
    customer_id VARCHAR(20) NOT NULL,
    product_type_count INTEGER NOT NULL,
    total_price VARCHAR(20) NOT NULL,
    order_status VARCHAR(10) NOT NULL,
    create_date TIMESTAMP NOT NULL,
    create_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL,
    update_by VARCHAR(50) NOT NULL
);

-- Order details table (1:many relationship with orders)
CREATE TABLE order_detail (
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders(order_id),
    product_id VARCHAR(100) NOT NULL,
    price VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    create_date TIMESTAMP NOT NULL,
    create_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL,
    update_by VARCHAR(50) NOT NULL
);

-- Inventory table (product stock management)
CREATE TABLE inventory (
    product_id SERIAL PRIMARY KEY,
    sku VARCHAR(20) UNIQUE NOT NULL,
    product_title VARCHAR(10) NOT NULL,
    product_price VARCHAR(20) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    available_quantity INTEGER NOT NULL,
    create_date TIMESTAMP NOT NULL,
    create_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL,
    update_by VARCHAR(50) NOT NULL
);
```

**Key Design Choices:**
1. **Normalized Structure**: Separate tables for orders, details, and inventory
2. **Audit Fields**: Track creation and modification with timestamps and user info
3. **Referential Integrity**: Foreign key constraints maintain data consistency
4. **Indexing Strategy**: Primary keys and unique constraints for performance
5. **String Storage**: Prices as strings to avoid floating-point precision issues

## 🛡️ Error Handling & Resiliency

### Retry Strategy Implementation

**Error Handling Options:**

This project offers two approaches to error handling. We currently use **Option 1 (WMS API Failures)** for its simplicity and ease of maintenance, while **Option 2 (Graceful Degradation)** provides enterprise-grade resilience for production environments.

**Option 1: WMS API Failures (Currently Implemented)**
```kotlin
@Retryable(
    value = [WmsApiException::class],
    maxAttempts = 3,
    backoff = BackOff(delay = 1000, multiplier = 2.0)
)
suspend fun createFulfillmentOrder(order: Order): WmsFulfillmentResult
```

**Option 2: Graceful Degradation (Alternative)**
- Cache WMS health status (TTL: 1 minute)
- Return error responses instead of hanging
- Maintain system stability under WMS failures

**3. Circuit Breaker Pattern**
```kotlin
@CircuitBreaker(
    name = "wms-api",
    fallbackMethod = "createFulfillmentOrderFallback"
)
suspend fun createFulfillmentOrder(order: Order): WmsFulfillmentResult
```

**4. Dead Letter Queue for Kafka**
- Failed inventory updates sent to error topic
- Retry service processes failed messages
- Exponential backoff for retry attempts

---

**Quick Comparison:**

| Option | Pros | Cons |
|--------|------|------|
| **WMS API Failures** | Simple, easy to debug, low overhead | Limited resilience, no fallback |
| **Graceful Degradation** | Better stability, health monitoring | More complex, requires caching |
| **Circuit Breaker** | Prevents cascading failures, fast failure detection | Complex state management, debugging challenges |
| **Dead Letter Queue** | Guaranteed message processing, automated retries | Additional infrastructure, message ordering issues |



## 🚀 Quick Start

### Prerequisites
- Docker and Docker Compose
- Java 17+
- Gradle

### 🧪 Comprehensive Testing Guide

The Order Management System includes a comprehensive testing framework that validates all aspects of the system. Use the provided Makefile commands to run tests efficiently.

#### **1. Build and Start All Services**

**🎯 ONE COMMAND SOLUTION (Recommended):**
```bash
# Build, start infrastructure, initialize DB, start services, and run comprehensive tests
make test-everything-simple
```

This single command will:
1. **Build** all three microservices (`order-manage-api`, `order-manage-data-api`, `order-stream-process`)
2. **Start** infrastructure (PostgreSQL, Zookeeper, Kafka, Mock WMS API)
3. **Initialize** database with schema and test data
4. **Start** all application services
5. **Run** comprehensive tests to verify everything works

**Alternative: Step-by-step Commands**
If you prefer to run steps individually:

```bash
# Step 1: Build all components
make build-all

# Step 2: Start infrastructure and initialize database
make start-order-services

# Step 3: Start all application services
make init-db

# Step 4: Run comprehensive tests
make test-all-scenarios
```

**Or even more granular control:**
```bash
# Build individual services
cd order-manage-data-api && ./gradlew clean build -x test
cd order-manage-api && ./gradlew clean build -x test  
cd order-stream-process && ./gradlew clean build -x test

# Start infrastructure only
make start-infra

# Initialize database
make init-db

# Start services one by one
docker-compose up -d order-manage-data-api
docker-compose up -d order-manage-api
docker-compose up -d order-stream-process

# Check service status
make status

# Run tests
make test-all-scenarios
```

**Manual Troubleshooting Commands:**
```bash
# Check if containers are running
docker-compose ps

# Check individual service logs
docker-compose logs order-manage-data-api
docker-compose logs order-manage-api
docker-compose logs order-stream-process

# Check database directly
docker exec order-management-postgres psql -U postgres -d order_management -c "SELECT COUNT(*) FROM orders;"

# Restart a specific service
docker-compose restart order-manage-data-api

# Stop everything and start fresh
make stop-all
docker system prune -f
make test-everything-simple
```

**What Gets Started:**
- **Infrastructure**: PostgreSQL (5432), Zookeeper (2181), Kafka (9092), Mock WMS API (8084)
- **Applications**: order-manage-api (8080), order-manage-data-api (8081), order-stream-process (8083)
- **Database**: Tables, sample data, and test product catalog

#### **2. Initialize Database and Mock Data**
The system automatically initializes:
- Database schema creation
- Sample inventory data
- Mock WMS API endpoints
- Test product catalog

#### **3. Start Order Services**

**Option 1: Start Everything (Including Zookeeper/Kafka)**
```bash
# Start all services with full infrastructure
make start-all
```

**Option 2: Start Order Services Only**
```bash
# Start just the order services (no infrastructure wait)
make start-order-services
```

**Option 3: Start Individual Services**
```bash
# Start order-manage-data-api (Database operations & inventory management)
docker-compose up -d order-manage-data-api

# Start order-manage-api (Shopify webhooks & WMS integration)  
docker-compose up -d order-manage-api

# Start order-stream-process (Kafka consumption & Shopify sync)
docker-compose up -d order-stream-process
```

## 🎉 **System Status: FULLY OPERATIONAL**

**✅ All Services Running Successfully:**
- PostgreSQL Database: Healthy
- Zookeeper: Healthy (Issue Resolved)
- Kafka: Healthy
- Mock WMS API: Healthy
- order-manage-data-api: Healthy
- order-manage-api: Healthy
- order-stream-process: Healthy

**✅ All Test Scenarios Passing:**
- 7 comprehensive test scenarios executed successfully
- Success and failure cases working properly
- Kafka integration functioning correctly
- No service downtime or errors

**Service Details:**
- **order-manage-data-api** (Port 8081): Database operations, inventory management, order CRUD
- **order-manage-api** (Port 8080): Shopify webhooks, WMS integration, Kafka publishing
- **order-stream-process** (Port 8083): Kafka consumption, Shopify inventory synchronization

**Verify Services Are Running:**
```bash
# Check service status
make status

# View service logs
make logs

# Check individual service health
curl http://localhost:8081/actuator/health  # order-manage-data-api
curl http://localhost:8080/actuator/health  # order-manage-api
curl http://localhost:8083/actuator/health  # order-stream-process
```

#### **4. Testing Commands (After Services Are Running)**

Once all services are started and running, use these commands to test the system:

##### **Run All Tests**
```bash
# Run complete end-to-end testing with comprehensive logging
make test-all-scenarios
```

##### **Individual Test Commands**
```bash
# Test basic Shopify to Order flow
make test-shopify-order

# Test complete Kafka consumption to Shopify flow
make test-kafka-shopify-flow
```

##### **Test Scenarios Included**

The system includes **7 comprehensive test scenarios** that validate both success and failure cases with detailed logging and database verification:

**✅ Zookeeper Issue Resolved:** The Zookeeper startup issue has been fixed. All test commands now work properly with the full infrastructure.

**Success Scenarios:**
- **3.1** Single Product Order (1 product, 1 quantity)
- **3.2** Multiple Products with Sufficient Inventory (3 products, 2 quantity each)
- **3.3** Large Product Set with Sufficient Inventory (5 products, 1 quantity each)

**Failure Scenarios:**
- **3.4** Duplicate Order ID Rejection
- **3.5** Non-existent Product Rejection
- **3.6** Mixed Valid/Invalid Products Rejection
- **3.7** Insufficient Inventory Rejection

##### **View Test Results and Logs**
```bash
# View success logs from all pods/services
make logs-success-cases

# View comprehensive logs from all services
make logs

# Check service status
make status

# Check recent logs from all services
make check-logs

# Check database data for recent orders
make check-db

# Check specific order details
make check-order ORDER_NUM=7001
```

**Log Verification Points:**
- **order-manage-api**: Order processing success, webhook handling
- **order-manage-data-api**: Inventory checks, database operations
- **order-stream-process**: Kafka message consumption, Shopify API calls
- **Mock WMS API**: External API integration success

**Comprehensive Testing Features:**
- **Real-time Logging**: Each test case shows logs from all services
- **Database Verification**: Automatic database queries after each test
- **Success/Failure Analysis**: Detailed analysis for both success and failure cases
- **Helper Commands**: Easy debugging with `check-logs`, `check-db`, and `check-order`

#### **6. Test Results Interpretation**

**Success Indicators:**
- ✅ All services start without errors
- ✅ Database initialization completes
- ✅ Test orders processed successfully
- ✅ Inventory updates reflected in database
- ✅ Kafka messages consumed and processed
- ✅ Shopify API integrations working

**Common Issues & Solutions:**
- **Service startup failures**: Check Docker resources and port availability
- **Database connection issues**: Verify PostgreSQL container status
- **Kafka connectivity problems**: Ensure Zookeeper and Kafka are running
- **Test failures**: Check service logs for specific error details



## 📚 API Documentation

### 🚀 Swagger/OpenAPI Interactive Documentation

The Order Management System provides comprehensive, interactive API documentation using **Swagger/OpenAPI 3.0.3** standards. Each microservice includes detailed API specifications with real-time testing capabilities.

#### **Available Swagger Documentation**

| Service | Port | Swagger UI URL | Description |
|---------|------|----------------|-------------|
| **order-manage-api** | 8080 | [`http://localhost:8080/swagger-ui.html`](http://localhost:8080/swagger-ui.html) | Shopify webhooks, WMS integration, Kafka publishing |
| **order-manage-data-api** | 8081 | [`http://localhost:8081/swagger-ui.html`](http://localhost:8081/swagger-ui.html) | Database operations, inventory management, order CRUD |
| **order-stream-process** | 8083 | [`http://localhost:8083/swagger-ui.html`](http://localhost:8083/swagger-ui.html) | Kafka consumption, Shopify inventory sync |


## 🔧 Configuration
```yaml
# Database
SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/order_management
SPRING_DATASOURCE_USERNAME: postgres
SPRING_DATASOURCE_PASSWORD: postgres123

# Kafka
APP_KAFKA_BOOTSTRAP_SERVERS: localhost:9092
APP_KAFKA_TOPICS_INVENTORY_UPDATES: inventory-updates

# External APIs
APP_WMS_API_BASE_URL: http://localhost:8084
APP_SHOPIFY_BASE_URL: https://your-shop.myshopify.com
```

