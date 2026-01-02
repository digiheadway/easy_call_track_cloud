ALTER TABLE `users`
ADD COLUMN `plan_expiry_date` DATETIME DEFAULT NULL,
ADD COLUMN `allowed_users_count` INT DEFAULT 5,
ADD COLUMN `allowed_storage_gb` DECIMAL(10,2) DEFAULT 1.00,
ADD COLUMN `storage_used_bytes` BIGINT DEFAULT 0,
ADD COLUMN `last_storage_check` DATETIME DEFAULT NULL;
