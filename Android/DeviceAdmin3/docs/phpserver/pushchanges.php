<?php
/**
 * Push Commands API (Simple Version)
 * 
 * GET /pushchanges.php?pairingcode=1120&command=LOCK_DEVICE&api_key=xxx
 * GET /pushchanges.php?all=1&command=SYNC&api_key=xxx
 */

header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=utf-8");

require_once __DIR__ . '/config/database.php';
require_once __DIR__ . '/config/firebase.php';

// Get API key from header or param
$apiKey = $_SERVER['HTTP_X_API_KEY'] ?? $_GET['api_key'] ?? null;

if (!$apiKey) {
    die(json_encode(['success' => false, 'error' => 'API key required']));
}

// Verify user
$user = fetchOne("SELECT * FROM users WHERE api_key = ? AND is_active = 1", [$apiKey]);
if (!$user) {
    http_response_code(401);
    die(json_encode(['success' => false, 'error' => 'Invalid API key']));
}

$pairingCode = $_GET['pairingcode'] ?? null;
$command = $_GET['command'] ?? null;
$message = $_GET['message'] ?? null;
$amount = isset($_GET['amount']) ? (float)$_GET['amount'] : null;
$duration = isset($_GET['duration']) ? (int)$_GET['duration'] : 120;
$pushAll = ($_GET['all'] ?? '') === '1';

$commands = ['LOCK_DEVICE', 'UNLOCK_DEVICE', 'TEMPORAL_UNLOCK', 'REMOVE_PROTECTION', 'SYNC'];

if ($command && !in_array($command, $commands)) {
    die(json_encode(['success' => false, 'error' => 'Invalid command', 'available' => $commands]));
}

// Get target customers
if ($pushAll) {
    $customers = fetchAll(
        "SELECT * FROM customers WHERE user_id = ? AND status = 'active' AND fcm_token IS NOT NULL",
        [$user['id']]
    );
} else {
    if (!$pairingCode) {
        die(json_encode(['success' => false, 'error' => 'pairingcode or all=1 required']));
    }
    $customer = fetchOne(
        "SELECT * FROM customers WHERE pairing_code = ? AND user_id = ? AND status = 'active'",
        [$pairingCode, $user['id']]
    );
    if (!$customer) {
        die(json_encode(['success' => false, 'error' => 'Customer not found']));
    }
    $customers = [$customer];
}

$results = [];

foreach ($customers as $c) {
    $code = $c['pairing_code'];
    $data = ['command' => $command ?: 'SYNC'];
    
    // Update DB based on command
    switch ($command) {
        case 'LOCK_DEVICE':
            $msg = $message ?: $c['freeze_message'];
            $sql = "UPDATE customers SET is_freezed = 1, freeze_message = ?";
            $params = [$msg];
            if ($amount !== null) {
                $sql .= ", pending_amount = ?";
                $params[] = $amount;
            }
            $sql .= " WHERE id = ?";
            $params[] = $c['id'];
            query($sql, $params);
            $data['is_freezed'] = '1';
            $data['message'] = $msg;
            if ($amount !== null) $data['amount'] = (string)$amount;
            break;
            
        case 'UNLOCK_DEVICE':
            query("UPDATE customers SET is_freezed = 0 WHERE id = ?", [$c['id']]);
            $data['is_freezed'] = '0';
            break;
            
        case 'REMOVE_PROTECTION':
            query("UPDATE customers SET is_protected = 0, is_freezed = 0 WHERE id = ?", [$c['id']]);
            $data['is_protected'] = '0';
            $data['is_freezed'] = '0';
            break;
            
        default: // SYNC
            $data['is_freezed'] = $c['is_freezed'] ? '1' : '0';
            $data['is_protected'] = $c['is_protected'] ? '1' : '0';
            $data['message'] = $c['freeze_message'];
            $data['amount'] = (string)$c['pending_amount'];
            $data['unlock_codes'] = $c['unlock_codes'] ?: '';
    }
    
    $data['call_to'] = $c['call_to'] ?: $user['phone'];
    
    // Send FCM
    if ($c['fcm_token']) {
        $fcmResult = sendFCM($c['fcm_token'], $data);
        $results[$code] = ['sent' => true, 'fcm' => $fcmResult];
    } else {
        $results[$code] = ['sent' => false, 'error' => 'No FCM token'];
    }
}

echo json_encode([
    'success' => true,
    'command' => $command,
    'results' => $results
]);
