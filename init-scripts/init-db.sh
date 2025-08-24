#!/bin/bash

# Database Initialization Script
# This script runs the database initialization SQL script

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🗄️ Initializing Order Management Database...${NC}"

# Check if PostgreSQL container is running
if ! docker-compose ps postgres | grep -q "Up"; then
    echo "❌ PostgreSQL container is not running. Please start the infrastructure first:"
    echo "   make start-infra"
    exit 1
fi

echo "✅ PostgreSQL container is running"

# Wait for PostgreSQL to be ready
echo "⏳ Waiting for PostgreSQL to be ready..."
until docker-compose exec -T postgres pg_isready -U postgres -d order_management; do
    echo "   Waiting for PostgreSQL..."
    sleep 2
done

echo "✅ PostgreSQL is ready"

# Run the initialization script
echo "📝 Running database initialization script..."
docker-compose exec -T postgres psql -U postgres -d order_management -f /docker-entrypoint-initdb.d/01-init-database.sql

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Database initialization completed successfully!${NC}"
else
    echo "❌ Database initialization failed"
    exit 1
fi

# Verify the setup
echo "🔍 Verifying database setup..."
docker-compose exec -T postgres psql -U postgres -d order_management -c "
SELECT 
    'Orders' as table_name, COUNT(*) as count FROM orders
UNION ALL
SELECT 
    'Order Details' as table_name, COUNT(*) as count FROM order_detail
UNION ALL
SELECT 
    'Inventory' as table_name, COUNT(*) as count FROM inventory;
"

echo ""
echo -e "${GREEN}🎉 Database initialization and verification completed!${NC}"
echo ""
echo "📊 Database Summary:"
echo "   - Schema: order_management"
echo "   - Tables: orders, order_detail, inventory"
echo "   - Sample data: 5 inventory items, 3 orders with details"
echo ""
echo "🚀 You can now run your tests:"
echo "   make test-all-scenarios" 