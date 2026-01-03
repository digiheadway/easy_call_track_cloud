<?php
require_once 'config.php';
require_once 'utils.php';

// Add columns to users table
$sqls = [
    "ALTER TABLE users ADD COLUMN otp VARCHAR(10) DEFAULT NULL",
    "ALTER TABLE users ADD COLUMN otp_expiry DATETIME DEFAULT NULL",
    "ALTER TABLE users ADD COLUMN is_verified TINYINT(1) DEFAULT 0",
    "UPDATE users SET is_verified = 1"
];

foreach ($sqls as $sql) {
    $result = Database::execute($sql);
    echo "Executed: $sql <br>";
    echo "Result: " . json_encode($result) . "<br><br>";
}

echo "Migration completed.";
?>
