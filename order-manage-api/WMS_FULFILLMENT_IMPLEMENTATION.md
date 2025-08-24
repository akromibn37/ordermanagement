# WMS Fulfillment Implementation - Order Management API

## Overview

This document outlines the complete implementation of WMS (Warehouse Management System) fulfillment integration in the order-manage-api service. The implementation follows the exact API specifications provided and integrates seamlessly with the clean architecture.

## API Endpoints

### 1. Inventory Check API

**Endpoint**: `GET /api/v1/products/{productId}/inventory`

**Purpose**: Check real-time inventory availability for a specific product

**Request**:
```
GET /api/v1/products/12345/inventory
Authorization: Bearer {api-key}
```

**Response**:
```json
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

### 2. Fulfillment Order Creation API

**Endpoint**: `POST /api/v1/fulfillment-orders`

**Purpose**: Create a new fulfillment order in the WMS

**Request**:
```json
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

**Response**:
```json
{
    "fulfillmentOrderId": 98765,
    "status": "pending",
    "referenceId": "ORDER-1001",
    "estimatedShipDate": "2024-01-16T10:00:00Z",
    "trackingNumber": null
}
```

## Implementation Details

### Architecture Components

#### 1. **WmsApiPort Interface**
```kotlin
interface WmsApiPort {
    suspend fun getProductInventory(productId: Long): WmsInventoryResponse
    suspend fun createFulfillmentOrder(order: Order): WmsFulfillmentResult
}
```

#### 2. **WmsApiAdapter Implementation**
```kotlin
@Component
class WmsApiAdapter(
    private val webClient: WebClient
) : WmsApiPort {
    // Implementation details...
}
```

#### 3. **Data Transfer Objects**

**WmsInventoryResponse**:
```kotlin
data class WmsInventoryResponse(
    val productId: Long,
    val availableQuantity: Int,
    val totalQuantity: Int,
    val reservedQuantity: Int = 0,
    val sku: String = "",
    val locationId: Long = 0,
    val lastUpdated: String = ""
)
```

**WmsFulfillmentRequest**:
```kotlin
data class WmsFulfillmentRequest(
    val referenceId: String,
    val items: List<WmsFulfillmentItem>,
    val shippingAddress: WmsShippingAddress,
    val shippingMethod: String,
    val customerEmail: String
)
```

**WmsFulfillmentResponse**:
```kotlin
data class WmsFulfillmentResponse(
    val fulfillmentOrderId: Long,
    val status: String,
    val referenceId: String,
    val estimatedShipDate: String,
    val trackingNumber: String?
)
```

### Data Mapping

#### **Order to WMS Fulfillment Mapping**

| Order Field | WMS Field | Notes |
|-------------|-----------|-------|
| `order.orderNumber` | `referenceId` | Order identifier |
| `order.lineItems` | `items` | Product line items |
| `order.shippingAddress.firstName` | `shippingAddress.firstName` | Customer first name |
| `order.shippingAddress.lastName` | `shippingAddress.lastName` | Customer last name |
| `order.shippingAddress.address1` | `shippingAddress.address1` | Street address |
| `order.shippingAddress.city` | `shippingAddress.city` | City |
| `order.shippingAddress.province` | `shippingAddress.state` | State/Province |
| `order.shippingAddress.zip` | `shippingAddress.zipCode` | ZIP/Postal code |
| `order.shippingAddress.country` | `shippingAddress.country` | Country |
| `order.customer.email` | `customerEmail` | Customer email |
| `"standard"` | `shippingMethod` | Default shipping method |

#### **WMS Response to Domain Mapping**

| WMS Field | Domain Field | Notes |
|-----------|--------------|-------|
| `fulfillmentOrderId` | `fulfillmentId` | WMS fulfillment identifier |
| `status` | `isSuccess` | Success determination |
| `estimatedShipDate` | N/A | For future use |
| `trackingNumber` | N/A | For future use |

## Business Logic Integration

### **Inventory Check Flow**

