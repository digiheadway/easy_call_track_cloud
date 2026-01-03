<?php
/**
 * Migration Script: Create upi_payments table and trigger
 * Run this by visiting: https://api.miniclickcrm.com/migration_upi.php
 */

require_once 'config.php';
require_once 'utils.php';

echo "<h2>UPI Payments Migration</h2>";

$sql_table = "CREATE TABLE IF NOT EXISTS `upi_payments` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `org_id` varchar(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `utr` varchar(50) NOT NULL,
  `user_count` int(11) DEFAULT NULL,
  `storage_gb` int(11) DEFAULT NULL,
  `duration_months` int(11) DEFAULT NULL,
  `is_renewing` tinyint(1) DEFAULT 0,
  `status` enum('pending','completed','failed') DEFAULT 'completed',
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_utr` (`utr`),
  KEY `idx_org_id` (`org_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

$sql_trigger_drop = "DROP TRIGGER IF EXISTS `after_upi_payment_insert`;";

$sql_trigger = "CREATE TRIGGER `after_upi_payment_insert` 
AFTER INSERT ON `upi_payments` 
FOR EACH ROW 
BEGIN
    IF NEW.status = 'completed' THEN
        UPDATE users SET 
            allowed_users_count = NEW.user_count,
            allowed_storage_gb = NEW.storage_gb,
            plan_expiry_date = IF(NEW.is_renewing = 1, 
                DATE_ADD(IF(plan_expiry_date > NOW(), plan_expiry_date, NOW()), INTERVAL NEW.duration_months MONTH),
                plan_expiry_date
            )
        WHERE id = NEW.user_id;
    END IF;
END;";

echo "Creating table... ";
$res1 = Database::execute($sql_table);
if ($res1['status']) echo "DONE<br>"; else echo "FAILED: " . ($res1['error'] ?? 'Unknown error') . "<br>";

echo "Dropping old trigger if exists... ";
$res2 = Database::execute($sql_trigger_drop);
if ($res2['status']) echo "DONE<br>"; else echo "FAILED: " . ($res2['error'] ?? 'Unknown error') . "<br>";

echo "Creating trigger... ";
$res3 = Database::execute($sql_trigger);
if ($res3['status']) echo "DONE<br>"; else echo "FAILED: " . ($res3['error'] ?? 'Unknown error') . "<br>";

echo "<br><b>Migration Finished.</b>";
?>
