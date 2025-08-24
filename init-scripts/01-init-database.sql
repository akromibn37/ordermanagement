-- =====================================================
-- ORDER MANAGEMENT DATABASE INITIALIZATION
-- Updated to match test script requirements exactly
-- =====================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Set search path to default
SET search_path TO public;

-- =====================================================
-- ORDERS TABLE (matches OrderEntity)
-- =====================================================
DROP TABLE IF EXISTS order_detail CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS inventory CASCADE;

CREATE TABLE orders (
    order_id SERIAL PRIMARY KEY,
    order_number INTEGER UNIQUE NOT NULL,
    customer_id VARCHAR(20) NOT NULL,
    product_type_count INTEGER NOT NULL,
    total_price VARCHAR(20) NOT NULL,
    order_status VARCHAR(10) NOT NULL,
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(50) NOT NULL
);

-- =====================================================
-- ORDER DETAIL TABLE (matches OrderDetailEntity)
-- =====================================================
CREATE TABLE order_detail (
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders(order_id),
    product_id BIGINT NOT NULL,
    price VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(50) NOT NULL
);

-- =====================================================
-- INVENTORY TABLE (matches InventoryEntity)
-- =====================================================
CREATE TABLE inventory (
    product_id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(20) UNIQUE NOT NULL,
    product_title VARCHAR(10) NOT NULL,
    product_price VARCHAR(20) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    available_quantity INTEGER NOT NULL,
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(50) NOT NULL
);

-- =====================================================
-- INDEXES
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_orders_order_number ON orders(order_number);
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_order_detail_order_id ON order_detail(order_id);
CREATE INDEX IF NOT EXISTS idx_order_detail_product_id ON order_detail(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_sku ON inventory(sku);
CREATE INDEX IF NOT EXISTS idx_inventory_available_quantity ON inventory(available_quantity);

-- =====================================================
-- CLEAR EXISTING DATA (to ensure clean state)
-- =====================================================
-- Tables are dropped and recreated above, so no need to clear data or reset sequences

-- =====================================================
-- SAMPLE INVENTORY DATA (matches test script requirements)
-- =====================================================

-- Insert sample products into inventory with specific IDs that match test scripts
-- Test 3.1: productId 1 (T-Shirt) - needs 1 quantity, has 100 ✅
-- Test 3.2: productId 1,2,3 (T-Shirt, Jeans, Shoes) - needs 2,1,1 quantities, has 100,75,50 ✅
-- Test 3.3: productId 1,2,3,4,5 (all products) - needs 1 quantity each, has sufficient ✅
-- Test 3.7: productId 6 (Socks) - needs 200 quantity, has 150 (insufficient) ❌
INSERT INTO inventory (product_id, sku, product_title, product_price, currency, available_quantity, create_date, update_date, create_by, update_by) VALUES
(1, 'PROD-001', 'T-Shirt', '25.00', 'USD', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
(2, 'PROD-002', 'Jeans', '50.00', 'USD', 75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
(3, 'PROD-003', 'Shoes', '80.00', 'USD', 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
(4, 'PROD-004', 'Hat', '15.00', 'USD', 200, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
(5, 'PROD-005', 'Bag', '35.00', 'USD', 60, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
(6, 'PROD-006', 'Socks', '8.00', 'USD', 150, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
(7, 'PROD-007', 'Belt', '20.00', 'USD', 80, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
(8, 'PROD-008', 'Watch', '120.00', 'USD', 25, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
(9, 'PROD-009', 'Wallet', '30.00', 'USD', 90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
(10, 'PROD-010', 'Scarf', '18.00', 'USD', 120, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system');

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================
SELECT 'Database initialization completed successfully!' as status;

SELECT COUNT(*) as total_products FROM inventory;
SELECT COUNT(*) as total_orders FROM orders;
SELECT COUNT(*) as total_order_details FROM order_detail;

-- Show all inventory items with their IDs
SELECT 
    product_id,
    sku,
    product_title,
    product_price,
    available_quantity,
    currency
FROM inventory 
ORDER BY product_id;

-- Verify that product IDs 1-6 exist and have appropriate inventory for tests
SELECT 
    product_id,
    sku,
    product_title,
    available_quantity,
    CASE 
        WHEN product_id = 6 AND available_quantity < 200 THEN '❌ INSUFFICIENT (Test 3.7 needs 200, has 150)'
        WHEN available_quantity >= 10 THEN '✅ SUFFICIENT'
        WHEN available_quantity >= 5 THEN '⚠️ MODERATE'
        ELSE '❌ LOW'
    END as inventory_status
FROM inventory 
WHERE product_id BETWEEN 1 AND 6
ORDER BY product_id;

-- Additional verification to ensure database is fully ready
SELECT 'Database is ready for testing!' as readiness_check;
SELECT NOW() as current_timestamp; 