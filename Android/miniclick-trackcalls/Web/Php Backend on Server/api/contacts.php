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
        // Return unique callers with stats
        if ($action === 'callers') {
            $search = $_GET['search'] ?? '';
            $label = $_GET['label'] ?? '';
            $page = isset($_GET['page']) ? (int)$_GET['page'] : 1;
            $limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 50;
            $offset = ($page - 1) * $limit;

            $dateFilter = $_GET['dateRange'] ?? 'all';
            $timezoneOffset = isset($_GET['tzOffset']) ? (int)$_GET['tzOffset'] : 0; 
            $dbOffsetMinutes = -1 * $timezoneOffset;
            $sqlLocalTime = "DATE_ADD(c.call_time, INTERVAL $dbOffsetMinutes MINUTE)";
            $sqlLocalNow = "DATE_ADD(UTC_TIMESTAMP(), INTERVAL $dbOffsetMinutes MINUTE)";

            $where = ["c.org_id = '$orgId'"];
            
            // Build WHERE Clause (Basic filters)
            $where = ["c.org_id = '$orgId'"];
            
            // Date Filter
            $dateClause = "";
            switch ($dateFilter) {
                case 'today': $dateClause = "DATE($sqlLocalTime) = DATE($sqlLocalNow)"; break;
                case 'yesterday': $dateClause = "DATE($sqlLocalTime) = DATE(DATE_SUB($sqlLocalNow, INTERVAL 1 DAY))"; break;
                case '3days': $dateClause = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 3 DAY)"; break;
                case '7days': $dateClause = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 7 DAY)"; break;
                case '14days': $dateClause = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 14 DAY)"; break;
                case '30days': $dateClause = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 30 DAY)"; break;
                case 'this_month': $dateClause = "DATE_FORMAT($sqlLocalTime, '%Y-%m') = DATE_FORMAT($sqlLocalNow, '%Y-%m')"; break;
                case 'last_month': $dateClause = "DATE_FORMAT($sqlLocalTime, '%Y-%m') = DATE_FORMAT(DATE_SUB($sqlLocalNow, INTERVAL 1 MONTH), '%Y-%m')"; break;
                case 'custom':
                    if (isset($_GET['startDate']) && isset($_GET['endDate'])) {
                        $s = Database::escape($_GET['startDate']);
                        $e = Database::escape($_GET['endDate']);
                        $dateClause = "DATE($sqlLocalTime) BETWEEN '$s' AND '$e'";
                    }
                    break;
            }
            if ($dateClause) $where[] = $dateClause;

            if ($search) {
                $search = Database::escape($search);
                $where[] = "(c.caller_phone LIKE '%$search%' OR c.caller_name LIKE '%$search%' OR co.name LIKE '%$search%')";
            }
            if ($label && $label !== 'all') {
                $label = Database::escape($label);
                $where[] = "co.label LIKE '%$label%'";
            }

            // Simple status filters (at WHERE level if possible)
            $noteStatus = $_GET['noteStatus'] ?? 'all';
            if ($noteStatus === 'has_note') $where[] = "(co.notes IS NOT NULL AND co.notes != '')";
            if ($noteStatus === 'no_note') $where[] = "(co.notes IS NULL OR co.notes = '')";

            $whereClause = implode(' AND ', $where);

            // HAVING Clause (Filters on Aggregates)
            $having = [];
            
            $connectStatus = $_GET['connectStatus'] ?? 'all';
            if ($connectStatus === 'connected') $having[] = "connected_calls > 0";
            if ($connectStatus === 'never_connected') $having[] = "connected_calls = 0";

            $reviewStatus = $_GET['reviewStatus'] ?? 'all';
            if ($reviewStatus === 'all_reviewed') $having[] = "unreviewed_count = 0";
            if ($reviewStatus === 'pending') $having[] = "unreviewed_count > 0";

            $recordingStatus = $_GET['recordingStatus'] ?? 'all';
            if ($recordingStatus === 'has_recordings') $having[] = "recordings_count > 0";
            if ($recordingStatus === 'no_recordings') $having[] = "recordings_count = 0";

            $minDuration = isset($_GET['minDuration']) ? (int)$_GET['minDuration'] : null;
            if ($minDuration !== null) $having[] = "total_duration >= $minDuration";

            $minInteractions = isset($_GET['minInteractions']) ? (int)$_GET['minInteractions'] : null;
            if ($minInteractions !== null) $having[] = "total_calls >= $minInteractions";

            // New filters for Callers2
            $durationFilter = $_GET['durationFilter'] ?? 'all';
            if ($durationFilter === 'short') $having[] = "total_duration < 60";
            if ($durationFilter === 'medium') $having[] = "total_duration >= 60 AND total_duration <= 600";
            if ($durationFilter === 'long') $having[] = "total_duration > 600";

            $interactionFilter = $_GET['interactionFilter'] ?? 'all';
            if ($interactionFilter === 'frequent') $having[] = "total_calls > 10";
            if ($interactionFilter === 'rare') $having[] = "total_calls >= 1 AND total_calls <= 3";

            $lastCallTypeFilter = $_GET['lastCallType'] ?? 'all';
            if ($lastCallTypeFilter === 'inbound') $having[] = "(LOWER(last_call_type) LIKE '%inbound%' OR LOWER(last_call_type) LIKE '%in%')";
            if ($lastCallTypeFilter === 'outbound') $having[] = "(LOWER(last_call_type) LIKE '%outbound%' OR LOWER(last_call_type) LIKE '%out%')";

            $firstCallTypeFilter = $_GET['firstCallType'] ?? 'all';
            if ($firstCallTypeFilter === 'inbound') $having[] = "(LOWER(first_call_type) LIKE '%inbound%' OR LOWER(first_call_type) LIKE '%in%')";
            if ($firstCallTypeFilter === 'outbound') $having[] = "(LOWER(first_call_type) LIKE '%outbound%' OR LOWER(first_call_type) LIKE '%out%')";

            $ratioFilter = $_GET['inOutRatioFilter'] ?? 'all';
            if ($ratioFilter === 'more_in') $having[] = "in_out_ratio > 1";
            if ($ratioFilter === 'more_out') $having[] = "in_out_ratio < 1";
            if ($ratioFilter === 'equal') $having[] = "ABS(in_out_ratio - 1) < 0.01";

            $lastByFilter = $_GET['lastCallBy'] ?? 'all';
            if ($lastByFilter === 'caller') $having[] = "(LOWER(last_call_type) LIKE '%inbound%' OR LOWER(last_call_type) LIKE '%in%')";
            if ($lastByFilter === 'employee') $having[] = "(LOWER(last_call_type) LIKE '%outbound%' OR LOWER(last_call_type) LIKE '%out%')";

            $havingClause = !empty($having) ? "HAVING " . implode(' AND ', $having) : "";

            // Sorting
            $sortBy = $_GET['sortBy'] ?? 'last_call';
            $sortOrder = $_GET['sortOrder'] ?? 'DESC';
            
            $allowedSort = [
                'name' => 'name',
                'phone' => 'phone',
                'total_calls' => 'total_calls',
                'total_duration' => 'total_duration',
                'last_call' => 'last_call',
                'first_call' => 'first_call',
                'avg_duration' => 'avg_duration',
                'in_out_ratio' => 'in_out_ratio'
            ];
            $sortColumn = $allowedSort[$sortBy] ?? 'last_call';
            $sortOrder = ($sortOrder === 'ASC') ? 'ASC' : 'DESC';

            // Total count for pagination (requires subquery because of HAVING)
            $countQuery = "SELECT COUNT(*) as total FROM (
                SELECT c.caller_phone
                FROM call_log c 
                LEFT JOIN call_log_phones co ON (c.caller_phone = co.phone AND c.org_id = co.org_id)
                WHERE $whereClause
                GROUP BY c.caller_phone
                $havingClause
            ) as t";
            $countRes = Database::getOne($countQuery);
            $total = $countRes['total'] ?? 0;

            $query = "
                SELECT 
                    c.caller_phone as phone,
                    IFNULL(co.name, MAX(c.caller_name)) as name,
                    co.label,
                    co.notes,
                    SUBSTRING_INDEX(GROUP_CONCAT(COALESCE(c.labels, '') ORDER BY c.call_time DESC SEPARATOR '||'), '||', 1) as call_labels,
                    COUNT(c.id) as total_calls,
                    SUM(c.duration) as total_duration,
                    MIN(c.call_time) as first_call,
                    MAX(c.call_time) as last_call,
                    AVG(NULLIF(c.duration, 0)) as avg_duration,
                    COUNT(CASE WHEN c.duration > 0 THEN 1 END) as connected_calls,
                    COUNT(CASE WHEN c.duration = 0 THEN 1 ELSE NULL END) as missed_calls,
                    COUNT(CASE WHEN (c.reviewed = 0 OR c.reviewed IS NULL) THEN 1 END) as unreviewed_count,
                    COUNT(CASE WHEN (c.recording_url IS NOT NULL AND c.recording_url != '') THEN 1 END) as recordings_count,
                    COUNT(CASE WHEN (LOWER(c.type) LIKE '%inbound%' OR LOWER(c.type) LIKE '%in%') THEN 1 END) / NULLIF(COUNT(CASE WHEN (LOWER(c.type) LIKE '%outbound%' OR LOWER(c.type) LIKE '%out%') THEN 1 END), 0) as in_out_ratio,
                    SUBSTRING_INDEX(GROUP_CONCAT(c.type ORDER BY c.call_time DESC SEPARATOR '||'), '||', 1) as last_call_type,
                    SUBSTRING_INDEX(GROUP_CONCAT(c.type ORDER BY c.call_time ASC SEPARATOR '||'), '||', 1) as first_call_type,
                    SUBSTRING_INDEX(GROUP_CONCAT(c.duration ORDER BY c.call_time DESC SEPARATOR '||'), '||', 1) as last_call_duration,
                    SUBSTRING_INDEX(GROUP_CONCAT(c.id ORDER BY c.call_time DESC SEPARATOR '||'), '||', 1) as last_call_id,
                    SUBSTRING_INDEX(GROUP_CONCAT(COALESCE(e.name, 'Unknown') ORDER BY c.call_time DESC SEPARATOR '||'), '||', 1) as last_call_by
                FROM call_log c
                LEFT JOIN call_log_phones co ON (c.caller_phone = co.phone AND c.org_id = co.org_id)
                LEFT JOIN employees e ON c.employee_id = e.id
                WHERE $whereClause
                GROUP BY c.caller_phone
                $havingClause
                ORDER BY $sortColumn $sortOrder
                LIMIT $limit OFFSET $offset
            ";
            
            $callers = Database::select($query);
            Response::success([
                'data' => $callers,
                'pagination' => [
                    'total' => (int)$total,
                    'page' => $page,
                    'limit' => $limit,
                    'total_pages' => ceil($total / $limit)
                ]
            ], 'Callers retrieved successfully');
            break;
        }

        // Return unique labels
        if ($action === 'labels') {
            $labelsRows = Database::select("
                SELECT DISTINCT label 
                FROM call_log_phones 
                WHERE org_id = '$orgId' AND label IS NOT NULL AND label != ''
                ORDER BY label ASC
            ");
            
            $uniqueLabels = [];
            foreach ($labelsRows as $row) {
                $parts = explode(',', $row['label']);
                foreach ($parts as $part) {
                    $part = trim($part);
                    if ($part && !in_array($part, $uniqueLabels)) {
                        $uniqueLabels[] = $part;
                    }
                }
            }
            sort($uniqueLabels);
            $result = array_map(function($l) { return ['label' => $l]; }, $uniqueLabels);
            Response::success($result, 'Labels retrieved successfully');
            break;
        }
        
        $search = $_GET['search'] ?? '';
        $label = $_GET['label'] ?? '';
        
        $where = ["org_id = '$orgId'"];
        
        if ($search) {
            $search = Database::escape($search);
            $where[] = "(name LIKE '%$search%' OR phone LIKE '%$search%')";
        }
        
        if ($label && $label !== 'all') {
            $label = Database::escape($label);
            $where[] = "label LIKE '%$label%'";
        }
        
        $whereClause = implode(' AND ', $where);
        
        $contacts = Database::select("
            SELECT * FROM call_log_phones 
            WHERE $whereClause 
            ORDER BY name ASC 
            LIMIT 100
        ");
        
        Response::success($contacts, 'Contacts retrieved successfully');
        break;
    
    /* ===== CREATE/UPDATE CONTACT (UPSERT) ===== */
    case 'POST':
        // Handle exclude action
        if ($action === 'exclude') {
            Validator::required($data, ['phone_number']);
            
            $phone = Database::escape($data['phone_number']);
            $excluded = isset($data['excluded']) ? (bool)$data['excluded'] : true;
            $name = isset($data['name']) ? Database::escape($data['name']) : '';
            
            // Check if already in excluded_contacts
            $existing = Database::getOne("SELECT id FROM excluded_contacts WHERE org_id = '$orgId' AND phone = '$phone'");
            
            if ($excluded) {
                // Add to exclude list
                if (!$existing) {
                    $sql = "INSERT INTO excluded_contacts (org_id, phone, name, is_active) 
                            VALUES ('$orgId', '$phone', '$name', 1)";
                    Database::insert($sql);
                } else {
                    // Reactivate if exists but was inactive
                    Database::execute("UPDATE excluded_contacts SET is_active = 1 WHERE id = {$existing['id']}");
                }
                Response::success(['excluded' => true], 'Contact added to exclude list');
            } else {
                // Remove from exclude list
                if ($existing) {
                    Database::execute("DELETE FROM excluded_contacts WHERE id = {$existing['id']}");
                }
                Response::success(['excluded' => false], 'Contact removed from exclude list');
            }
            break;
        }
        
        Validator::required($data, ['phone']);
        
        $phone = Database::escape($data['phone']);
        $name = isset($data['name']) ? Database::escape($data['name']) : '';
        $label = isset($data['label']) ? Database::escape($data['label']) : ''; // e.g. 'VIP', 'Spam'
        $email = isset($data['email']) ? Database::escape($data['email']) : '';
        $notes = isset($data['notes']) ? Database::escape($data['notes']) : '';
        
        // Check if exists
        $existing = Database::getOne("SELECT id FROM call_log_phones WHERE org_id = '$orgId' AND phone = '$phone'");
        
        if ($existing) {
            // Update
            $updates = [];
            if (isset($data['name'])) $updates[] = "name = '$name'";
            if (isset($data['label'])) $updates[] = "label = '$label'";
            if (isset($data['email'])) $updates[] = "email = '$email'";
            if (isset($data['notes'])) $updates[] = "notes = '$notes'";
            
            if (!empty($updates)) {
                $sql = "UPDATE call_log_phones SET " . implode(', ', $updates) . " WHERE id = {$existing['id']}";
                Database::execute($sql);
            }
            $contactId = $existing['id'];
            $msg = 'Contact updated';
        } else {
            // Insert
            $sql = "INSERT INTO call_log_phones (org_id, phone, name, label, email, notes) 
                    VALUES ('$orgId', '$phone', '$name', '$label', '$email', '$notes')";
            $contactId = Database::insert($sql);
            $msg = 'Contact created';
        }
        
        $contact = Database::getOne("SELECT * FROM call_log_phones WHERE id = $contactId");
        Response::success($contact, $msg);
        break;

    /* ===== DELETE CONTACT ===== */
    case 'DELETE':
        if (!$id) Response::error('ID required');
        
        Database::execute("DELETE FROM call_log_phones WHERE id = $id AND org_id = '$orgId'");
        Response::success(null, 'Contact deleted');
        break;
        
    default:
        Response::error('Method not allowed', 405);
}
