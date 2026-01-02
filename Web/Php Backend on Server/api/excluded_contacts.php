<?php
/* =====================================
   CallCloud Admin - Excluded Contacts API
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
$id = $_GET['id'] ?? null;

switch ($method) {
    
    /* ===== GET ALL EXCLUDED CONTACTS ===== */
    case 'GET':
        $contacts = Database::select("
            SELECT * FROM excluded_contacts 
            WHERE org_id = '$orgId' 
            ORDER BY created_at DESC
        ");
        
        Response::success($contacts, 'Excluded contacts retrieved successfully');
        break;
    
    /* ===== CREATE EXCLUDED CONTACT ===== */
    case 'POST':
        $action = $_GET['action'] ?? null;
        
        if ($action === 'delete_all_data') {
            // Delete all data for ALL excluded contacts
            $excluded = Database::select("SELECT phone FROM excluded_contacts WHERE org_id = '$orgId'");
            if (empty($excluded)) {
                Response::success([], 'No excluded contacts to process');
            }
            
            $phones = array_map(function($c) { return "'" . Database::escape($c['phone']) . "'"; }, $excluded);
            $phoneList = implode(',', $phones);
            
            // Delete Physical Files
            $recordings = Database::select("SELECT recording_url FROM calls WHERE caller_phone IN ($phoneList) AND org_id = '$orgId' AND recording_url IS NOT NULL AND recording_url != ''");
            foreach ($recordings as $rec) {
                if ($rec['recording_url'] && file_exists($rec['recording_url'])) {
                    @unlink($rec['recording_url']);
                }
            }
            
            // 1. Delete Calls
            Database::execute("DELETE FROM calls WHERE caller_phone IN ($phoneList) AND org_id = '$orgId'");
            // 2. Delete Contact Info
            Database::execute("DELETE FROM contacts WHERE phone IN ($phoneList) AND org_id = '$orgId'");
            
            Response::success([], "All related data for " . count($excluded) . " excluded contacts deleted");
        }

        if ($action === 'delete_contact_data') {
            Validator::required($data, ['phone']);
            $phone = Database::escape($data['phone']);
            
            // Delete Physical Files
            $recordings = Database::select("SELECT recording_url FROM calls WHERE caller_phone = '$phone' AND org_id = '$orgId' AND recording_url IS NOT NULL AND recording_url != ''");
            foreach ($recordings as $rec) {
                if ($rec['recording_url'] && file_exists($rec['recording_url'])) {
                    @unlink($rec['recording_url']);
                }
            }
            
            // 1. Delete Calls
            Database::execute("DELETE FROM calls WHERE caller_phone = '$phone' AND org_id = '$orgId'");
            // 2. Delete Contact Info
            Database::execute("DELETE FROM contacts WHERE phone = '$phone' AND org_id = '$orgId'");
            
            Response::success([], 'Related data for this contact deleted');
        }

        Validator::required($data, ['phone']);
        
        $phone = Database::escape($data['phone']);
        $name = isset($data['name']) ? Database::escape($data['name']) : '';
        $isActive = isset($data['is_active']) ? (int)$data['is_active'] : 1;
        
        // Check if already exists
        $existing = Database::getOne("SELECT id FROM excluded_contacts WHERE org_id = '$orgId' AND phone = '$phone'");
        if ($existing) {
            Response::error('This contact is already excluded');
        }
        
        $sql = "INSERT INTO excluded_contacts (org_id, phone, name, is_active) 
                VALUES ('$orgId', '$phone', '$name', $isActive)";
        
        $insertId = Database::insert($sql);
        
        if (!$insertId) {
            Response::error('Failed to add excluded contact');
        }
        
        $contact = Database::getOne("SELECT * FROM excluded_contacts WHERE id = $insertId");
        Response::success($contact, 'Contact excluded successfully');
        break;
    
    /* ===== UPDATE EXCLUDED CONTACT ===== */
    case 'PUT':
        if (!$id) {
            Response::error('ID required');
        }
        
        // Check ownership
        $contact = Database::getOne("SELECT id FROM excluded_contacts WHERE id = $id AND org_id = '$orgId'");
        if (!$contact) {
            Response::error('Contact not found', 404);
        }
        
        $updates = [];
        if (isset($data['phone'])) $updates[] = "phone = '" . Database::escape($data['phone']) . "'";
        if (isset($data['name'])) $updates[] = "name = '" . Database::escape($data['name']) . "'";
        if (isset($data['is_active'])) $updates[] = "is_active = " . (int)$data['is_active'];
        
        if (empty($updates)) {
            Response::error('No fields to update');
        }
        
        $sql = "UPDATE excluded_contacts SET " . implode(', ', $updates) . " WHERE id = $id";
        Database::execute($sql);
        
        $updated = Database::getOne("SELECT * FROM excluded_contacts WHERE id = $id");
        Response::success($updated, 'Excluded contact updated successfully');
        break;
    
    /* ===== DELETE EXCLUDED CONTACT ===== */
    case 'DELETE':
        if (!$id) {
            Response::error('ID required');
        }
        
        // Check ownership
        $contact = Database::getOne("SELECT id FROM excluded_contacts WHERE id = $id AND org_id = '$orgId'");
        if (!$contact) {
            Response::error('Contact not found', 404);
        }
        
        $sql = "DELETE FROM excluded_contacts WHERE id = $id";
        Database::execute($sql);
        
        Response::success([], 'Excluded contact removed successfully');
        break;
    
    default:
        Response::error('Method not allowed', 405);
}
