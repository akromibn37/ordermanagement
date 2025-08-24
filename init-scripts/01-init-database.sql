-- =====================================================
-- ORDER MANAGEMENT DATABASE INITIALIZATION
-- Simplified for order-manage-data-api service
-- =====================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Set search path to default
SET search_path TO public;

-- =====================================================
-- ORDERS TABLE (matches OrderEntity)
-- =====================================================
CREATE TABLE IF NOT EXISTS orders (
    order_id BIGSERIAL PRIMARY KEY,
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
CREATE TABLE IF NOT EXISTS order_detail (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(order_id),
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
CREATE TABLE IF NOT EXISTS inventory (
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
-- SAMPLE DATA
-- =====================================================

-- Insert sample products into inventory
INSERT INTO inventory (sku, product_title, product_price, currency, available_quantity, create_date, update_date, create_by, update_by) VALUES
('PROD-001', 'T-Shirt', '25.00', 'USD', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
('PROD-002', 'Jeans', '50.00', 'USD', 75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
('PROD-003', 'Shoes', '80.00', 'USD', 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
('PROD-004', 'Hat', '15.00', 'USD', 200, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
('PROD-005', 'Bag', '35.00', 'USD', 60, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system')
ON CONFLICT (sku) DO NOTHING;

-- Insert sample orders
INSERT INTO orders (order_number, customer_id, product_type_count, total_price, order_status, create_date, update_date, create_by, update_by) VALUES
(1001, 'CUST-001', 2, '100.00', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),  -- 2 products: T-Shirt(2x$25) + Jeans(1x$50) = $100.00
(1002, 'CUST-002', 1, '80.00', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),   -- 1 product: Shoes(1x$80) = $80.00
(1003, 'CUST-003', 3, '125.00', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system')   -- 3 products: T-Shirt(1x$25) + Hat(2x$15) + Bag(2x$35) = $125.00
ON CONFLICT (order_number) DO NOTHING;

-- Insert sample order details (using numeric product IDs that match inventory.product_id)
-- Order 1001: product_type_count = 2, so we need 2 order_detail records
INSERT INTO order_detail (order_id, product_id, price, quantity, create_date, update_date, create_by, update_by) VALUES
(1, 1, '25.00', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),  -- Order 1001: 2x T-Shirt ($25.00 each) = $50.00
(1, 2, '50.00', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system')   -- Order 1001: 1x Jeans ($50.00) = $50.00
-- Total for Order 1001: $50.00 + $50.00 = $100.00
ON CONFLICT DO NOTHING;

-- Order 1002: product_type_count = 1, so we need 1 order_detail record  
INSERT INTO order_detail (order_id, product_id, price, quantity, create_date, update_date, create_by, update_by) VALUES
(2, 3, '80.00', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system')   -- Order 1002: 1x Shoes ($80.00) = $80.00
ON CONFLICT DO NOTHING;

-- Order 1003: product_type_count = 3, so we need 3 order_detail records
INSERT INTO order_detail (order_id, product_id, price, quantity, create_date, update_date, create_by, update_by) VALUES
(3, 1, '25.00', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),  -- Order 1003: 1x T-Shirt ($25.00) = $25.00
(3, 4, '15.00', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),  -- Order 1003: 2x Hat ($15.00 each) = $30.00
(3, 5, '35.00', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system')   -- Order 1003: 2x Bag ($35.00 each) = $70.00
-- Total for Order 1003: $25.00 + $30.00 + $70.00 = $125.00
ON CONFLICT DO NOTHING;

-- =====================================================
-- VERIFICATION
-- =====================================================
SELECT 'Database initialization completed successfully!' as status;

SELECT COUNT(*) as total_products FROM inventory;
SELECT COUNT(*) as total_orders FROM orders;
SELECT COUNT(*) as total_order_details FROM order_detail;

-- Verify data consistency: order_detail records should match product_type_count
SELECT 
    o.order_number,
    o.product_type_count as expected_products,
    COUNT(od.id) as actual_products,
    CASE 
        WHEN o.product_type_count = COUNT(od.id) THEN '✅ MATCH'
        ELSE '❌ MISMATCH'
    END as status
FROM orders o
LEFT JOIN order_detail od ON o.order_id = od.order_id
GROUP BY o.order_id, o.order_number, o.product_type_count
ORDER BY o.order_number; 