-- ============================================================
-- FRESH DATABASE SCHEMA FOR CALL TRACKING SYSTEM
-- Generated: 2026-01-11
-- ============================================================

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";

START TRANSACTION;

SET time_zone = "+00:00";

-- ============================================================
-- TABLE 1: call_log
-- Purpose: Central ledger for all individual call events
-- ============================================================
CREATE TABLE IF NOT EXISTS call_log (
    id INT(11) NOT NULL AUTO_INCREMENT,
    unique_id VARCHAR(100) NOT NULL COMMENT 'UID from phone (type-dev-phone-time)',
    org_id VARCHAR(20) NOT NULL COMMENT 'Organization ID',
    employee_id VARCHAR(20) NOT NULL COMMENT 'Staff member device ID',
    caller_phone VARCHAR(20) NOT NULL COMMENT 'Normalized digits only',
    caller_name VARCHAR(100) DEFAULT NULL,
    duration INT(11) DEFAULT 0 COMMENT 'Duration in seconds',
    type ENUM(
        'incoming',
        'outgoing',
        'missed',
        'rejected',
        'blocked',
        'unknown'
    ) DEFAULT 'incoming',
    call_time DATETIME NOT NULL COMMENT 'When the call occurred',
    file_status ENUM(
        'pending',
        'completed',
        'not_found',
        'failed'
    ) DEFAULT 'pending',
    recording_url TEXT DEFAULT NULL COMMENT 'Cloud path to audio file',
    reviewed TINYINT(1) DEFAULT 0 COMMENT '0=New, 1=Reviewed',
    note TEXT DEFAULT NULL COMMENT 'Note for this call',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY idx_uid (unique_id),
    INDEX idx_sync (
        org_id,
        employee_id,
        updated_at
    ),
    INDEX idx_lookup (caller_phone, call_time),
    INDEX idx_org_time (org_id, call_time),
    INDEX idx_reviewed (org_id, reviewed)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ============================================================
-- TABLE 2: call_log_phones
-- Purpose: CRM contact profiles and aggregated statistics
-- ============================================================
CREATE TABLE IF NOT EXISTS call_log_phones (
    id INT(11) NOT NULL AUTO_INCREMENT,
    org_id VARCHAR(20) NOT NULL,
    phone VARCHAR(20) NOT NULL COMMENT 'Normalized digits only',
    name VARCHAR(255) DEFAULT NULL,
    label VARCHAR(50) DEFAULT NULL COMMENT 'e.g. Lead, Customer',
    person_note TEXT DEFAULT NULL COMMENT 'Main note for contact',
    fully_reviewed TINYINT(1) DEFAULT 0 COMMENT '1=Manager finished checking',
    last_employee_id VARCHAR(20) DEFAULT NULL COMMENT 'Last staff to handle',
    first_call_time DATETIME DEFAULT NULL COMMENT 'Lead entry date',
    total_calls INT(11) DEFAULT 0,
    total_duration BIGINT(20) DEFAULT 0,
    total_connected INT(11) DEFAULT 0 COMMENT 'Calls with duration > 0',
    total_not_answered INT(11) DEFAULT 0 COMMENT 'Missed + Rejected + 0-sec',
    total_incoming INT(11) DEFAULT 0,
    total_outgoing INT(11) DEFAULT 0,
    total_missed INT(11) DEFAULT 0,
    total_rejected INT(11) DEFAULT 0,
    last_call_time DATETIME DEFAULT NULL,
    last_call_type ENUM(
        'incoming',
        'outgoing',
        'missed',
        'rejected',
        'blocked'
    ) DEFAULT NULL,
    last_call_duration INT(11) DEFAULT 0,
    exclude_from_sync TINYINT(1) DEFAULT 0 COMMENT '1=Stop tracking',
    exclude_from_list TINYINT(1) DEFAULT 0 COMMENT '1=Hide from UI',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY idx_org_phone (org_id, phone),
    INDEX idx_review_queue (
        org_id,
        fully_reviewed,
        last_call_time
    ),
    INDEX idx_list_sort (org_id, last_call_time),
    INDEX idx_sync_pull (org_id, updated_at),
    INDEX idx_label (org_id, label)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ============================================================
-- TABLE 3: employees
-- Purpose: Staff/Device management
-- ============================================================
CREATE TABLE IF NOT EXISTS employees (
    id INT(11) NOT NULL AUTO_INCREMENT,
    org_id VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    device_id VARCHAR(100) DEFAULT NULL COMMENT 'Android ID of device',
    status ENUM('active', 'inactive') DEFAULT 'active',
    join_date DATE NOT NULL,
    expiry_date DATETIME DEFAULT NULL,
    last_sync DATETIME DEFAULT NULL,
    call_track TINYINT(1) DEFAULT 1,
    call_record_crm TINYINT(1) DEFAULT 1,
    allow_personal_exclusion TINYINT(1) DEFAULT 0,
    allow_changing_tracking_start_date TINYINT(1) DEFAULT 0,
    allow_updating_tracking_sims TINYINT(1) DEFAULT 0,
    default_tracking_starting_date DATETIME DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_org (org_id),
    INDEX idx_device (device_id),
    INDEX idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ============================================================
-- TABLE 4: users (Admin/Manager accounts)
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id INT(11) NOT NULL AUTO_INCREMENT,
    org_id VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    org_name VARCHAR(255) DEFAULT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('admin', 'manager', 'user') DEFAULT 'admin',
    status VARCHAR(50) DEFAULT 'active',
    plan_info TEXT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY email (email),
    INDEX idx_org (org_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ============================================================
-- TABLE 5: sessions
-- ============================================================
CREATE TABLE IF NOT EXISTS sessions (
    id INT(11) NOT NULL AUTO_INCREMENT,
    user_id INT(11) NOT NULL,
    token VARCHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY token (token),
    INDEX idx_user (user_id),
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ============================================================
-- TABLE 6: excluded_contacts (Org-level exclusions)
-- ============================================================
CREATE TABLE IF NOT EXISTS excluded_contacts (
    id INT(11) NOT NULL AUTO_INCREMENT,
    org_id VARCHAR(20) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    name VARCHAR(100) DEFAULT NULL,
    exclude_from_sync TINYINT(1) DEFAULT 1,
    exclude_from_list TINYINT(1) DEFAULT 1,
    is_active TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_org (org_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ============================================================
-- TABLE 7: settings (Key-value org settings)
-- ============================================================
CREATE TABLE IF NOT EXISTS settings (
    id INT(11) NOT NULL AUTO_INCREMENT,
    org_id VARCHAR(20) NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY unique_org_setting (org_id, setting_key),
    INDEX idx_org (org_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ============================================================
-- TRIGGER: Auto-update call_log_phones when new call inserted
-- ============================================================
DELIMITER $$

CREATE TRIGGER after_call_log_insert 
AFTER INSERT ON call_log 
FOR EACH ROW 
BEGIN
  INSERT INTO call_log_phones (
    org_id, phone, name, last_employee_id, first_call_time,
    total_calls, total_duration, total_connected, total_not_answered,
    total_incoming, total_outgoing, total_missed, total_rejected,
    last_call_time, last_call_type, last_call_duration
  ) VALUES (
    NEW.org_id, NEW.caller_phone, NEW.caller_name, NEW.employee_id, NEW.call_time,
    1, NEW.duration,
    IF(NEW.duration > 0, 1, 0),
    IF(NEW.duration = 0 OR NEW.type IN ('missed', 'rejected'), 1, 0),
    IF(NEW.type = 'incoming', 1, 0),
    IF(NEW.type = 'outgoing', 1, 0),
    IF(NEW.type = 'missed', 1, 0),
    IF(NEW.type = 'rejected', 1, 0),
    NEW.call_time, NEW.type, NEW.duration
  )
  ON DUPLICATE KEY UPDATE
    name = IF(NEW.caller_name IS NOT NULL AND NEW.caller_name != '', NEW.caller_name, name),
    last_employee_id = NEW.employee_id,
    total_calls = total_calls + 1,
    total_duration = total_duration + NEW.duration,
    total_connected = total_connected + IF(NEW.duration > 0, 1, 0),
    total_not_answered = total_not_answered + IF(NEW.duration = 0 OR NEW.type IN ('missed', 'rejected'), 1, 0),
    total_incoming = total_incoming + IF(NEW.type = 'incoming', 1, 0),
    total_outgoing = total_outgoing + IF(NEW.type = 'outgoing', 1, 0),
    total_missed = total_missed + IF(NEW.type = 'missed', 1, 0),
    total_rejected = total_rejected + IF(NEW.type = 'rejected', 1, 0),
    last_call_time = NEW.call_time,
    last_call_type = NEW.type,
    last_call_duration = NEW.duration,
    fully_reviewed = 0;
END$$

DELIMITER;

COMMIT;