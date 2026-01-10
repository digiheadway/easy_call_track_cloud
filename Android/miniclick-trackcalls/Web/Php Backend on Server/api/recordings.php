<?php
/* =====================================
   CallCloud Admin - Recordings API
   ===================================== */

require_once '../config.php';
require_once '../utils.php';

// Set headers
header('Access-Control-Allow-Origin: ' . CORS_ALLOWED_ORIGINS);
header('Access-Control-Allow-Methods: GET, POST, DELETE, OPTIONS');
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
    
    /* ===== GET RECORDINGS ===== */
    case 'GET':
        if ($action === 'stats') {
            // Get recording statistics from calls table
            $stats = Database::getOne("
                SELECT 
                    COUNT(*) as total,
                    SUM(CASE WHEN recording_url IS NOT NULL AND recording_url != '' THEN TIME_TO_SEC(duration) ELSE 0 END) as total_duration_seconds
                FROM call_log 
                WHERE org_id = '$orgId' AND recording_url IS NOT NULL AND recording_url != ''
            ");
            
            // Format total duration to HH:MM:SS
            $totalSecs = $stats['total_duration_seconds'] ?? 0;
            $hours = floor($totalSecs / 3600);
            $minutes = floor(($totalSecs % 3600) / 60);
            $seconds = $totalSecs % 60;
            $stats['total_duration_formatted'] = sprintf('%02d:%02d:%02d', $hours, $minutes, $seconds);
            
            // File size tracking is no longer in DB, so we omit 'total_size_mb' or return 0
            $stats['total_size_mb'] = 0; 
            
            Response::success($stats, 'Recording statistics retrieved');
        } else {
            // Get search parameter
            $search = $_GET['search'] ?? '';
            
            // Build query
            $where = ["org_id = '$orgId'", "recording_url IS NOT NULL", "recording_url != ''"];
            
            if ($search) {
                $search = Database::escape($search);
                // caller_name -> contact_name in old API response structure?
                $where[] = "(caller_name LIKE '%$search%' OR caller_phone LIKE '%$search%')";
            }
            
            $whereClause = implode(' AND ', $where);
            
            // Map calls fields to resemble old recordings structure if needed, or just return calls
            $recordings = Database::select("
                SELECT 
                    c.id, c.id as call_id, c.employee_id, 
                    COALESCE(c.caller_name, 'Unknown') as contact_name, 
                    c.caller_phone,
                    c.duration,
                    c.recording_url as file_path,
                    c.call_time as recording_timestamp,
                    e.name as employee_name,
                    CONCAT('Call with ', COALESCE(c.caller_name, c.caller_phone)) as title
                FROM call_log c
                LEFT JOIN employees e ON c.employee_id = e.id
                WHERE $whereClause 
                ORDER BY c.call_time DESC
                LIMIT 50
            ");
            
            Response::success($recordings, 'Recordings retrieved successfully');
        }
        break;
    
    /* ===== CREATE RECORDING (Attach to Call) ===== */
    case 'POST':
        // Expects `call_id` and `file_path` (recording_url)
        Validator::required($data, ['call_id', 'file_path']);
        
        $callId = (int)$data['call_id'];
        $recordingUrl = Database::escape($data['file_path']);
        
        // Verify call belongs to organisation
        $call = Database::getOne("SELECT id FROM call_log WHERE id = $callId AND org_id = '$orgId'");
        if (!$call) {
            Response::error('Invalid call ID');
        }
        
        Database::execute("UPDATE call_log SET recording_url = '$recordingUrl' WHERE id = $callId");
        
        $updatedCall = Database::getOne("SELECT * FROM call_log WHERE id = $callId");
        
        Response::success($updatedCall, 'Recording attached successfully');
        break;
    
    /* ===== DELETE RECORDING ===== */
    case 'DELETE':
        if (!$id) { // This `id` is actually the CALL ID now since 1:1 mapping
            Response::error('Recording ID (Call ID) required');
        }
        
        // Check if call belongs to organization
        $call = Database::getOne("SELECT id, recording_url FROM call_log WHERE id = $id AND org_id = '$orgId'");
        if (!$call) {
            Response::error('Recording not found', 404);
        }
        
        // Delete physical file if possible (local fs)
        // Note: $call['recording_url'] might be an absolute URL or local path. 
        // If local path, we can unlink. 
        if ($call['recording_url'] && file_exists($call['recording_url'])) {
             @unlink($call['recording_url']);
        }
        
        $sql = "UPDATE call_log SET recording_url = NULL WHERE id = $id";
        Database::execute($sql);
        
        Response::success([], 'Recording deleted successfully');
        break;
    
    default:
        Response::error('Method not allowed', 405);
}
