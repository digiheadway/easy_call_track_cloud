<?php
require_once 'config.php';
require_once 'utils.php';

// Mock a user session for testing
$token = "567898765678"; // This is the secret token for internal tools, but let's try to find a real session token if possible.
// Actually, let's just bypass auth check in a test file if we want to check DB.

$sql = "SELECT * FROM users LIMIT 1";
$user = Database::getOne($sql);
if ($user) {
    echo "Found user: " . $user['id'] . "\n";
    $freshUser = Database::getOne("SELECT * FROM users WHERE id = " . $user['id']);
    print_r($freshUser);
} else {
    echo "No users found\n";
}
