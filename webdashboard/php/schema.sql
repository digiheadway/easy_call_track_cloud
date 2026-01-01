-- CallCloud Admin Dashboard Database Schema
-- ============================================


-- Users/Admins Table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    org_id VARCHAR(6) NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('admin', 'manager', 'user') DEFAULT 'admin',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_org_id (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Employees Table
CREATE TABLE IF NOT EXISTS employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    org_id VARCHAR(6) NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    status ENUM('active', 'inactive') DEFAULT 'active',
    join_date DATE NOT NULL,
    device_id VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_org_id (org_id),
    INDEX idx_status (status),
    INDEX idx_device_id (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Contacts Table (for labeling persons)
CREATE TABLE IF NOT EXISTS contacts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    org_id VARCHAR(6) NOT NULL,
    employee_id INT, -- Added to track who saved/last interacted
    phone VARCHAR(20) NOT NULL,
    name VARCHAR(255),
    label VARCHAR(50), -- e.g. 'VIP', 'Spam', 'Lead'
    email VARCHAR(255),
    notes TEXT,
    incomings INT DEFAULT 0,
    incoming_connected INT DEFAULT 0,
    outgoings INT DEFAULT 0,
    outgoing_connected INT DEFAULT 0,
    last_call_type VARCHAR(20),
    last_call_duration INT DEFAULT 0,
    last_call_time DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE SET NULL,
    UNIQUE KEY unique_org_phone (org_id, phone),
    INDEX idx_phone (phone),
    INDEX idx_label (label),
    INDEX idx_employee_id (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Calls Table
CREATE TABLE IF NOT EXISTS calls (
    id INT AUTO_INCREMENT PRIMARY KEY,
    org_id VARCHAR(6) NOT NULL,
    employee_id INT NOT NULL,
    contact_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    direction ENUM('inbound', 'outbound') NOT NULL,
    duration VARCHAR(10) NOT NULL,
    status ENUM('completed', 'missed', 'rejected') DEFAULT 'completed',
    has_recording BOOLEAN DEFAULT FALSE,
    call_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    INDEX idx_org_id (org_id),
    INDEX idx_employee_id (employee_id),
    INDEX idx_direction (direction),
    INDEX idx_status (status),
    INDEX idx_call_timestamp (call_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Settings Table
CREATE TABLE IF NOT EXISTS settings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    org_id VARCHAR(6) NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_org_setting (org_id, setting_key),
    INDEX idx_org_id (org_id),
    INDEX idx_setting_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sessions Table (for authentication)
CREATE TABLE IF NOT EXISTS sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token VARCHAR(64) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Triggers to maintain contacts
DELIMITER //
CREATE TRIGGER after_call_insert
AFTER INSERT ON calls
FOR EACH ROW
BEGIN
    INSERT INTO contacts (org_id, employee_id, phone, name, 
        incomings, incoming_connected, outgoings, outgoing_connected, 
        last_call_type, last_call_duration, last_call_time)
    VALUES (NEW.org_id, NEW.employee_id, NEW.caller_phone, NEW.caller_name,
        IF(NEW.type = 'Inbound', 1, 0),
        IF(NEW.type = 'Inbound' AND NEW.duration > 0, 1, 0),
        IF(NEW.type = 'Outbound', 1, 0),
        IF(NEW.type = 'Outbound' AND NEW.duration > 0, 1, 0),
        NEW.type, NEW.duration, NEW.call_time)
    ON DUPLICATE KEY UPDATE
        employee_id = IF(NEW.employee_id IS NOT NULL, NEW.employee_id, employee_id),
        name = IF(NEW.caller_name IS NOT NULL AND NEW.caller_name != '', NEW.caller_name, name),
        incomings = incomings + IF(NEW.type = 'Inbound', 1, 0),
        incoming_connected = incoming_connected + IF(NEW.type = 'Inbound' AND NEW.duration > 0, 1, 0),
        outgoings = outgoings + IF(NEW.type = 'Outbound', 1, 0),
        outgoing_connected = outgoing_connected + IF(NEW.type = 'Outbound' AND NEW.duration > 0, 1, 0),
        last_call_type = NEW.type,
        last_call_duration = NEW.duration,
        last_call_time = NEW.call_time,
        updated_at = NOW();
END //
DELIMITER ;

