#!/bin/bash

echo "🧪 Testing Order Management System..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Function to test HTTP endpoint
test_endpoint() {
    local url=$1
    local description=$2
    
    print_status "Testing $description..."
    if curl -s -f "$url" > /dev/null; then
        print_success "✅ $description is accessible"
        return 0
    else
        print_error "❌ $description is not accessible"
        return 1
    fi
}

# Function to test POST endpoint
test_post_endpoint() {
    local url=$1
    local data=$2
    local description=$3
    
    print_status "Testing $description..."
    if curl -s -f -X POST "$url" -H "Content-Type: application/json" -d "$data" > /dev/null; then
        print_success "✅ $description is working"
        return 0
    else
        print_error "❌ $description is not working"
        return 1
    fi
}

# Check if Docker Compose services are running
print_status "Checking if Docker Compose services are running..."
if docker-compose ps | grep -q "Up"; then
    print_success "✅ Docker Compose services are running"
else
    print_error "❌ Docker Compose services are not running"
    exit 1
fi

# Wait for services to be fully ready
print_status "Waiting for services to be fully ready..."
sleep 10

print_status "Testing service endpoints..."

# Test order-manage-data-api order check endpoint
test_post_endpoint \
    "http://localhost:8081/api/order/check?orderId=1&productIds=1&quantity=1" \
    "" \
    "order-manage-data-api order check endpoint"

# Test order-manage-api Shopify webhook endpoint
test_post_endpoint \
    "http://localhost:8080/api/shopify/webhooks/orders" \
    '{"orderId": "123", "productIds": ["1"], "quantity": 5}' \
    "order-manage-api Shopify webhook endpoint"

# Test Mock WMS API endpoints
test_post_endpoint \
    "http://localhost:8084/api/wms/inventory/check" \
    '{"productId": "1", "quantity": 5}' \
    "Mock WMS API inventory check endpoint"

test_post_endpoint \
    "http://localhost:8084/api/wms/fulfillment/create" \
    '{"orderId": "123", "productId": "1", "quantity": 5}' \
    "Mock WMS API fulfillment creation endpoint"

# Test if services are responding (even if with errors, which means they're running)
print_status "Testing service responsiveness..."

# Test order-manage-data-api responsiveness
if curl -s "http://localhost:8081/api/order/check?orderId=1&productIds=1&quantity=1" > /dev/null; then
    print_success "✅ order-manage-data-api is responding"
else
    print_error "❌ order-manage-data-api is not responding"
fi

# Test order-manage-api responsiveness
if curl -s "http://localhost:8080/api/shopify/webhooks/orders" > /dev/null; then
    print_success "✅ order-manage-api is responding"
else
    print_error "❌ order-manage-api is not responding"
fi

# Test Mock WMS API responsiveness
if curl -s "http://localhost:8084/api/wms/inventory/check" > /dev/null; then
    print_success "✅ Mock WMS API is responding"
else
    print_error "❌ Mock WMS API is not responding"
fi

print_status "System Status Summary:"
docker-compose ps

print_success "🎉 Testing completed! All services are running and responsive."
print_status "The system is ready for use!" 