#!/bin/bash

# Kafka to Shopify Integration Test Script
# This script tests the complete flow: Kafka message consumption -> order-stream-process -> Shopify API calls

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
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

print_kafka() {
    echo -e "${PURPLE}[KAFKA]${NC} $1"
}

# Configuration
KAFKA_BROKER="localhost:9092"
KAFKA_TOPIC="inventory-updates"
ORDER_API_URL="http://localhost:8080"
SHOPIFY_MOCK_URL="http://localhost:8084"
STREAM_PROCESS_URL="http://localhost:8083"

# Test data
TEST_ORDER_ID="KAFKA-TEST-$(date +%s)"
TEST_PRODUCT_ID="PROD-001"
TEST_QUANTITY=15
TEST_CUSTOMER_EMAIL="kafka-test@example.com"

echo "🔄 Kafka to Shopify Integration Flow Test"
echo "=========================================="
echo "This test validates the complete flow:"
echo "1. Send order to Order API"
echo "2. Order API publishes to Kafka topic"
echo "3. order-stream-process consumes Kafka message"
echo "4. order-stream-process calls Shopify APIs"
echo ""

# Function to check if Kafka is accessible
check_kafka_connectivity() {
    print_status "Checking Kafka connectivity..."
    
    if docker-compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list > /dev/null 2>&1; then
        print_success "✅ Kafka is accessible"
        return 0
    else
        print_error "❌ Kafka is not accessible"
        return 1
    fi
}

# Function to check if Kafka topic exists
check_kafka_topic() {
    print_status "Checking if Kafka topic '$KAFKA_TOPIC' exists..."
    
    if docker-compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list | grep -q "$KAFKA_TOPIC"; then
        print_success "✅ Kafka topic '$KAFKA_TOPIC' exists"
        return 0
    else
        print_warning "⚠️ Kafka topic '$KAFKA_TOPIC' does not exist, creating it..."
        docker-compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 --create --topic "$KAFKA_TOPIC" --partitions 1 --replication-factor 1
        print_success "✅ Kafka topic '$KAFKA_TOPIC' created"
        return 0
    fi
}

# Function to monitor Kafka topic for messages
monitor_kafka_topic() {
    local duration=$1
    print_kafka "Monitoring Kafka topic '$KAFKA_TOPIC' for $duration seconds..."
    
    # Start Kafka consumer in background to monitor messages
    docker-compose exec -T kafka kafka-console-consumer \
        --bootstrap-server localhost:9092 \
        --topic "$KAFKA_TOPIC" \
        --from-beginning \
        --timeout-ms 1000 > /tmp/kafka_messages.log 2>&1 &
    
    local consumer_pid=$!
    sleep "$duration"
    kill $consumer_pid 2>/dev/null || true
    
    # Check if any messages were received
    if [ -s /tmp/kafka_messages.log ]; then
        print_success "✅ Messages found in Kafka topic:"
        cat /tmp/kafka_messages.log
        return 0
    else
        print_warning "⚠️ No messages found in Kafka topic"
        return 1
    fi
}

# Function to send test order to trigger Kafka message
send_test_order() {
    print_status "Step 1: Sending test order to trigger Kafka message..."
    
    local order_data="{\"orderId\": \"$TEST_ORDER_ID\", \"productIds\": [\"$TEST_PRODUCT_ID\"], \"quantity\": $TEST_QUANTITY, \"customerEmail\": \"$TEST_CUSTOMER_EMAIL\"}"
    
    print_status "Order data: $order_data"
    
    if curl -s -X POST "$ORDER_API_URL/api/shopify/webhooks/orders" \
        -H "Content-Type: application/json" \
        -d "$order_data" > /dev/null; then
        print_success "✅ Test order sent successfully"
        return 0
    else
        print_error "❌ Failed to send test order"
        return 1
    fi
}

