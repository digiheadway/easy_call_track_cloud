<?php
/* =====================================
   CallCloud Admin - Employees API
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
    
    /* ===== GET ALL EMPLOYEES ===== */
    case 'GET':
        if ($action === 'stats') {
            // Get employee statistics
            $stats = Database::getOne("
                SELECT 
                    COUNT(*) as total,
                    SUM(CASE WHEN status = 'active' THEN 1 ELSE 0 END) as active,
                    SUM(calls_today) as calls_today
                FROM employees 
                WHERE org_id = '$orgId'
            ");
            
            Response::success($stats, 'Employee statistics retrieved');
        } else {
            // Get all employees
            $employees = Database::select("
                SELECT * FROM employees 
                WHERE org_id = '$orgId' 
                ORDER BY created_at DESC
            ");
            
            Response::success($employees, 'Employees retrieved successfully');
        }
        break;
    
    /* ===== CREATE EMPLOYEE ===== */
    case 'POST':
        // Validate required fields
        Validator::required($data, ['name', 'email', 'phone', 'department', 'role']);
        
        $name = Database::escape($data['name']);
        $email = Database::escape($data['email']);
        $phone = Database::escape($data['phone']);
        $department = Database::escape($data['department']);
        $role = Database::escape($data['role']);
        $joinDate = date('Y-m-d');
        
        // Optional fields
        $callTrack = isset($data['call_track']) ? (int)$data['call_track'] : 1;
        $callRecordCrm = isset($data['call_record_crm']) ? (int)$data['call_record_crm'] : 1;
        $expiryDate = isset($data['expiry_date']) ? "'" . Database::escape($data['expiry_date']) . "'" : "NULL";
        
        $sql = "INSERT INTO employees (org_id, name, email, phone, department, role, join_date, status, call_track, call_record_crm, expiry_date) 
                VALUES ('$orgId', '$name', '$email', '$phone', '$department', '$role', '$joinDate', 'active', $callTrack, $callRecordCrm, $expiryDate)";
        
        $employeeId = Database::insert($sql);
        
        if (!$employeeId) {
            Response::error('Failed to create employee');
        }
        
        $employee = Database::getOne("SELECT * FROM employees WHERE id = $employeeId");
        
        Response::success($employee, 'Employee created successfully');
        break;
    
    /* ===== UPDATE EMPLOYEE ===== */
    case 'PUT':
        if (!$id) {
            Response::error('Employee ID required');
        }
        
        // Check if employee belongs to organization
        $employee = Database::getOne("SELECT id FROM employees WHERE id = $id AND org_id = '$orgId'");
        if (!$employee) {
            Response::error('Employee not found', 404);
        }
        
        // Build update query
        $updates = [];
        if (isset($data['name'])) $updates[] = "name = '" . Database::escape($data['name']) . "'";
        if (isset($data['email'])) $updates[] = "email = '" . Database::escape($data['email']) . "'";
        if (isset($data['phone'])) $updates[] = "phone = '" . Database::escape($data['phone']) . "'";
        if (isset($data['department'])) $updates[] = "department = '" . Database::escape($data['department']) . "'";
        if (isset($data['role'])) $updates[] = "role = '" . Database::escape($data['role']) . "'";
        if (isset($data['status'])) $updates[] = "status = '" . Database::escape($data['status']) . "'";
        
        // Update new fields
        if (isset($data['call_track'])) $updates[] = "call_track = " . (int)$data['call_track'];
        if (isset($data['call_record_crm'])) $updates[] = "call_record_crm = " . (int)$data['call_record_crm'];
        if (isset($data['expiry_date'])) {
             $ed = Database::escape($data['expiry_date']);
             $updates[] = "expiry_date = " . ($ed ? "'$ed'" : "NULL");
        }
        if (isset($data['last_sync'])) $updates[] = "last_sync = '" . Database::escape($data['last_sync']) . "'";
        
        if (empty($updates)) {
            Response::error('No fields to update');
        }
        
        $sql = "UPDATE employees SET " . implode(', ', $updates) . " WHERE id = $id";
        Database::execute($sql);
        
        $updatedEmployee = Database::getOne("SELECT * FROM employees WHERE id = $id");
        
        Response::success($updatedEmployee, 'Employee updated successfully');
        break;
    
    /* ===== DELETE EMPLOYEE ===== */
    case 'DELETE':
        if (!$id) {
            Response::error('Employee ID required');
        }
        
        // Check if employee belongs to organization
        $employee = Database::getOne("SELECT id FROM employees WHERE id = $id AND org_id = '$orgId'");
        if (!$employee) {
            Response::error('Employee not found', 404);
        }
        
        $sql = "DELETE FROM employees WHERE id = $id";
        Database::execute($sql);
        
        Response::success([], 'Employee deleted successfully');
        break;
    
    default:
        Response::error('Method not allowed', 405);
}
