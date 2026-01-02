-- Migration: Create payment_orders table for Cashfree integration
-- Date: 2026-01-03

CREATE TABLE IF NOT EXISTS payment_orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    cf_order_id VARCHAR(100) UNIQUE NULL,           -- Cashfree's order ID
    order_id VARCHAR(100) UNIQUE NOT NULL,          -- Our internal order ID
    order_amount DECIMAL(10,2) NOT NULL,
    order_currency VARCHAR(10) DEFAULT 'INR',
    order_status ENUM('PENDING', 'ACTIVE', 'PAID', 'EXPIRED', 'FAILED', 'CANCELLED') DEFAULT 'PENDING',
    payment_session_id TEXT NULL,                   -- For Cashfree checkout
    cf_payment_id VARCHAR(100) NULL,                -- Cashfree payment ID after success

    -- Order details (what user is purchasing)
    user_count INT DEFAULT 0,
    storage_gb INT DEFAULT 0,
    duration_months INT DEFAULT 1,
    is_renewing TINYINT(1) DEFAULT 0,
    add_users INT DEFAULT 0,
    add_storage INT DEFAULT 0,
    promo_code VARCHAR(50) NULL,
    promo_discount_percent DECIMAL(5,2) DEFAULT 0,

    -- Payment metadata
    payment_method VARCHAR(50) NULL,
    bank_reference VARCHAR(100) NULL,
    payment_time DATETIME NULL,
    webhook_received_at DATETIME NULL,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_cf_order_id (cf_order_id),
    INDEX idx_order_status (order_status),
    INDEX idx_created_at (created_at),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
