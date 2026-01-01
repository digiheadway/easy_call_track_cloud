<?php
/* =====================================
   CallCloud Admin - Calls API
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
$orgId = $currentUser['org_id'];

// Get request data
$data = json_decode(file_get_contents('php://input'), true);
$method = $_SERVER['REQUEST_METHOD'];
$action = $_GET['action'] ?? '';
$id = $_GET['id'] ?? null;

switch ($method) {
    
    /* ===== GET CALLS ===== */
    case 'GET':
        if ($action === 'stats') {
            // Get call statistics
            $stats = Database::getOne("
                SELECT 
                    COUNT(*) as total,
                    SUM(CASE WHEN type = 'Incoming' THEN 1 ELSE 0 END) as inbound,
                    SUM(CASE WHEN type = 'Outgoing' THEN 1 ELSE 0 END) as outbound,
                    SUM(CASE WHEN duration = 0 THEN 1 ELSE 0 END) as missed,
                    AVG(NULLIF(duration, 0)) as avg_duration
                FROM calls 
                WHERE org_id = '$orgId'
            ");
            
            // Format average duration back to MM:SS
            $avg = $stats['avg_duration'] ?? 0;
            $minutes = floor($avg / 60);
            $seconds = $avg % 60;
            $stats['avg_duration_formatted'] = sprintf('%d:%02d', $minutes, $seconds);
            
            Response::success($stats, 'Call statistics retrieved');
        } else {
            // Get filters
            $direction = $_GET['direction'] ?? ''; // Maps to type
            $dateFilter = $_GET['dateFilter'] ?? 'all';
            $search = $_GET['search'] ?? '';
            $contactLabel = $_GET['contactLabel'] ?? '';
            
            // Build query
            $where = ["c.org_id = '$orgId'"];
            
            if ($direction && $direction !== 'all') {
                $dirMap = [
                    'inbound' => 'Incoming',
                    'outbound' => 'Outgoing',
                    'missed' => 'Missed' 
                ];
                $dbType = $dirMap[$direction] ?? $direction;
                $where[] = "c.type = '" . Database::escape($dbType) . "'";
            }
            
            switch ($dateFilter) {
                case 'today':
                    $where[] = "DATE(c.call_time) = CURDATE()";
                    break;
                case 'week':
                    $where[] = "c.call_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)";
                    break;
                case 'month':
                    $where[] = "c.call_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
                    break;
            }
            
            if ($search) {
                $search = Database::escape($search);
                // Search in call log names/phones OR in saved contact names
                $where[] = "(c.caller_name LIKE '%$search%' OR c.caller_phone LIKE '%$search%' OR co.name LIKE '%$search%')";
            }
            
            if ($contactLabel) {
                 $contactLabel = Database::escape($contactLabel);
                 if ($contactLabel === 'uncategorized') {
                     $where[] = "co.label IS NULL";
                 } else {
                     $where[] = "co.label = '$contactLabel'";
                 }
            }
            
            $whereClause = implode(' AND ', $where);
            
            $calls = Database::select("
                SELECT 
                    c.*, 
                    c.caller_name as contact_name,
                    c.caller_phone as phone_number,
                    e.name as employee_name,
                    co.label as contact_label,
                    co.name as saved_contact_name
                FROM calls c
                LEFT JOIN employees e ON c.employee_id = e.id
                LEFT JOIN contacts co ON c.caller_phone = co.phone AND c.org_id = co.org_id
                WHERE $whereClause 
                ORDER BY c.call_time DESC
                LIMIT 100
            ");
            
            Response::success($calls, 'Calls retrieved successfully');
        }
        break;
    
    /* ===== CREATE CALL LOG ===== */
    case 'POST':
        Validator::required($data, ['employee_id', 'phone_number', 'type']);
        
        $employeeId = (int)$data['employee_id'];
        $callerName = Database::escape($data['contact_name'] ?? '');
        $callerPhone = Database::escape($data['phone_number']);
        $type = Database::escape($data['type']); // Incoming, Outgoing, Missed
        $duration = (int)($data['duration'] ?? 0);
        $note = Database::escape($data['note'] ?? '');
        $recordingUrl = Database::escape($data['recording_url'] ?? '');
        $callTime = isset($data['call_time']) ? Database::escape($data['call_time']) : date('Y-m-d H:i:s');
        
        $employee = Database::getOne("SELECT id, name FROM employees WHERE id = $employeeId AND org_id = '$orgId'");
        if (!$employee) {
            Response::error('Invalid employee ID');
        }
        
        $sql = "INSERT INTO calls (
                    org_id, 
                    employee_id, 
                    caller_name, 
                    caller_phone, 
                    type, 
                    duration, 
                    call_time, 
                    note, 
                    recording_url
                ) VALUES (
                    '$orgId', 
                    $employeeId, 
                    '$callerName', 
                    '$callerPhone', 
                    '$type', 
                    $duration, 
                    '$callTime', 
                    '$note', 
                    '$recordingUrl'
                )";
        
        $callId = Database::insert($sql);
        
        if (!$callId) {
            Response::error('Failed to create call log');
        }
        
        Database::execute("UPDATE employees SET calls_today = calls_today + 1 WHERE id = $employeeId");
        
        $call = Database::getOne("SELECT * FROM calls WHERE id = $callId");
        
        Response::success($call, 'Call logged successfully');
        break;

    /* ===== UPDATE CALL LOG (e.g. Note) ===== */
    case 'PUT':
        if (!$id) {
            Response::error('Call ID required');
        }

        // Verify call belongs to Org
        $call = Database::getOne("SELECT id FROM calls WHERE id = $id AND org_id = '$orgId'");
        if (!$call) {
            Response::error('Call not found', 404);
        }

        $updates = [];

        // Allow updating Note
        if (isset($data['note'])) {
            $updates[] = "note = '" . Database::escape($data['note']) . "'";
        }
        
        // Allow updating caller name if changed
        if (isset($data['contact_name'])) {
            $updates[] = "caller_name = '" . Database::escape($data['contact_name']) . "'";
        }
        
        // Allow updating recording (if attaching later)
        if (isset($data['recording_url'])) {
            $updates[] = "recording_url = '" . Database::escape($data['recording_url']) . "'";
        }

        if (empty($updates)) {
            Response::error('No fields to update');
        }

        $sql = "UPDATE calls SET " . implode(', ', $updates) . " WHERE id = $id";
        Database::execute($sql);

        $updatedCall = Database::getOne("SELECT * FROM calls WHERE id = $id");
        Response::success($updatedCall, 'Call updated successfully');
        break;
    
    default:
        Response::error('Method not allowed', 405);
}
