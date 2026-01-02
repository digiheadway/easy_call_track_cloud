<?php
require_once 'config.php';
require_once 'utils.php';

// disable error reporting to output only json
error_reporting(0);
ini_set('display_errors', 0);

header('Content-Type: application/json');

try {
    // 1. Check for 'labels' column
    $checkLabels = Database::getOne("SHOW COLUMNS FROM calls LIKE 'labels'");
    if (!$checkLabels) {
        Database::execute("ALTER TABLE calls ADD COLUMN labels TEXT DEFAULT NULL");
        $labelsMsg = "Added 'labels' column.";
    } else {
        $labelsMsg = "'labels' column exists.";
    }

    // 2. Check for 'is_liked' column
    $checkLiked = Database::getOne("SHOW COLUMNS FROM calls LIKE 'is_liked'");
    if (!$checkLiked) {
        Database::execute("ALTER TABLE calls ADD COLUMN is_liked TINYINT(1) DEFAULT 0");
        $likedMsg = "Added 'is_liked' column.";
    } else {
        $likedMsg = "'is_liked' column exists.";
    }

    // 3. Check for 'device_id' in employees
    $checkDeviceId = Database::getOne("SHOW COLUMNS FROM employees LIKE 'device_id'");
    if (!$checkDeviceId) {
        Database::execute("ALTER TABLE employees ADD COLUMN device_id VARCHAR(255) DEFAULT NULL");
        $deviceMsg = "Added 'device_id' column.";
    } else {
        $deviceMsg = "'device_id' column exists.";
    }

    // 4. Check for 'last_sync' in employees
    $checkLastSync = Database::getOne("SHOW COLUMNS FROM employees LIKE 'last_sync'");
    if (!$checkLastSync) {
        Database::execute("ALTER TABLE employees ADD COLUMN last_sync DATETIME DEFAULT NULL");
        $syncMsg = "Added 'last_sync' column.";
    } else {
        $syncMsg = "'last_sync' column exists.";
    }

    echo json_encode([
        "status" => true, 
        "message" => "Migration complete. $labelsMsg $likedMsg $deviceMsg $syncMsg"
    ]);

} catch (Exception $e) {
    echo json_encode([
        "status" => false, 
        "message" => $e->getMessage()
    ]);
}
