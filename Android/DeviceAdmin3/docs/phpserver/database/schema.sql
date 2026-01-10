-- ============================================
-- Device Admin Protection System
-- SIMPLE Database Schema (2 Tables Only)
-- ============================================

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";

SET time_zone = "+05:30";

-- ============================================
-- 1. USERS TABLE (Managers)
-- ============================================
CREATE TABLE IF NOT EXISTS `users` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(150) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `phone` VARCHAR(20) NOT NULL,
    `company_name` VARCHAR(150) DEFAULT NULL,
    `api_key` VARCHAR(64) NOT NULL UNIQUE,
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_login_at` TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ============================================
-- 2. CUSTOMERS TABLE (includes device info)
-- ============================================
CREATE TABLE IF NOT EXISTS `customers` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` INT UNSIGNED NOT NULL,

-- Customer Info
`name` VARCHAR(100) NOT NULL,
`phone` VARCHAR(20) NOT NULL,
`email` VARCHAR(150) DEFAULT NULL,
`address` TEXT DEFAULT NULL,
`loan_amount` DECIMAL(12, 2) DEFAULT 0.00,
`pending_amount` DECIMAL(12, 2) DEFAULT 0.00,

-- Device Info
`pairing_code` VARCHAR(20) NOT NULL UNIQUE,
`device_name` VARCHAR(100) DEFAULT NULL,
`device_model` VARCHAR(100) DEFAULT NULL,
`imei` VARCHAR(20) DEFAULT NULL,
`imei2` VARCHAR(20) DEFAULT NULL,
`fcm_token` TEXT DEFAULT NULL,

-- Control Settings
`is_freezed` TINYINT(1) NOT NULL DEFAULT 0,
`is_protected` TINYINT(1) NOT NULL DEFAULT 1,
`freeze_message` VARCHAR(255) DEFAULT 'Device Locked - Contact Manager',
`call_to` VARCHAR(20) DEFAULT NULL,
`unlock_codes` TEXT DEFAULT NULL,

-- Status: 'active' = in use, 'used' = customer deleted but device was linked

`status` ENUM('active', 'used') NOT NULL DEFAULT 'active',
    `last_seen_at` TIMESTAMP NULL DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_pairing_code` (`pairing_code`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_phone` (`phone`),
    INDEX `idx_imei` (`imei`),
    INDEX `idx_imei2` (`imei2`),
    INDEX `idx_status` (`status`),
    CONSTRAINT `fk_customers_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- INSERT DEFAULT USER
-- ============================================
INSERT INTO
    `users` (
        `name`,
        `email`,
        `password`,
        `phone`,
        `company_name`,
        `api_key`
    )
VALUES (
        'Demo Manager',
        'manager@demo.com',
        '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
        '9068062563',
        'Demo Finance',
        'sk_demo_abc123xyz789'
    );

-- Sample Customer
INSERT INTO
    `customers` (
        `user_id`,
        `name`,
        `phone`,
        `loan_amount`,
        `pending_amount`,
        `pairing_code`,
        `device_name`,
        `is_freezed`,
        `is_protected`,
        `freeze_message`,
        `call_to`,
        `status`
    )
VALUES (
        1,
        'Rahul Kumar',
        '9876543210',
        50000.00,
        25000.00,
        'U1C1',
        'Samsung Galaxy A53',
        1,
        1,
        'Payment Pending - Contact Manager',
        '9068062563',
        'active'
    ),
    (
        1,
        'Amit Singh',
        '9876543211',
        30000.00,
        0.00,
        'U1C2',
        'Xiaomi Redmi Note 12',
        0,
        1,
        'Device Protected',
        '9068062563',
        'active'
    );