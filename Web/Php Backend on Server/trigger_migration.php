<?php
require_once 'config.php';

// Redefine Database class purely for this script if utils.php is not suitable or to avoid side effects
// But reusing utils.php is better if it works.
// However, init_database.php copies the curl logic. I will do the same to be safe.

echo "Migrating Database...\n";

$sql = "ALTER TABLE calls ADD COLUMN is_archived TINYINT(1) DEFAULT 0";

$ch = curl_init(MYSQL_MANAGER_URL);
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST => true,
    CURLOPT_HTTPHEADER => [
        'Authorization: Bearer ' . API_SECRET_TOKEN,
        'Content-Type: application/json'
    ],
    CURLOPT_POSTFIELDS => json_encode(['sql' => $sql])
]);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

echo "HTTP Code: " . $httpCode . "\n";
echo "Response: " . $response . "\n";

$result = json_decode($response, true);
if ($httpCode === 200 && ($result['status'] ?? false)) {
    echo "Migration Successful!\n";
} else {
    echo "Migration Failed or Column Already Exists (check error).\n";
}
?>
