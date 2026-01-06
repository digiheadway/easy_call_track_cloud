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
                    SUM(CASE WHEN status = 'active' THEN 1 ELSE 0 END) as active
                FROM employees 
                WHERE org_id = '$orgId'
            ");
            
            Response::success($stats, 'Employee statistics retrieved');
        } elseif ($action === 'data_stats' && $id) {
            // Get employee data statistics for deletion flow
            $employee = Database::getOne("SELECT id FROM employees WHERE id = $id AND org_id = '$orgId'");
            if (!$employee) {
                Response::error('Employee not found', 404);
            }
            
            // Get calls count and contacts count
            $callsStats = Database::getOne("
                SELECT 
                    COUNT(*) as calls_count,
                    COUNT(CASE WHEN recording_url IS NOT NULL AND recording_url != '' THEN 1 END) as recordings_count
                FROM calls 
                WHERE employee_id = '$id' AND org_id = '$orgId'
            ");
            
            $contactsCount = Database::getOne("
                SELECT COUNT(*) as count FROM contacts 
                WHERE employee_id = $id AND org_id = '$orgId'
            ")['count'] ?? 0;
            
            // Calculate recordings size
            $recordings = Database::select("
                SELECT recording_url FROM calls 
                WHERE employee_id = '$id' AND org_id = '$orgId' 
                AND recording_url IS NOT NULL AND recording_url != ''
            ");
            
            $totalSize = 0;
            foreach ($recordings as $rec) {
                $filePath = $rec['recording_url'];
                // Handle both URL and local paths
                if (strpos($filePath, 'http') === 0) {
                    // Convert URL to local path
                    $filePath = str_replace(BASE_URL . '/', '../', $filePath);
                }
                if (file_exists($filePath)) {
                    $totalSize += filesize($filePath);
                }
            }
            
            Response::success([
                'calls_count' => (int)($callsStats['calls_count'] ?? 0),
                'recordings_count' => (int)($callsStats['recordings_count'] ?? 0),
                'recordings_size_bytes' => $totalSize,
                'contacts_count' => (int)$contactsCount
            ], 'Employee data stats retrieved');
        } elseif ($action === 'delete_calls' && $id) {
            // Delete all calls and contacts for this employee
            $employee = Database::getOne("SELECT id FROM employees WHERE id = $id AND org_id = '$orgId'");
            if (!$employee) {
                Response::error('Employee not found', 404);
            }
            
            // Delete calls (without recordings - those are handled separately)
            $callsDeleted = Database::execute("DELETE FROM calls WHERE employee_id = '$id' AND org_id = '$orgId' AND (recording_url IS NULL OR recording_url = '')");
            
            // Delete contacts linked to this employee
            $contactsDeleted = Database::execute("DELETE FROM contacts WHERE employee_id = $id AND org_id = '$orgId'");
            
            Response::success([
                'calls_deleted' => true,
                'contacts_deleted' => true
            ], 'Calls and contacts deleted successfully');
        } elseif ($action === 'delete_recordings' && $id) {
            // Delete recordings files and update calls
            $employee = Database::getOne("SELECT id FROM employees WHERE id = $id AND org_id = '$orgId'");
            if (!$employee) {
                Response::error('Employee not found', 404);
            }
            
            // Get all recordings
            $recordings = Database::select("
                SELECT id, recording_url FROM calls 
                WHERE employee_id = '$id' AND org_id = '$orgId' 
                AND recording_url IS NOT NULL AND recording_url != ''
            ");
            
            $deletedCount = 0;
            $freedBytes = 0;
            
            foreach ($recordings as $rec) {
                $filePath = $rec['recording_url'];
                // Handle both URL and local paths
                if (strpos($filePath, 'http') === 0) {
                    $filePath = str_replace(BASE_URL . '/', '../', $filePath);
                }
                if (file_exists($filePath)) {
                    $freedBytes += filesize($filePath);
                    @unlink($filePath);
                    $deletedCount++;
                }
            }
            
            // Now clear the recording URL from calls instead of deleting the call log
            Database::execute("UPDATE calls SET recording_url = NULL WHERE employee_id = '$id' AND org_id = '$orgId' AND recording_url IS NOT NULL AND recording_url != ''");
            
            Response::success([
                'recordings_deleted' => $deletedCount,
                'bytes_freed' => $freedBytes
            ], 'Recordings deleted successfully');
        } elseif ($action === 'archive' && $id) {
            // Archive employee (set status to inactive)
            $employee = Database::getOne("SELECT id FROM employees WHERE id = $id AND org_id = '$orgId'");
            if (!$employee) {
                Response::error('Employee not found', 404);
            }
            
            Database::execute("UPDATE employees SET status = 'inactive' WHERE id = $id");
            
            Response::success(['archived' => true], 'Employee archived successfully');
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
        // Check organization limits (only count active employees)
        $userStats = Database::getOne("SELECT allowed_users_count FROM users WHERE id = " . $currentUser['id']);
        $currentCount = Database::getOne("SELECT COUNT(*) as count FROM employees WHERE org_id = '$orgId' AND status = 'active'")['count'];
        
        $allowed = (int)($userStats['allowed_users_count'] ?? 0);
        if ($currentCount >= $allowed) {
            Response::error("Employee limit reached ($allowed). Please upgrade your plan.");
        }

        // Validate required fields
        Validator::required($data, ['name']);
        
        $name = Database::escape($data['name']);
        $phone = Database::escape($data['phone'] ?? '');
        $joinDate = date('Y-m-d');
        
        // Optional fields
        $callTrack = isset($data['track_calls']) ? (int)$data['track_calls'] : 1;
        $callRecordCrm = isset($data['track_recordings']) ? (int)$data['track_recordings'] : 1;
        
        // New settings
        $allowPersonalExclusion = isset($data['allow_personal_exclusion']) ? (int)$data['allow_personal_exclusion'] : 0;
        $allowChangeStart = isset($data['allow_changing_tracking_start_date']) ? (int)$data['allow_changing_tracking_start_date'] : 0;
        $allowUpdateSims = isset($data['allow_updating_tracking_sims']) ? (int)$data['allow_updating_tracking_sims'] : 0;
        $defaultStartDate = isset($data['default_tracking_starting_date']) && !empty($data['default_tracking_starting_date']) ? "'" . Database::escape($data['default_tracking_starting_date']) . "'" : "NULL";
        
        $sql = "INSERT INTO employees (org_id, name, phone, join_date, status, call_track, call_record_crm,
                    allow_personal_exclusion, allow_changing_tracking_start_date, allow_updating_tracking_sims, default_tracking_starting_date) 
                VALUES ('$orgId', '$name', '$phone', '$joinDate', 'active', $callTrack, $callRecordCrm,
                    $allowPersonalExclusion, $allowChangeStart, $allowUpdateSims, $defaultStartDate)";
        
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
        $employee = Database::getOne("SELECT status FROM employees WHERE id = $id AND org_id = '$orgId'");
        if (!$employee) {
            Response::error('Employee not found', 404);
        }
        
        // Build update query
        $updates = [];
        if (isset($data['name'])) $updates[] = "name = '" . Database::escape($data['name']) . "'";
        if (isset($data['phone'])) $updates[] = "phone = '" . Database::escape($data['phone']) . "'";
        
        if (isset($data['status'])) {
            $newStatus = $data['status'];
            // If activating, check limit
            if ($newStatus === 'active' && $employee['status'] !== 'active') {
                $userStats = Database::getOne("SELECT allowed_users_count FROM users WHERE id = " . $currentUser['id']);
                $currentCount = Database::getOne("SELECT COUNT(*) as count FROM employees WHERE org_id = '$orgId' AND status = 'active'")['count'];
                $allowed = (int)($userStats['allowed_users_count'] ?? 0);
                if ($currentCount >= $allowed) {
                    Response::error("Cannot activate. Employee limit reached ($allowed). Please upgrade your plan.");
                }
            }
            $updates[] = "status = '" . Database::escape($newStatus) . "'";
        }
        
        // Update fields
        if (isset($data['track_calls'])) $updates[] = "call_track = " . (int)$data['track_calls'];
        
        if (isset($data['track_recordings'])) {
            $isEnabling = (int)$data['track_recordings'] === 1;
            if ($isEnabling) {
                $userStats = Database::getOne("SELECT allowed_storage_gb FROM users WHERE id = " . $currentUser['id']);
                $allowedStorage = (float)($userStats['allowed_storage_gb'] ?? 0);
                if ($allowedStorage <= 0) {
                    Response::error("No storage space available to enable recordings. Please upgrade your storage plan.");
                }
            }
            $updates[] = "call_record_crm = " . (int)$data['track_recordings'];
        }
        if (isset($data['last_sync'])) $updates[] = "last_sync = '" . Database::escape($data['last_sync']) . "'";
        
        if (isset($data['allow_personal_exclusion'])) $updates[] = "allow_personal_exclusion = " . (int)$data['allow_personal_exclusion'];
        if (isset($data['allow_changing_tracking_start_date'])) $updates[] = "allow_changing_tracking_start_date = " . (int)$data['allow_changing_tracking_start_date'];
        if (isset($data['allow_updating_tracking_sims'])) $updates[] = "allow_updating_tracking_sims = " . (int)$data['allow_updating_tracking_sims'];
        if (isset($data['default_tracking_starting_date'])) {
             $dt = Database::escape($data['default_tracking_starting_date']);
             $updates[] = "default_tracking_starting_date = " . ($dt ? "'$dt'" : "NULL");
        }
        
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

        // Check for existing data
        $callsCount = Database::getOne("SELECT COUNT(*) as count FROM calls WHERE employee_id = $id AND org_id = '$orgId'")['count'];
        $contactsCount = Database::getOne("SELECT COUNT(*) as count FROM contacts WHERE employee_id = $id AND org_id = '$orgId'")['count'];

        if ($callsCount > 0 || $contactsCount > 0) {
            Response::error('Cannot delete employee. Associated calls and contacts must be deleted first.', 400);
        }
        
        $sql = "DELETE FROM employees WHERE id = $id";
        Database::execute($sql);
        
        Response::success([], 'Employee deleted successfully');
        break;
    
    default:
        Response::error('Method not allowed', 405);
}