1. **Product ID Extraction**: Extract product IDs from order line items
2. **WMS API Call**: Call `/api/v1/products/{productId}/inventory` for each product
3. **Availability Validation**: Check if `availableQuantity >= requestedQuantity`
4. **Response Processing**: Handle success/failure scenarios

### **Fulfillment Creation Flow**

1. **Order Validation**: Ensure order is valid and inventory is available
2. **Request Preparation**: Map order data to WMS fulfillment request
3. **WMS API Call**: Call `/api/v1/fulfillment-orders`
4. **Response Handling**: Process WMS response and determine success
5. **Error Handling**: Handle failures gracefully

### **Success Criteria**

**Inventory Check Success**:
- All products have sufficient available quantity
- WMS API responds successfully
- No network or authentication errors

**Fulfillment Creation Success**:
- WMS API responds with status "pending" or "success"
- Fulfillment order ID is received
- No validation or business rule violations

## Error Handling

### **Network Errors**
- Connection timeouts
- Authentication failures
- Service unavailable

### **Business Logic Errors**
- Insufficient inventory
- Invalid product IDs
- Shipping address validation failures

### **Recovery Strategies**
- **Graceful Degradation**: Continue processing when possible
- **Default Values**: Return safe defaults for inventory checks
- **Error Logging**: Comprehensive error tracking for debugging
- **Retry Logic**: Future enhancement for transient failures

## Configuration

### **Environment Variables**
```yaml
app:
  wms:
    base-url: http://localhost:8081
    api-key: wms-api-key
```

### **HTTP Client Configuration**
- **Base URL**: Configurable WMS service endpoint
- **Authentication**: Bearer token authentication
- **Timeout**: Configurable request timeouts
- **Retry**: Future enhancement for resilience

## Testing

### **Unit Tests**
- **WmsApiAdapterTest**: Comprehensive adapter testing
- **Mock WebClient**: Isolated testing without external dependencies
- **Success Scenarios**: Valid API responses
- **Failure Scenarios**: Error handling and edge cases

### **Test Coverage**
- ✅ Inventory check success
- ✅ Inventory check failure
- ✅ Fulfillment creation success
- ✅ Fulfillment creation failure
- ✅ Data mapping validation
- ✅ Error handling scenarios

## Integration Points

### **Order Processing Use Case**
```kotlin
// Step 4: Check inventory in WMS for all products
val inventoryCheck = checkWmsInventory(order)
if (!inventoryCheck.isAvailable) {
    return ShopifyOrderResponseDto(
        status = "error",
        message = "Insufficient inventory: ${inventoryCheck.message}",
        orderId = shopifyOrderDto.id
    )
}

// ... later in the flow ...

// Step 7: Send fulfillment request to WMS
val fulfillmentResult = wmsApiPort.createFulfillmentOrder(order)
if (!fulfillmentResult.isSuccess) {
    println("Warning: Failed to create fulfillment order: ${fulfillmentResult.message}")
}
```

### **External Service Coordination**
1. **order-manage-api** → **WMS** (inventory check)
2. **order-manage-api** → **WMS** (fulfillment creation)
3. **WMS** → **order-manage-api** (response handling)

## Future Enhancements

### **1. Retry Logic**
- Exponential backoff for transient failures
- Circuit breaker pattern for service protection
- Dead letter queue for failed requests

### **2. Caching**
- Inventory cache for frequently accessed products
- Cache invalidation strategies
- Performance optimization

### **3. Monitoring**
- API response time metrics
- Success/failure rate tracking
- Alerting for service degradation

### **4. Enhanced Error Handling**
- Detailed error categorization
- Customer-friendly error messages
- Automated recovery mechanisms

## Conclusion

The WMS fulfillment implementation provides:

- **Complete API Compliance**: Matches exact specifications
- **Robust Error Handling**: Graceful failure management
- **Clean Architecture**: Follows DDD principles
- **Comprehensive Testing**: Full test coverage
- **Future-Ready Design**: Extensible for enhancements

This implementation ensures reliable communication between the order management system and warehouse management system, enabling seamless order fulfillment workflows. 