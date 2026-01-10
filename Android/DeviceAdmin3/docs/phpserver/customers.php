<?php
/**
 * Customers API (Simple Version)
 * 
 * GET    /customers.php?api_key=xxx              - List all
 * GET    /customers.php?id=1&api_key=xxx         - Get one
 * POST   /customers.php                          - Create
 * PUT    /customers.php?id=1                     - Update
 * DELETE /customers.php?id=1                     - Delete
 */

header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE");
header("Content-Type: application/json; charset=utf-8");

require_once __DIR__ . '/config/database.php';

// Auth
$apiKey = $_SERVER['HTTP_X_API_KEY'] ?? $_GET['api_key'] ?? $_POST['api_key'] ?? null;
if (!$apiKey) die(json_encode(['success' => false, 'error' => 'API key required']));

$user = fetchOne("SELECT * FROM users WHERE api_key = ? AND is_active = 1", [$apiKey]);
if (!$user) die(json_encode(['success' => false, 'error' => 'Invalid API key']));

$method = $_SERVER['REQUEST_METHOD'];
$id = $_GET['id'] ?? null;

// ============ LIST / GET ============
if ($method === 'GET') {
    if ($id) {
        $c = fetchOne("SELECT * FROM customers WHERE id = ? AND user_id = ?", [$id, $user['id']]);
        if (!$c) die(json_encode(['success' => false, 'error' => 'Not found']));
        echo json_encode(['success' => true, 'customer' => $c]);
    } else {
        $customers = fetchAll("SELECT * FROM customers WHERE user_id = ? AND status = 'active' ORDER BY created_at DESC", [$user['id']]);
        echo json_encode(['success' => true, 'customers' => $customers]);
    }
    exit;
}

// ============ CREATE ============
if ($method === 'POST') {
    $data = json_decode(file_get_contents('php://input'), true) ?: $_POST;
    
    if (empty($data['name']) || empty($data['phone'])) {
        die(json_encode(['success' => false, 'error' => 'name and phone required']));
    }
    
    // Generate pairing code
    // Generate temporary pairing code
    $tempCode = 'TEMP_' . uniqid();
    
    query("INSERT INTO customers (user_id, name, phone, email, address, loan_amount, pending_amount, 
           device_name, is_freezed, is_protected, freeze_message, call_to, pairing_code) 
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", [
        $user['id'],
        $data['name'],
        $data['phone'],
        $data['email'] ?? null,
        $data['address'] ?? null,
        $data['loan_amount'] ?? 0,
        $data['pending_amount'] ?? 0,
        $data['device_name'] ?? null,
        $data['is_freezed'] ?? 0,
        $data['is_protected'] ?? 1,
        $data['freeze_message'] ?? 'Device Locked - Contact Manager',
        $data['call_to'] ?? $user['phone'],
        $tempCode
    ]);
    
    $id = db()->lastInsertId();
    $code = "U{$user['id']}C{$id}"; // U1C5
    query("UPDATE customers SET pairing_code = ? WHERE id = ?", [$code, $id]);
    
    echo json_encode(['success' => true, 'id' => $id, 'pairing_code' => $code]);
    exit;
}

// ============ UPDATE ============
if ($method === 'PUT') {
    if (!$id) die(json_encode(['success' => false, 'error' => 'id required']));
    
    $c = fetchOne("SELECT * FROM customers WHERE id = ? AND user_id = ?", [$id, $user['id']]);
    if (!$c) die(json_encode(['success' => false, 'error' => 'Not found']));
    
    $data = json_decode(file_get_contents('php://input'), true) ?: [];
    $fields = ['name', 'phone', 'email', 'address', 'loan_amount', 'pending_amount', 
               'device_name', 'imei', 'imei2', 'is_freezed', 'is_protected', 'freeze_message', 'call_to'];
    
    $updates = [];
    $params = [];
    foreach ($fields as $f) {
        if (isset($data[$f])) {
            $updates[] = "$f = ?";
            $params[] = $data[$f];
        }
    }
    
    if ($updates) {
        $params[] = $id;
        query("UPDATE customers SET " . implode(', ', $updates) . " WHERE id = ?", $params);
        
        // Push to device if FCM token exists
        if ($c['fcm_token']) {
            require_once __DIR__ . '/config/firebase.php';
            sendFCM($c['fcm_token'], [
                'command' => 'SYNC',
                'is_freezed' => ($data['is_freezed'] ?? $c['is_freezed']) ? '1' : '0',
                'is_protected' => ($data['is_protected'] ?? $c['is_protected']) ? '1' : '0',
                'message' => $data['freeze_message'] ?? $c['freeze_message'],
                'amount' => (string)($data['pending_amount'] ?? $c['pending_amount']),
                'call_to' => $data['call_to'] ?? $c['call_to'] ?? $user['phone']
            ]);
        }
    }
    
    echo json_encode(['success' => true]);
    exit;
}

// ============ DELETE ============
if ($method === 'DELETE') {
    if (!$id) die(json_encode(['success' => false, 'error' => 'id required']));
    
    query("UPDATE customers SET status = 'used' WHERE id = ? AND user_id = ?", [$id, $user['id']]);
    echo json_encode(['success' => true]);
    exit;
}
