<?php
/* =====================================
   CallCloud Admin - Numbers API
   ===================================== */

require_once '../config.php';
require_once '../utils.php';

// Set headers
header('Access-Control-Allow-Origin: ' . CORS_ALLOWED_ORIGINS);
header('Access-Control-Allow-Methods: GET, PUT, POST, OPTIONS');
header('Access-Control-Allow-Headers: Authorization, Content-Type');
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Require authentication
$currentUser = Auth::requireAuth();
// numbers table doesn't have org_id yet based on create stmt, or does it?
// Step 47 create stmt: `CREATE TABLE IF NOT EXISTS numbers (...)` - NO org_id.
// Wait, numbers table is "global" based on original structure? 
// The user request said: "Numbers - id,phone, person_note, ... - Data will be added here through triggers mysql".
// It implies one numbers table. 
// However, if multiple orgs use this, they might share the same number entry? 
// If Org A updates person_note, does Org B see it?
// Original user prompt: "Users - including org data... Employees... Calls... Numbers..."
// It seems intended for a single deployment or shared?
// But `calls` has `org_id`. `employees` has `org_id`.
// If `numbers` does NOT have `org_id`, it is shared or unique by phone.
// Use case: "person_note" usually is specific to the Org/User calling.
// PROBABLY `numbers` should have been org-specific or we treat it as global per installation.
// Given "triggers mysql" adds to it from calls (which has org_id), but trigger insert didn't use org_id.
// So `numbers` is effectively a global directory for this installation.
// We will proceed assuming it's shared for this instance.

// Get request data
$data = json_decode(file_get_contents('php://input'), true);
$method = $_SERVER['REQUEST_METHOD'];
$action = $_GET['action'] ?? '';
// ID or Phone can identify a number
$id = $_GET['id'] ?? null;
$phone = $_GET['phone'] ?? null;

switch ($method) {
    
    /* ===== GET NUMBERS ===== */
    case 'GET':
        if ($phone) {
            $phoneEsc = Database::escape($phone);
            $number = Database::getOne("SELECT * FROM numbers WHERE phone = '$phoneEsc'");
            if ($number) {
                 Response::success($number, 'Number found');
            } else {
                 Response::error('Number not found', 404);
            }
        } elseif ($id) {
            $number = Database::getOne("SELECT * FROM numbers WHERE id = $id");
            if ($number) {
                 Response::success($number, 'Number found');
            } else {
                 Response::error('Number not found', 404);
            }
        } else {
             // List/Search
             $search = $_GET['search'] ?? '';
             $sql = "SELECT * FROM numbers";
             if ($search) {
                 $search = Database::escape($search);
                 $sql .= " WHERE phone LIKE '%$search%' OR name LIKE '%$search%'";
             }
             $sql .= " ORDER BY lastCalltime DESC LIMIT 50";
             
             $numbers = Database::select($sql);
             Response::success($numbers, 'Numbers retrieved');
        }
        break;

    /* ===== UPDATE NUMBER (Person Note, Name, Labels) ===== */
    case 'PUT':
        // Identify by ID or Phone
        $targetId = $id;
        
        if (!$targetId && $phone) {
            $phoneEsc = Database::escape($phone);
            $numRow = Database::getOne("SELECT id FROM numbers WHERE phone = '$phoneEsc'");
            if ($numRow) $targetId = $numRow['id'];
        }

        if (!$targetId) {
             Response::error('Number ID or Phone required');
        }

        $updates = [];

        if (isset($data['person_note'])) {
            $updates[] = "person_note = '" . Database::escape($data['person_note']) . "'";
        }
        
        if (isset($data['name'])) {
            $updates[] = "name = '" . Database::escape($data['name']) . "'";
        }
        
        if (isset($data['labels'])) {
            $updates[] = "labels = '" . Database::escape($data['labels']) . "'";
        }

        if (empty($updates)) {
            Response::error('No updates provided');
        }

        $sql = "UPDATE numbers SET " . implode(', ', $updates) . " WHERE id = $targetId";
        Database::execute($sql);

        $updatedNumber = Database::getOne("SELECT * FROM numbers WHERE id = $targetId");
        Response::success($updatedNumber, 'Number updated successfully');
        break;

    default:
        Response::error('Method not allowed', 405);
}