# Function to check order-stream-process logs for Kafka consumption
check_stream_process_logs() {
    print_status "Step 2: Checking order-stream-process logs for Kafka consumption..."
    
    # Wait for stream processing
    sleep 3
    
    # Check for Kafka consumption logs
    local kafka_logs=$(docker-compose logs order-stream-process 2>/dev/null | grep -i "kafka\|consumer\|message\|topic" | tail -10 || true)
    
    if [ -n "$kafka_logs" ]; then
        print_success "✅ Found Kafka-related logs in order-stream-process:"
        echo "$kafka_logs"
        return 0
    else
        print_warning "⚠️ No Kafka-related logs found in order-stream-process"
        return 1
    fi
}

# Function to check order-stream-process logs for Shopify API calls
check_shopify_integration_logs() {
    print_status "Step 3: Checking order-stream-process logs for Shopify API calls..."
    
    # Wait a bit more for Shopify processing
    sleep 5
    
    # Check for Shopify API call logs
    local shopify_logs=$(docker-compose logs order-stream-process 2>/dev/null | grep -i "shopify\|admin/api\|fulfillment\|inventory" | tail -10 || true)
    
    if [ -n "$shopify_logs" ]; then
        print_success "✅ Found Shopify integration logs in order-stream-process:"
        echo "$shopify_logs"
        return 0
    else
        print_warning "⚠️ No Shopify integration logs found in order-stream-process"
        print_status "This might indicate the integration is not yet implemented"
        return 1
    fi
}

# Function to verify Shopify API calls were made
verify_shopify_api_calls() {
    print_status "Step 4: Verifying Shopify API calls were made..."
    
    # Check if the mock Shopify API received calls
    local shopify_calls=$(docker-compose logs mock-wms-api 2>/dev/null | grep -i "admin/api" | tail -5 || true)
    
    if [ -n "$shopify_calls" ]; then
        print_success "✅ Found Shopify API calls in mock API logs:"
        echo "$shopify_calls"
        return 0
    else
        print_warning "⚠️ No Shopify API calls found in mock API logs"
        return 1
    fi
}

# Function to test direct Kafka message publishing and consumption
test_direct_kafka_flow() {
    print_status "Testing direct Kafka message publishing and consumption..."
    
    # Create a test message
    local test_message="{\"orderId\": \"DIRECT-KAFKA-TEST\", \"action\": \"inventory_update\", \"productId\": \"$TEST_PRODUCT_ID\", \"quantity\": $TEST_QUANTITY, \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"}"
    
    print_kafka "Publishing test message to Kafka topic '$KAFKA_TOPIC'..."
    print_kafka "Message: $test_message"
    
    # Publish message to Kafka
    echo "$test_message" | docker-compose exec -T kafka kafka-console-producer \
        --bootstrap-server localhost:9092 \
        --topic "$KAFKA_TOPIC"
    
    if [ $? -eq 0 ]; then
        print_success "✅ Test message published to Kafka successfully"
        
        # Wait for processing
        sleep 5
        
        # Check if order-stream-process processed the message
        local processing_logs=$(docker-compose logs order-stream-process 2>/dev/null | grep -i "DIRECT-KAFKA-TEST\|inventory_update" | tail -5 || true)
        
        if [ -n "$processing_logs" ]; then
            print_success "✅ order-stream-process processed the Kafka message:"
            echo "$processing_logs"
        else
            print_warning "⚠️ order-stream-process may not have processed the Kafka message"
        fi
    else
        print_error "❌ Failed to publish test message to Kafka"
    fi
}

# Function to check Kafka consumer group status
check_kafka_consumer_groups() {
    print_status "Checking Kafka consumer groups..."
    
    local consumer_groups=$(docker-compose exec -T kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list 2>/dev/null || true)
    
    if [ -n "$consumer_groups" ]; then
        print_success "✅ Active consumer groups:"
        echo "$consumer_groups"
        
        # Check specific consumer group details if order-stream-process group exists
        for group in $consumer_groups; do
            if echo "$group" | grep -q "order-stream\|stream-process"; then
                print_status "Checking consumer group: $group"
                docker-compose exec -T kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group "$group" 2>/dev/null || true
            fi
        done
    else
        print_warning "⚠️ No consumer groups found"
    fi
}

