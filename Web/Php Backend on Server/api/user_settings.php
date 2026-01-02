<?php
/* =====================================
   CallCloud Admin - User Settings API
   ===================================== */

require_once '../config.php';
require_once '../utils.php';

// Set headers
header('Access-Control-Allow-Origin: ' . CORS_ALLOWED_ORIGINS);
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Authorization, Content-Type');
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Require authentication
$currentUser = Auth::requireAuth();
$userId = $currentUser['id'];
$orgId = $currentUser['org_id'];

$data = json_decode(file_get_contents('php://input'), true);
$method = $_SERVER['REQUEST_METHOD'];
$action = $_GET['action'] ?? '';

/* 
   We store user-specific settings in the 'settings' table.
   Since 'settings' table is keyed by (org_id, setting_key),
   we will prefix keys with "user_{userId}_" to make them unique per user.
*/

function getUserKey($key, $uid) {
    return "user_{$uid}_{$key}";
}

switch ($method) {
    
    /* ===== GET SETTINGS ===== */
    case 'GET':
        $keyParam = $_GET['key'] ?? '';
        if (!$keyParam) {
            Response::error('Key required');
        }

        $userKey = getUserKey($keyParam, $userId);
        $escapedKey = Database::escape($userKey);

        $row = Database::getOne("SELECT setting_value FROM settings WHERE org_id = '$orgId' AND setting_key = '$escapedKey'");
        
        $value = $row ? json_decode($row['setting_value'], true) : null;
        
        Response::success(['key' => $keyParam, 'value' => $value], 'Setting retrieved');
        break;

    /* ===== SAVE SETTINGS ===== */
    case 'POST':
        if (!isset($data['key']) || !isset($data['value'])) {
            Response::error('Key and Value required');
        }

        $keyParam = $data['key'];
        $userKey = getUserKey($keyParam, $userId);
        $escapedKey = Database::escape($userKey);
        
        // Encode value to JSON if it's not a string
        $val = $data['value'];
        $jsonVal = is_string($val) ? $val : json_encode($val);
        $escapedVal = Database::escape($jsonVal);

        // Check if exists
        $exists = Database::getOne("SELECT id FROM settings WHERE org_id = '$orgId' AND setting_key = '$escapedKey'");

        if ($exists) {
            $sql = "UPDATE settings SET setting_value = '$escapedVal', updated_at = NOW() WHERE id = {$exists['id']}";
        } else {
            $sql = "INSERT INTO settings (org_id, setting_key, setting_value, created_at, updated_at) 
                    VALUES ('$orgId', '$escapedKey', '$escapedVal', NOW(), NOW())";
        }

        Database::execute($sql);
        Response::success([], 'Setting saved');
        break;

    default:
        Response::error('Method not allowed', 405);
}
