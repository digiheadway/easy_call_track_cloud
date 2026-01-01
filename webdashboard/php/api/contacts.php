<?php
/* =====================================
   CallCloud Admin - Contacts API
   ===================================== */

require_once '../config.php';
require_once '../utils.php';

// Set headers
header('Access-Control-Allow-Origin: ' . CORS_ALLOWED_ORIGINS);
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Authorization, Content-Type');
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Require authentication
$currentUser = Auth::requireAuth();
$orgId = $currentUser['org_id'];

// Get request data
$data = json_decode(file_get_contents('php://input'), true);
$method = $_SERVER['REQUEST_METHOD'];
$action = $_GET['action'] ?? '';
$id = $_GET['id'] ?? null;

switch ($method) {
    
    /* ===== GET CONTACTS ===== */
    case 'GET':
        $search = $_GET['search'] ?? '';
        $label = $_GET['label'] ?? '';
        
        $where = ["org_id = '$orgId'"];
        
        if ($search) {
            $cleanSearch = Validator::phone($search);
            $search = Database::escape($search);
            if ($cleanSearch && is_numeric($cleanSearch)) {
                $where[] = "(name LIKE '%$search%' OR phone LIKE '%$cleanSearch%')";
            } else {
                $where[] = "name LIKE '%$search%'";
            }
        }
        
        if ($label && $label !== 'all') {
            $label = Database::escape($label);
            $where[] = "label = '$label'";
        }
        
        $whereClause = implode(' AND ', $where);
        
        $contacts = Database::select("
            SELECT c.*, e.name as employee_name 
            FROM contacts c
            LEFT JOIN employees e ON c.employee_id = e.id
            WHERE c.$whereClause 
            ORDER BY c.name ASC 
            LIMIT 100
        ");
        
        Response::success($contacts, 'Contacts retrieved successfully');
        break;
    
    /* ===== CREATE/UPDATE CONTACT (UPSERT) ===== */
    case 'POST':
        Validator::required($data, ['phone']);
        
        $phone = Database::escape(Validator::phone($data['phone']));
        $name = isset($data['name']) ? Database::escape($data['name']) : '';
        $label = isset($data['label']) ? Database::escape($data['label']) : ''; // e.g. 'VIP', 'Spam'
        $email = isset($data['email']) ? Database::escape($data['email']) : '';
        $notes = isset($data['notes']) ? Database::escape($data['notes']) : '';
        $employee_id = isset($data['employee_id']) ? intval($data['employee_id']) : 'NULL';
        
        // Check if exists
        $existing = Database::getOne("SELECT id FROM contacts WHERE org_id = '$orgId' AND phone = '$phone'");
        
        if ($existing) {
            // Update
            $updates = [];
            if (isset($data['name'])) $updates[] = "name = '$name'";
            if (isset($data['label'])) $updates[] = "label = '$label'";
            if (isset($data['email'])) $updates[] = "email = '$email'";
            if (isset($data['notes'])) $updates[] = "notes = '$notes'";
            if (isset($data['employee_id'])) $updates[] = "employee_id = $employee_id";
            
            if (!empty($updates)) {
                $sql = "UPDATE contacts SET " . implode(', ', $updates) . " WHERE id = {$existing['id']}";
                Database::execute($sql);
            }
            $contactId = $existing['id'];
            $msg = 'Contact updated';
        } else {
            // Insert
            $sql = "INSERT INTO contacts (org_id, employee_id, phone, name, label, email, notes) 
                    VALUES ('$orgId', $employee_id, '$phone', '$name', '$label', '$email', '$notes')";
            $contactId = Database::insert($sql);
            $msg = 'Contact created';
        }
        
        $contact = Database::getOne("SELECT * FROM contacts WHERE id = $contactId");
        Response::success($contact, $msg);
        break;

    /* ===== DELETE CONTACT ===== */
    case 'DELETE':
        if (!$id) Response::error('ID required');
        
        Database::execute("DELETE FROM contacts WHERE id = $id AND org_id = '$orgId'");
        Response::success(null, 'Contact deleted');
        break;
        
    default:
        Response::error('Method not allowed', 405);
}
