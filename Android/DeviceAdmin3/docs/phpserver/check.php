<?php
/**
 * Device Check-in API
 * 
 * GET /api/check.php?pairingcode=1120&fcm_token=xxx&imei=xxx&imei2=xxx
 * 
 * Logic:
 * - Active customers: normal check-in
 * - Used customers: only allow if EITHER IMEI matches (same device reconnecting)
 */

header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=utf-8");

require_once __DIR__ . '/config/database.php';
require_once __DIR__ . '/config/firebase.php';

$pairingCode = $_REQUEST['pairingcode'] ?? $_REQUEST['pairing_code'] ?? null;
$fcmToken = $_REQUEST['fcm_token'] ?? null;
$imei1 = $_REQUEST['imei'] ?? null;
$imei2 = $_REQUEST['imei2'] ?? null;

if (!$pairingCode) {
    die(json_encode(['success' => false, 'error' => 'pairingcode is required']));
}

// Find customer by pairing code
$customer = fetchOne(
    "SELECT c.*, u.phone as manager_phone FROM customers c 
     INNER JOIN users u ON c.user_id = u.id 
     WHERE c.pairing_code = ?",
    [$pairingCode]
);

if (!$customer) {
    http_response_code(404);
    die(json_encode(['success' => false, 'error' => 'Invalid pairing code']));
}

// Check status and IMEI logic
if ($customer['status'] === 'used') {
    // If no IMEI on file, we can't verify, so block to be safe OR allow if it was never set? 
    // Let's assume strict: must match existing IMEI record.
    
    $storedImeis = array_filter([$customer['imei'], $customer['imei2']]);
    $incomingImeis = array_filter([$imei1, $imei2]);
    
    $match = false;
    
    if (empty($storedImeis)) {
        // Edge case: Customer marked used but no IMEI was ever recorded. 
        // Decide: Block reuse to prevent hijacking? OR Allow?
        // Safe bet: Block reuse. "Cannot verify device identity"
        $match = false;
    } else {
        // Check for intersection
        foreach ($incomingImeis as $inc) {
            if (in_array($inc, $storedImeis)) {
                $match = true;
                break;
            }
        }
    }
    
    if (!$match) {
        http_response_code(403);
        die(json_encode([
            'success' => false, 
            'error' => 'This pairing code is no longer active', 
            'status' => 'used'
        ]));
    }
} else {
    // Status is 'active'. 
    // PREVENT HIJACKING: Check if this IMEI is already linked to ANOTHER active customer
    if ($imei1 || $imei2) {
        $imeiCheck = fetchOne(
            "SELECT id, pairing_code FROM customers 
             WHERE (imei = ? OR imei2 = ?) 
             AND status = 'active' 
             AND id != ? LIMIT 1",
            [$imei1 ?: '', $imei2 ?: '', $customer['id']]
        );
        
        if ($imeiCheck) {
            http_response_code(403);
            die(json_encode([
                'success' => false,
                'error' => "Device already linked to another customer (Code: {$imeiCheck['pairing_code']}). Delete old customer first."
            ]));
        }
    }
}

// Update device info
$updates = ['last_seen_at = NOW()'];
$params = [];

if ($fcmToken) {
    $updates[] = 'fcm_token = ?';
    $params[] = $fcmToken;
}

// Update IMEIs if provided
if ($imei1) {
    $updates[] = 'imei = ?';
    $params[] = $imei1;
}
if ($imei2) {
    $updates[] = 'imei2 = ?';
    $params[] = $imei2;
}

// Other fields
foreach (['device_name', 'device_model', 'installed_apps'] as $field) {
    $value = $_REQUEST[$field] ?? null;
    if ($value) {
        $updates[] = "$field = ?";
        $params[] = $value;
    }
}

$params[] = $customer['id'];
query("UPDATE customers SET " . implode(', ', $updates) . " WHERE id = ?", $params);

// Return status
echo json_encode([
    'success' => true,
    'pairingcode' => $pairingCode,
    'status' => $customer['status'],
    'data' => [
        'amount' => (float)$customer['pending_amount'],
        'message' => $customer['freeze_message'],
        'is_freezed' => (bool)$customer['is_freezed'],
        'call_to' => $customer['call_to'] ?: $customer['manager_phone'],
        'is_protected' => (bool)$customer['is_protected'],
        'unlock_codes' => $customer['unlock_codes'] ? array_map('trim', explode(',', $customer['unlock_codes'])) : []
    ],
    'time' => date('Y-m-d H:i:s'),
    'token_registered' => (bool)$fcmToken
]);
