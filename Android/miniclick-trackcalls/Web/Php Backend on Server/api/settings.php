<?php
/* =====================================
   CallCloud Admin - Settings API
   ===================================== */

require_once '../config.php';
require_once '../utils.php';

// Set headers
header('Access-Control-Allow-Origin: ' . CORS_ALLOWED_ORIGINS);
header('Access-Control-Allow-Methods: GET, POST, PUT, OPTIONS');
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

// Handle PUT requests (updates)
if ($method === 'PUT') {
    $action = $data['action'] ?? '';

    switch ($action) {
        /* ===== UPDATE PROFILE ===== */
        case 'update_profile':
            // Allow update of name, org_name, billing_address, gst_number, state
            $fields = [];
            
            if (isset($data['name'])) {
                $fields[] = "name = '" . Database::escape($data['name']) . "'";
            }
            if (isset($data['org_name'])) {
                $fields[] = "org_name = '" . Database::escape($data['org_name']) . "'";
            }
            if (isset($data['billing_address'])) {
                $fields[] = "billing_address = '" . Database::escape($data['billing_address']) . "'";
            }
            if (isset($data['gst_number'])) {
                $fields[] = "gst_number = '" . Database::escape($data['gst_number']) . "'";
            }
            if (isset($data['state'])) {
                $fields[] = "state = '" . Database::escape($data['state']) . "'";
            }

            if (empty($fields)) {
                Response::success([], 'No changes to update');
            }

            $sql = "UPDATE users SET " . implode(', ', $fields) . ", updated_at = NOW() WHERE id = $userId";
            $result = Database::execute($sql);

            if ($result) {
                // Fetch updated user to return
                $updatedUser = Database::getOne("SELECT * FROM users WHERE id = $userId");
                unset($updatedUser['password_hash']);
                Response::success(['user' => $updatedUser], 'Profile updated successfully');
            } else {
                Response::error('Failed to update profile');
            }
            break;

        /* ===== CHANGE PASSWORD ===== */
        case 'change_password':
            Validator::required($data, ['current_password', 'new_password']);
            
            // Get current password hash
            $user = Database::getOne("SELECT password_hash FROM users WHERE id = $userId");
            
            if (!Auth::verifyPassword($data['current_password'], $user['password_hash'])) {
                Response::error('Incorrect current password');
            }
            
            $newHash = Auth::hashPassword($data['new_password']);
            
            Database::execute("UPDATE users SET password_hash = '$newHash', updated_at = NOW() WHERE id = $userId");
            
            Response::success([], 'Password changed successfully');
            break;

        default:
            Response::error('Invalid action');
    }
} else {
    Response::error('Method not allowed', 405);
}
