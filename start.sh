#!/bin/bash

echo "🚀 Starting Order Management System Setup..."

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

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "Docker Compose is not installed. Please install it and try again."
    exit 1
fi

print_status "Building order-manage-data-api..."
cd order-manage-data-api
if ./gradlew build -x test; then
    print_status "✅ order-manage-data-api built successfully"
else
    print_error "❌ Failed to build order-manage-data-api"
    exit 1
fi
cd ..

print_status "Building order-manage-api..."
cd order-manage-api
if ./gradlew build -x test; then
    print_status "✅ order-manage-api built successfully"
else
    print_error "❌ Failed to build order-manage-api"
    exit 1
fi
cd ..

print_status "Building order-stream-process..."
cd order-stream-process
if ./gradlew build -x test; then
    print_status "✅ order-stream-process built successfully"
else
    print_error "❌ Failed to build order-stream-process"
    exit 1
fi
cd ..

print_status "All services built successfully! 🎉"

print_status "Starting Docker Compose services..."
if docker-compose up -d; then
    print_status "✅ Docker Compose services started successfully"
else
    print_error "❌ Failed to start Docker Compose services"
    exit 1
fi

print_status "Waiting for services to be healthy..."
sleep 10

# Check service status
print_status "Checking service status..."
docker-compose ps

print_status "🎉 Order Management System is starting up!"
print_status "Services will be available at:"
echo "  - order-manage-api: http://localhost:8080"
echo "  - order-manage-data-api: http://localhost:8081"
echo "  - order-stream-process: http://localhost:8083"
echo "  - Mock WMS API: http://localhost:8084"
echo "  - PostgreSQL: localhost:5432"
echo "  - Kafka: localhost:9092"

print_status "Use 'docker-compose logs -f' to view logs"
print_status "Use 'docker-compose down' to stop all services" 