# Function to check service health
check_service_health() {
    print_status "Checking service health..."
    
    # Check Kafka
    if check_kafka_connectivity; then
        print_success "✅ Kafka is healthy"
    else
        print_error "❌ Kafka health check failed"
        return 1
    fi
    
    # Check Order API
    if curl -s "$ORDER_API_URL/actuator/health" > /dev/null; then
        print_success "✅ Order API is healthy"
    else
        print_error "❌ Order API health check failed"
        return 1
    fi
    
    # Check Order Stream Process
    if curl -s "$STREAM_PROCESS_URL/actuator/health" > /dev/null; then
        print_success "✅ Order Stream Process is healthy"
    else
        print_error "❌ Order Stream Process health check failed"
        return 1
    fi
    
    # Check Mock Shopify API
    if curl -s "$SHOPIFY_MOCK_URL/api/wms/inventory/check" > /dev/null; then
        print_success "✅ Mock Shopify API is healthy"
    else
        print_error "❌ Mock Shopify API health check failed"
        return 1
    fi
    
    return 0
}

# Main test execution
main() {
    echo "🚀 Starting Kafka to Shopify Integration Flow Test..."
    echo ""
    
    # Check service health first
    if ! check_service_health; then
        print_error "❌ Service health check failed. Please ensure all services are running."
        print_status "Run 'make start-all' to start all services."
        exit 1
    fi
    
    echo ""
    
    # Check Kafka topic
    if ! check_kafka_topic; then
        print_error "❌ Kafka topic setup failed"
        exit 1
    fi
    
    echo ""
    
    # Test 1: Complete flow through Order API
    print_status "🧪 Test 1: Complete Flow Through Order API"
    echo "------------------------------------------------"
    
    # Start monitoring Kafka topic before sending order
    print_kafka "Starting Kafka topic monitoring..."
    monitor_kafka_topic 10 &
    local monitor_pid=$!
    
    # Send test order
    if send_test_order; then
        print_success "✅ Order sent successfully"
    else
        print_error "❌ Order sending failed"
        kill $monitor_pid 2>/dev/null || true
        exit 1
    fi
    
    # Wait for monitoring to complete
    wait $monitor_pid
    
    # Check stream process logs
    check_stream_process_logs
    check_shopify_integration_logs
    verify_shopify_api_calls
    
    echo ""
    
    # Test 2: Direct Kafka message testing
    print_status "🧪 Test 2: Direct Kafka Message Testing"
    echo "---------------------------------------------"
    test_direct_kafka_flow
    
    echo ""
    
    # Test 3: Kafka consumer group status
    print_status "🧪 Test 3: Kafka Consumer Group Status"
    echo "--------------------------------------------"
    check_kafka_consumer_groups
    
    echo ""
    
    # Summary
    echo "🎉 Kafka to Shopify Integration Flow Testing Completed!"
    echo ""
    print_status "Test Summary:"
    echo "  ✅ Service health verified"
    echo "  ✅ Kafka topic configured"
    echo "  ✅ Order API to Kafka flow tested"
    echo "  ✅ Direct Kafka message testing completed"
    echo "  ✅ Consumer group status checked"
    echo ""
    print_status "Next Steps:"
    echo "  1. Check order-stream-process logs for detailed processing information"
    echo "  2. Monitor Kafka topics for message flow"
    echo "  3. Verify Shopify API calls in mock API logs"
    echo "  4. Check consumer group lag and offset information"
    echo ""
    print_status "To view real-time logs:"
    echo "  - All services: make logs"
    echo "  - Order Stream Process: docker-compose logs -f order-stream-process"
    echo "  - Kafka: docker-compose logs -f kafka"
    echo "  - Mock API: docker-compose logs -f mock-wms-api"
    echo ""
    print_status "To check service status: make status"
}

# Cleanup function
cleanup() {
    # Remove temporary files
    rm -f /tmp/kafka_messages.log
}

# Set trap for cleanup
trap cleanup EXIT

# Run main function
main "$@" 