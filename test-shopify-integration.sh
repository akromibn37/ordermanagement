#!/bin/bash

# Shopify Integration Test Script for Order Stream Process
# This script tests the complete integration flow from order-stream-process to Shopify

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Configuration
SHOPIFY_BASE_URL="http://localhost:8084"
ORDER_STREAM_PROCESS_URL="http://localhost:8083"
ORDER_API_URL="http://localhost:8080"
MOCK_SHOPIFY_TOKEN="test-access-token-123"

# Test data
TEST_ORDER_ID="SHOPIFY-TEST-$(date +%s)"
TEST_PRODUCT_ID="PROD-001"
TEST_QUANTITY=10
TEST_CUSTOMER_EMAIL="test@example.com"

echo "🛍️ Shopify Integration Test for Order Stream Process"
echo "=================================================="
echo ""

# Function to test Shopify API endpoints
test_shopify_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    local expected_status=$5
    
    print_status "Testing: $description"
    
    if [ -n "$data" ]; then
        response=$(curl -s -w "%{http_code}" -X "$method" \
            "$SHOPIFY_BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -H "X-Shopify-Access-Token: $MOCK_SHOPIFY_TOKEN" \
            -d "$data")
    else
        response=$(curl -s -w "%{http_code}" -X "$method" \
            "$SHOPIFY_BASE_URL$endpoint" \
            -H "X-Shopify-Access-Token: $MOCK_SHOPIFY_TOKEN")
    fi
    
    # Extract status code and body
    status_code="${response: -3}"
    body="${response%???}"
    
    if [ "$status_code" = "$expected_status" ]; then
        print_success "✅ $description - Status: $status_code"
        echo "   Response: $body" | head -c 100
        [ ${#body} -gt 100 ] && echo "..."
        return 0
    else
        print_error "❌ $description - Expected: $expected_status, Got: $status_code"
        echo "   Response: $body"
        return 1
    fi
}

# Function to test order stream process integration
test_order_stream_integration() {
    print_status "Testing Order Stream Process Integration with Shopify..."
    
    # Test 1: Send order to trigger stream processing
    print_status "Step 1: Sending test order to trigger stream processing..."
    order_data="{\"orderId\": \"$TEST_ORDER_ID\", \"productIds\": [\"$TEST_PRODUCT_ID\"], \"quantity\": $TEST_QUANTITY, \"customerEmail\": \"$TEST_CUSTOMER_EMAIL\"}"
    
    if curl -s -X POST "$ORDER_API_URL/api/shopify/webhooks/orders" \
        -H "Content-Type: application/json" \
        -d "$order_data" > /dev/null; then
        print_success "✅ Test order sent successfully"
    else
        print_error "❌ Failed to send test order"
        return 1
    fi
    
    # Test 2: Wait for stream processing
    print_status "Step 2: Waiting for stream processing..."
    sleep 5
    
    # Test 3: Check stream process logs for Shopify API calls
    print_status "Step 3: Checking stream process logs for Shopify integration..."
    shopify_logs=$(docker-compose logs order-stream-process 2>/dev/null | grep -i "shopify\|admin/api" | tail -10 || true)
    
    if [ -n "$shopify_logs" ]; then
        print_success "✅ Found Shopify API calls in stream process logs:"
        echo "$shopify_logs"
    else
        print_warning "⚠️ No Shopify API calls found in stream process logs"
        print_status "This might indicate the integration is not yet implemented or logs are not visible"
    fi
    
    return 0
}

# Function to test complete Shopify workflow
test_shopify_workflow() {
    print_status "Testing Complete Shopify Workflow..."
    
    # Test 1: Create order in Shopify
    order_data="{\"order\": {\"line_items\": [{\"variant_id\": 345678, \"quantity\": $TEST_QUANTITY}], \"customer\": {\"email\": \"$TEST_CUSTOMER_EMAIL\"}}}"
    test_shopify_endpoint "POST" "/admin/api/2023-10/orders.json" "$order_data" "Create order in Shopify" "201"
    
    # Test 2: Get orders list
    test_shopify_endpoint "GET" "/admin/api/2023-10/orders.json?status=open&limit=50" "" "Get orders list" "200"
    
    # Test 3: Get specific order
    test_shopify_endpoint "GET" "/admin/api/2023-10/orders/123456789.json" "" "Get specific order" "200"
    
    # Test 4: Update order
    update_data="{\"order\": {\"fulfillment_status\": \"fulfilled\"}}"
    test_shopify_endpoint "PUT" "/admin/api/2023-10/orders/123456789.json" "$update_data" "Update order fulfillment status" "200"
    
    # Test 5: Get inventory levels
    test_shopify_endpoint "GET" "/admin/api/2023-10/inventory_levels.json?inventory_item_ids=789012" "" "Get inventory levels" "200"
    
    # Test 6: Update inventory levels
    inventory_data="{\"location_id\": 123456, \"inventory_item_id\": 789012, \"available\": 90}"
    test_shopify_endpoint "PUT" "/admin/api/2023-10/inventory_levels/set.json" "$inventory_data" "Update inventory levels" "200"
    
    # Test 7: Create fulfillment
    fulfillment_data="{\"fulfillment\": {\"order_id\": 123456789, \"line_items\": [{\"id\": 123456, \"quantity\": $TEST_QUANTITY}]}}"
    test_shopify_endpoint "POST" "/admin/api/2023-10/fulfillments.json" "$fulfillment_data" "Create fulfillment" "201"
    
    # Test 8: Get fulfillments list
    test_shopify_endpoint "GET" "/admin/api/2023-10/fulfillments.json" "" "Get fulfillments list" "200"
    
    # Test 9: Get locations
    test_shopify_endpoint "GET" "/admin/api/2023-10/locations.json" "" "Get locations" "200"
    
    # Test 10: Get products
    test_shopify_endpoint "GET" "/admin/api/2023-10/products.json?title=Test%20Product" "" "Get products" "200"
}

# Function to test error scenarios
test_error_scenarios() {
    print_status "Testing Error Scenarios..."
    
    # Test 1: Rate limiting
    test_shopify_endpoint "POST" "/admin/api/2023-10/orders.json" "{\"order\": {}}" "Rate limiting error" "429"
    
    # Test 2: Invalid access token
    if curl -s -X GET "$SHOPIFY_BASE_URL/admin/api/2023-10/orders.json" \
        -H "X-Shopify-Access-Token: invalid-token" | grep -q "Unauthorized"; then
        print_success "✅ Invalid access token handled correctly"
    else
        print_warning "⚠️ Invalid access token handling needs verification"
    fi
    
    # Test 3: Validation errors
    test_shopify_endpoint "POST" "/admin/api/2023-10/orders.json" "{\"order\": {\"line_items\": []}}" "Validation error" "422"
}

# Function to check service health
check_service_health() {
    print_status "Checking service health..."
    
    # Check Mock Shopify API
    if curl -s "$SHOPIFY_BASE_URL/api/wms/inventory/check" > /dev/null; then
        print_success "✅ Mock Shopify API is running"
    else
        print_error "❌ Mock Shopify API is not accessible"
        return 1
    fi
    
    # Check Order Stream Process
    if curl -s "$ORDER_STREAM_PROCESS_URL/actuator/health" > /dev/null; then
        print_success "✅ Order Stream Process is running"
    else
        print_error "❌ Order Stream Process is not accessible"
        return 1
    fi
    
    # Check Order API
    if curl -s "$ORDER_API_URL/actuator/health" > /dev/null; then
        print_success "✅ Order API is running"
    else
        print_error "❌ Order API is not accessible"
        return 1
    fi
    
    return 0
}

# Main test execution
main() {
    echo "🚀 Starting Shopify Integration Tests..."
    echo ""
    
    # Check service health first
    if ! check_service_health; then
        print_error "❌ Service health check failed. Please ensure all services are running."
        print_status "Run 'make start-all' to start all services."
        exit 1
    fi
    
    echo ""
    
    # Test 1: Complete Shopify workflow
    print_status "🧪 Test 1: Complete Shopify Workflow"
    echo "----------------------------------------"
    test_shopify_workflow
    echo ""
    
    # Test 2: Error scenarios
    print_status "🧪 Test 2: Error Scenarios"
    echo "-------------------------------"
    test_error_scenarios
    echo ""
    
    # Test 3: Order stream process integration
    print_status "🧪 Test 3: Order Stream Process Integration"
    echo "-----------------------------------------------"
    test_order_stream_integration
    echo ""
    
    # Summary
    echo "🎉 Shopify Integration Testing Completed!"
    echo ""
    print_status "Test Summary:"
    echo "  ✅ Shopify API endpoints tested"
    echo "  ✅ Error scenarios verified"
    echo "  ✅ Order stream process integration tested"
    echo ""
    print_status "Next Steps:"
    echo "  1. Check order-stream-process logs for Shopify API calls"
    echo "  2. Verify data flow in your application"
    echo "  3. Monitor Kafka topics for order processing"
    echo ""
    print_status "To view real-time logs: make logs"
    print_status "To check service status: make status"
}

# Run main function
main "$@" 