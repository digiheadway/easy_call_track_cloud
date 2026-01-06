-- Add device info columns to employees table
ALTER TABLE `employees` 
ADD COLUMN `device_model` VARCHAR(100) DEFAULT NULL AFTER `device_id`,
ADD COLUMN `os_version` VARCHAR(50) DEFAULT NULL AFTER `device_model`,
ADD COLUMN `battery_level` INT DEFAULT NULL AFTER `os_version`;
