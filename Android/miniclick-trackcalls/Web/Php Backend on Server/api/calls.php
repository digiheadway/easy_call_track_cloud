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
                FROM call_log 
                WHERE org_id = '$orgId'
            ");
            
            // Format average duration back to MM:SS
            $avg = $stats['avg_duration'] ?? 0;
            $minutes = floor($avg / 60);
            $seconds = $avg % 60;
            $stats['avg_duration_formatted'] = sprintf('%d:%02d', $minutes, $seconds);
            
            Response::success($stats, 'Call statistics retrieved');
        } else if ($action === 'debug_time') {
            $res = Database::select("
                SELECT 
                    id, 
                    call_time, 
                    UTC_TIMESTAMP() as db_utc_now,
                    NOW() as db_now
                FROM call_log 
                ORDER BY call_time DESC 
                LIMIT 5
            ");
            Response::success([
                'data' => $res,
                'php_now' => date('Y-m-d H:i:s'),
                'php_timezone' => date_default_timezone_get()
            ]);
        } else if ($action === 'list') {
            $limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 50;
            $calls = Database::select("
                SELECT 
                    c.*, 
                    IFNULL(c_info.name, c.caller_name) as contact_name,
                    c.caller_phone as phone_number,
                    e.name as employee_name
                FROM call_log c
                LEFT JOIN employees e ON c.employee_id = e.id
                LEFT JOIN call_log_phones c_info ON (c.caller_phone = c_info.phone AND c.org_id = c_info.org_id)
                WHERE c.org_id = '$orgId'
                ORDER BY c.call_time DESC
                LIMIT $limit
            ");
            Response::success($calls, 'Recent calls retrieved');
        } else if ($action === 'labels') {
            // Unique labels from calls
            $labelsRows = Database::select("SELECT DISTINCT labels FROM call_log WHERE org_id = '$orgId' AND labels != ''");
            // Unique labels from contacts
            $personLabelsRows = Database::select("SELECT DISTINCT label FROM call_log_phones WHERE org_id = '$orgId' AND label != ''");
            
            $uniqueLabels = [];
            
            // Collect from calls
            foreach ($labelsRows as $row) {
                $parts = explode(',', $row['labels']);
                foreach ($parts as $part) {
                    $part = trim($part);
                    if ($part && !in_array($part, $uniqueLabels)) {
                        $uniqueLabels[] = $part;
                    }
                }
            }
            
            // Collect from contacts
            foreach ($personLabelsRows as $row) {
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
        } else if ($action === 'migrate_db') {
            // Temporary migration to add is_archived column
            try {
                $check = Database::getOne("SHOW COLUMNS FROM call_log LIKE 'is_archived'");
                if (!$check) {
                    Database::execute("ALTER TABLE call_log ADD COLUMN is_archived TINYINT(1) DEFAULT 0");
                    Response::success([], 'Migration successful: Added is_archived column');
                } else {
                    Response::success([], 'Migration skipped: Column already exists');
                }
            } catch (Exception $e) {
                Response::error('Migration failed: ' . $e->getMessage());
            }
        } else {
            // Get filters
            $direction = $_GET['direction'] ?? ''; // Maps to type
            $dateFilter = $_GET['dateRange'] ?? $_GET['dateFilter'] ?? 'all'; // Support dateRange param
            $employeeId = $_GET['employeeId'] ?? ''; // Support employee filter
            $reviewedFilter = $_GET['reviewed'] ?? ''; // Filter by reviewed status
            
            $search = $_GET['search'] ?? '';
            $page = isset($_GET['page']) ? (int)$_GET['page'] : 1;
            $limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 20;
            $offset = ($page - 1) * $limit;

            // Sorting
            $sortBy = $_GET['sortBy'] ?? 'call_time';
            $sortOrder = $_GET['sortOrder'] ?? 'DESC';
            
            // Validate sortBy to prevent SQL Injection
            $allowedSort = ['call_time', 'duration', 'contact_name', 'phone_number', 'employee_name', 'type', 'reviewed', 'is_liked'];
            if (!in_array($sortBy, $allowedSort)) {
                $sortBy = 'call_time';
            }
            if ($sortOrder !== 'ASC' && $sortOrder !== 'DESC') {
                $sortOrder = 'DESC';
            }

            // Map frontend column names to DB columns if necessary
            $sortMap = [
                'contact_name' => 'IFNULL(c_info.name, c.caller_name)',
                'phone_number' => 'c.caller_phone',
                'employee_name' => 'e.name',
                'call_time' => 'c.call_time',
                'duration' => 'c.duration',
                'type' => 'c.type',
                'reviewed' => 'c.reviewed',
                'is_liked' => 'c.is_liked'
            ];
            $sortColumn = $sortMap[$sortBy] ?? 'c.call_time';

            // Custom Filters (JSON)
            $customFiltersJson = $_GET['customFilters'] ?? '';
            $customFilters = [];
            if ($customFiltersJson) {
                $customFilters = json_decode($customFiltersJson, true);
            }
            
            // Build query
            $where = ["c.org_id = '$orgId'"];

            // Archive Filter
            $archiveFilter = $_GET['archiveFilter'] ?? 'active'; // active, archived, all
            if ($archiveFilter === 'active') {
                $where[] = "(c.is_archived = 0 OR c.is_archived IS NULL)";
            } else if ($archiveFilter === 'archived') {
                $where[] = "c.is_archived = 1";
            }
            // if 'all', logic is to show everything (no filter on is_archived)

            // Exclusion Filter: Hide calls from contacts marked as exclude_from_list
            $where[] = "NOT EXISTS (
                SELECT 1 FROM excluded_contacts exc_list 
                WHERE exc_list.phone = c.caller_phone 
                AND exc_list.org_id = c.org_id 
                AND exc_list.exclude_from_list = 1
            )";
            
            // Employee Filter
            if ($employeeId && $employeeId !== 'all') {
                $where[] = "c.employee_id = " . (int)$employeeId;
            }
            
            if ($direction && $direction !== 'all') {
                $dirMap = [
                    'inbound' => 'Inbound',
                    'outbound' => 'Outbound',
                    'missed' => 'Missed',
                    'rejected' => 'Rejected',
                    'blocked' => 'Blocked'
                ];
                $dbType = $dirMap[$direction] ?? $direction;
                if ($dbType === 'Inbound') {
                    $where[] = "(c.type = 'Inbound' OR c.type = 'Incoming')";
                } else if ($dbType === 'Outbound') {
                    $where[] = "(c.type = 'Outbound' OR c.type = 'Outgoing')";
                } else {
                    $where[] = "c.type = '" . Database::escape($dbType) . "'";
                }
            }
            
            // Date Filter logic (aligned with Dashboard)
            $timezoneOffset = isset($_GET['tzOffset']) ? (int)$_GET['tzOffset'] : 0; 
            $dbOffsetMinutes = -1 * $timezoneOffset;
            $sqlLocalTime = "DATE_ADD(c.call_time, INTERVAL $dbOffsetMinutes MINUTE)";
            $sqlLocalNow = "DATE_ADD(UTC_TIMESTAMP(), INTERVAL $dbOffsetMinutes MINUTE)";

            switch ($dateFilter) {
                case 'today':
                    $where[] = "DATE($sqlLocalTime) = DATE($sqlLocalNow)";
                    break;
                case 'yesterday':
                    $where[] = "DATE($sqlLocalTime) = DATE(DATE_SUB($sqlLocalNow, INTERVAL 1 DAY))";
                    break;
                case '3days':
                   $where[] = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 3 DAY)";
                   break;
                case 'week': // Keeping for legacy support
                case '7days':
                    $where[] = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 7 DAY)";
                    break;
                case '14days':
                    $where[] = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 14 DAY)";
                    break;
                case 'month':
                case '30days':
                    $where[] = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 30 DAY)";
                    break;
                case '60days':
                    $where[] = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 60 DAY)";
                    break;
                case '90days':
                    $where[] = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 90 DAY)";
                    break;
                case '180days':
                    $where[] = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 180 DAY)";
                    break;
                case 'this_week':
                    // Mode 1: Monday is first day of week
                    $where[] = "YEARWEEK($sqlLocalTime, 1) = YEARWEEK($sqlLocalNow, 1)";
                    break;
                case 'last_week':
                    $where[] = "YEARWEEK($sqlLocalTime, 1) = YEARWEEK(DATE_SUB($sqlLocalNow, INTERVAL 1 WEEK), 1)";
                    break;
                case 'this_month':
                    $where[] = "DATE_FORMAT($sqlLocalTime, '%Y-%m') = DATE_FORMAT($sqlLocalNow, '%Y-%m')";
                    break;
                case 'last_month':
                    $where[] = "DATE_FORMAT($sqlLocalTime, '%Y-%m') = DATE_FORMAT(DATE_SUB($sqlLocalNow, INTERVAL 1 MONTH), '%Y-%m')";
                    break;
                case 'all_time':
                    // No date filter
                    break;
                case 'custom':
                    // Expect startDate and endDate in YYYY-MM-DD
                    if (isset($_GET['startDate']) && isset($_GET['endDate'])) {
                        $s = Database::escape($_GET['startDate']);
                        $e = Database::escape($_GET['endDate']);
                        $where[] = "DATE($sqlLocalTime) BETWEEN '$s' AND '$e'";
                    }
                    break;
            }
            
            // Reviewed Filter
            if ($reviewedFilter !== '' && $reviewedFilter !== 'all') {
                if ($reviewedFilter === 'reviewed') {
                    $where[] = "c.reviewed = 1";
                } else if ($reviewedFilter === 'unreviewed' || $reviewedFilter === 'not_reviewed') {
                    $where[] = "(c.reviewed = 0 OR c.reviewed IS NULL)";
                }
            }
            
            // Connected Filter
            $connectedFilter = $_GET['connected'] ?? 'all';
            if ($connectedFilter === 'connected') {
                $where[] = "c.duration > 0";
            } else if ($connectedFilter === 'not_connected') {
                $where[] = "c.duration = 0";
            }
            
            // Note Filter
            $noteFilter = $_GET['noteFilter'] ?? 'all';
            switch ($noteFilter) {
                case 'has_call_note':
                    $where[] = "(c.note IS NOT NULL AND c.note != '')";
                    break;
                case 'no_call_note':
                    $where[] = "(c.note IS NULL OR c.note = '')";
                    break;
                case 'has_person_note':
                    $where[] = "(c_info.notes IS NOT NULL AND c_info.notes != '')";
                    break;
                case 'no_person_note':
                    $where[] = "(c_info.notes IS NULL OR c_info.notes = '')";
                    break;
                case 'has_any_note':
                    $where[] = "((c.note IS NOT NULL AND c.note != '') OR (c_info.notes IS NOT NULL AND c_info.notes != ''))";
                    break;
            }
            
            // Name Filter
            $nameFilter = $_GET['nameFilter'] ?? 'all';
            if ($nameFilter === 'has_name') {
                $where[] = "((c_info.name IS NOT NULL AND c_info.name != '') OR (c.caller_name IS NOT NULL AND c.caller_name != '' AND c.caller_name != c.caller_phone))";
            } else if ($nameFilter === 'no_name') {
                $where[] = "((c_info.name IS NULL OR c_info.name = '') AND (c.caller_name IS NULL OR c.caller_name = '' OR c.caller_name = c.caller_phone))";
            }
            
            // Name Filter End

            // Recording Filter
            $recordingFilter = $_GET['recordingFilter'] ?? 'all';
            if ($recordingFilter === 'has_recording') {
                $where[] = "(c.recording_url IS NOT NULL AND c.recording_url != '')";
            } else if ($recordingFilter === 'no_recording') {
                $where[] = "(c.recording_url IS NULL OR c.recording_url = '')";
            } else if ($recordingFilter === 'pending_upload') {
                $where[] = "(c.upload_status = 'pending' AND (c.recording_url IS NULL OR c.recording_url = ''))";
            }
            
            // Duration Filter
            $durationFilter = $_GET['durationFilter'] ?? 'all';
            switch ($durationFilter) {
                case 'under_30s':
                    $where[] = "c.duration > 0 AND c.duration < 30";
                    break;
                case '30s_to_1m':
                    $where[] = "c.duration >= 30 AND c.duration < 60";
                    break;
                case '1m_to_5m':
                    $where[] = "c.duration >= 60 AND c.duration < 300";
                    break;
                case 'over_5m':
                    $where[] = "c.duration >= 300";
                    break;
            }
            
            // Label Filter
            $labelFilter = $_GET['label'] ?? 'all';
            if ($labelFilter !== 'all') {
                $labelFilter = Database::escape($labelFilter);
                $where[] = "(c.labels LIKE '%$labelFilter%' OR c_info.label LIKE '%$labelFilter%')";
            }
            
            if ($search) {
                $search = Database::escape($search);
                $where[] = "(
                    c.caller_name LIKE '%$search%' OR 
                    c.caller_phone LIKE '%$search%' OR 
                    c.note LIKE '%$search%' OR
                    c.labels LIKE '%$search%' OR
                    c_info.name LIKE '%$search%' OR
                    c_info.notes LIKE '%$search%' OR
                    c_info.label LIKE '%$search%' OR
                    e.name LIKE '%$search%'
                )";
            }

            // Advanced Filters
            if (!empty($customFilters) && is_array($customFilters)) {
                foreach ($customFilters as $f) {
                    $key = $f['key'] ?? '';
                    $op = $f['operator'] ?? 'equal';
                    $val = $f['value'] ?? '';
                    
                    if (!$key) continue;
                    
                    // Map keys to DB columns
                    $keyMap = [
                        'phone_number' => 'c.caller_phone',
                        'contact_name' => 'c.caller_name',
                        'duration' => 'c.duration',
                        'employee_name' => 'e.name',
                        'note' => 'c.note',
                        'person_note' => 'c_info.notes',
                        'type' => 'c.type',
                        'labels' => 'c.labels'
                    ];
                    
                    $dbKey = $keyMap[$key] ?? null;
                    if (!$dbKey) continue;
                    
                    $valEsc = Database::escape($val);
                    
                    $sql = "";
                    switch ($op) {
                        case 'equal': $sql = "$dbKey = '$valEsc'"; break;
                        case 'not_equal': $sql = "$dbKey != '$valEsc'"; break;
                        case 'contains': $sql = "$dbKey LIKE '%$valEsc%'"; break;
                        case 'not_contains': $sql = "$dbKey NOT LIKE '%$valEsc%'"; break;
                        case 'greater_than': $sql = "$dbKey > " . (float)$val; break;
                        case 'less_than': $sql = "$dbKey < " . (float)$val; break;
                        case 'starts_with': $sql = "$dbKey LIKE '$valEsc%'"; break;
                        case 'ends_with': $sql = "$dbKey LIKE '%$valEsc'"; break;
                        case 'is_empty': $sql = "($dbKey IS NULL OR $dbKey = '')"; break;
                        case 'is_not_empty': $sql = "($dbKey IS NOT NULL AND $dbKey != '')"; break;
                    }
                    
                    if ($key === 'labels' && $sql) {
                        // For labels, search in both tables
                        $personKey = 'c_info.label';
                        $personSql = str_replace($dbKey, $personKey, $sql);
                        if ($op === 'not_contains' || $op === 'not_equal' || $op === 'is_empty') {
                            $where[] = "($sql AND $personSql)";
                        } else {
                            $where[] = "($sql OR $personSql)";
                        }
                    } else if ($sql) {
                        $where[] = $sql;
                    }
                }
            }
            
            $whereClause = implode(' AND ', $where);
            
            // Get Total Count
            $countResult = Database::getOne("
                SELECT COUNT(*) as total 
                FROM call_log c 
                LEFT JOIN employees e ON c.employee_id = e.id
                LEFT JOIN (
                    SELECT phone, org_id, MAX(name) as name, MAX(notes) as notes, MAX(label) as label 
                    FROM call_log_phones 
                    GROUP BY phone, org_id
                ) c_info ON (c.caller_phone = c_info.phone AND c.org_id = c_info.org_id)
                LEFT JOIN excluded_contacts exc ON (c.caller_phone = exc.phone AND c.org_id = exc.org_id)
                WHERE $whereClause
            ");
            $total = $countResult['total'];
            
            // Get Data
            $calls = Database::select("
                SELECT 
                    c.*, 
                    IFNULL(c_info.name, c.caller_name) as contact_name,
                    c.caller_phone as phone_number,
                    e.name as employee_name,
                    c_info.notes as person_note,
                    c_info.label as person_labels,
                    CASE WHEN exc.id IS NOT NULL THEN 1 ELSE 0 END as is_excluded,
                    exc.exclude_from_sync,
                    exc.exclude_from_list
                FROM call_log c
                LEFT JOIN employees e ON c.employee_id = e.id
                LEFT JOIN (
                    SELECT phone, org_id, MAX(name) as name, MAX(notes) as notes, MAX(label) as label 
                    FROM call_log_phones 
                    GROUP BY phone, org_id
                ) c_info ON (c.caller_phone = c_info.phone AND c.org_id = c_info.org_id)
                LEFT JOIN excluded_contacts exc ON (c.caller_phone = exc.phone AND c.org_id = exc.org_id)
                WHERE $whereClause 
                ORDER BY $sortColumn $sortOrder
                LIMIT $limit OFFSET $offset
            ");
            
            Response::success([
                'data' => $calls,
                'pagination' => [
                    'total' => (int)$total,
                    'page' => $page,
                    'limit' => $limit,
                    'total_pages' => ceil($total / $limit)
                ]
            ], 'Calls retrieved successfully');
        }
        break;
    
    /* ===== CREATE OR UPDATE CALL LOG ===== */
    case 'POST':
    case 'PUT':
        // --- SPECIAL ACTIONS (Archive/Delete) ---
        if ($action === 'archive_calls') {
            $phone = $data['phone_number'] ?? '';
            if (!$phone) Response::error('Phone number required');
            $phoneEsc = Database::escape($phone);
            $sql = "UPDATE call_log SET is_archived = 1, updated_at = NOW() WHERE caller_phone = '$phoneEsc' AND org_id = '$orgId'";
            Database::execute($sql);
            Response::success([], 'Calls archived successfully');
        } 
        else if ($action === 'delete_person') {
            $phone = $data['phone_number'] ?? '';
            if (!$phone) Response::error('Phone number required');
            $phoneEsc = Database::escape($phone);
            
            // 1. Delete Calls
            Database::execute("DELETE FROM call_log WHERE caller_phone = '$phoneEsc' AND org_id = '$orgId'");
            // 2. Delete Contact Info
            Database::execute("DELETE FROM call_log_phones WHERE phone = '$phoneEsc' AND org_id = '$orgId'");
            // 3. Delete from Excluded List
            Database::execute("DELETE FROM excluded_contacts WHERE phone = '$phoneEsc' AND org_id = '$orgId'");
            
            Response::success([], 'Person and history deleted successfully');
        }

        $isUpdate = ($method === 'PUT') || ($action === 'update');

        if ($isUpdate) {
            // --- UPDATE LOGIC ---
            if (!$id) {
                Response::error('Call ID required');
            }

            // Start Logging
            $logMsg = date('Y-m-d H:i:s') . " Update Request for ID: $id\n";
            $logMsg .= "Data: " . json_encode($data) . "\n";
            
            // Verify call belongs to Org
            $call = Database::getOne("SELECT id, caller_phone, caller_name FROM call_log WHERE id = $id AND org_id = '$orgId'");
            if (!$call) {
                file_put_contents('debug_calls.log', $logMsg . "Error: Call not found\n", FILE_APPEND);
                Response::error('Call not found', 404);
            }

            $updates = [];

            // Allow updating Note
            if (isset($data['note'])) {
                $updates[] = "note = '" . Database::escape($data['note']) . "'";
            }
            
            // Allow updating reviewed status
            if (isset($data['reviewed'])) {
                $reviewed = $data['reviewed'] ? 1 : 0;
                $updates[] = "reviewed = $reviewed";
            }
            
            // Allow updating is_liked
            if (isset($data['is_liked'])) {
                $liked = $data['is_liked'] ? 1 : 0;
                $updates[] = "is_liked = $liked";
            }

            // Allow updating labels
            if (isset($data['labels'])) {
                // Determine if array or string
                $lbls = $data['labels'];
                if (is_array($lbls)) {
                    $lbls = implode(',', $lbls);
                }
                $updates[] = "labels = '" . Database::escape($lbls) . "'";
            }
            
            // Allow updating caller name
            if (isset($data['contact_name'])) {
                $pName = Database::escape($data['contact_name']);
                $updates[] = "caller_name = '$pName'";
                
                // Also update contacts table
                $phone = $call['caller_phone'];
                $contactExists = Database::getOne("SELECT id FROM call_log_phones WHERE phone = '$phone' AND org_id = '$orgId'");
                if ($contactExists) {
                    $res = Database::execute("UPDATE call_log_phones SET name = '$pName', updated_at = NOW() WHERE phone = '$phone' AND org_id = '$orgId'");
                    if (isset($res['status']) && $res['status'] === false) {
                         file_put_contents('debug_calls.log', $logMsg . "Error updating contact name: " . json_encode($res) . "\n", FILE_APPEND);
                    }
                } else {
                    Database::insert("INSERT INTO call_log_phones (org_id, phone, name, created_at, updated_at) VALUES ('$orgId', '$phone', '$pName', NOW(), NOW())");
                }
            }
            
            // Allow updating recording
            if (isset($data['recording_url'])) {
                $updates[] = "recording_url = '" . Database::escape($data['recording_url']) . "'";
            }
            
            // Update Person Note (in contacts table)
            if (isset($data['person_note'])) {
                $pNote = Database::escape($data['person_note']);
                $phone = $call['caller_phone'];
                
                $contactExists = Database::getOne("SELECT id FROM call_log_phones WHERE phone = '$phone' AND org_id = '$orgId'");
                if ($contactExists) {
                    $res = Database::execute("UPDATE call_log_phones SET notes = '$pNote', updated_at = NOW() WHERE phone = '$phone' AND org_id = '$orgId'");
                    if (isset($res['status']) && $res['status'] === false) {
                        file_put_contents('debug_calls.log', $logMsg . "Error updating person note: " . json_encode($res) . "\n", FILE_APPEND);
                        Response::error("Failed to update person note: " . ($res['error'] ?? 'Unknown error'));
                    }
                } else {
                    $pName = isset($data['contact_name']) ? Database::escape($data['contact_name']) : Database::escape($call['caller_name'] ?? 'Unknown');
                    $res = Database::insert("INSERT INTO call_log_phones (org_id, phone, name, notes, created_at, updated_at) VALUES ('$orgId', '$phone', '$pName', '$pNote', NOW(), NOW())");
                     if (!$res) { // insert returns ID or false/null? insert returns ID. execute returns array.
                         // Database::insert returns ID. If ID is missing, check execute logic. 
                         // Actually Database::insert calls Database::execute. 
                         // But insert returns insert_id. 
                     }
                }
            }
            
            // Update Person Labels (in contacts table)
            if (isset($data['person_labels'])) {
                $pLabels = Database::escape($data['person_labels']);
                $phone = $call['caller_phone'];
                
                $contactExists = Database::getOne("SELECT id FROM call_log_phones WHERE phone = '$phone' AND org_id = '$orgId'");
                if ($contactExists) {
                    Database::execute("UPDATE call_log_phones SET label = '$pLabels', updated_at = NOW() WHERE phone = '$phone' AND org_id = '$orgId'");
                } else {
                    $pName = isset($data['contact_name']) ? Database::escape($data['contact_name']) : Database::escape($call['caller_name'] ?? 'Unknown');
                    Database::insert("INSERT INTO call_log_phones (org_id, phone, name, label, created_at, updated_at) VALUES ('$orgId', '$phone', '$pName', '$pLabels', NOW(), NOW())");
                }
            }

            if (!empty($updates)) {
                $sql = "UPDATE call_log SET " . implode(', ', $updates) . " WHERE id = $id";
                $logMsg .= "SQL: $sql\n";
                $res = Database::execute($sql);
                if (isset($res['status']) && $res['status'] === false) {
                     file_put_contents('debug_calls.log', $logMsg . "Error executing SQL: " . json_encode($res) . "\n", FILE_APPEND);
                     Response::error("Failed to update call: " . ($res['error'] ?? 'Unknown DB Error'));
                }
            }
            
            file_put_contents('debug_calls.log', $logMsg . "Success\n", FILE_APPEND);

            $updatedCall = Database::getOne("
                SELECT c.*, c_info.notes as person_note 
                FROM call_log c 
                LEFT JOIN call_log_phones c_info ON (c.caller_phone = c_info.phone AND c.org_id = c_info.org_id)
                WHERE c.id = $id
            ");
            Response::success($updatedCall, 'Call updated successfully');
        
        } else {
            // --- CREATE LOGIC ---
            Validator::required($data, ['employee_id', 'phone_number', 'type']);
            
            $employeeId = (int)$data['employee_id'];
            $callerName = Database::escape($data['contact_name'] ?? '');
            $callerPhone = Database::escape($data['phone_number']);
            $type = Database::escape($data['type']);
            $duration = (int)($data['duration'] ?? 0);
            $note = Database::escape($data['note'] ?? '');
            $recordingUrl = Database::escape($data['recording_url'] ?? '');
            $callTime = isset($data['call_time']) ? Database::escape($data['call_time']) : date('Y-m-d H:i:s');
            
            $employee = Database::getOne("SELECT id, name FROM employees WHERE id = $employeeId AND org_id = '$orgId'");
            if (!$employee) {
                Response::error('Invalid employee ID');
            }
            
            $sql = "INSERT INTO call_log (
                        org_id, employee_id, caller_name, caller_phone, type, duration, call_time, note, recording_url
                    ) VALUES (
                        '$orgId', $employeeId, '$callerName', '$callerPhone', '$type', $duration, '$callTime', '$note', '$recordingUrl'
                    )";
            
            $callId = Database::insert($sql);
            
            if (!$callId) {
                Response::error('Failed to create call log');
            }
            
            Database::execute("UPDATE employees SET calls_today = calls_today + 1 WHERE id = $employeeId");
            $call = Database::getOne("SELECT * FROM call_log WHERE id = $callId");
            
            Response::success($call, 'Call logged successfully');
        }
        break;
    
    /* ===== ARCHIVE / DELETE CALLS (POST actions) ===== */
    // Note: These actions are also POST, so they should be handled inside the main POST block or we need to restructure.
    // The previous implementation had a second case 'POST' which is unreachable in a switch statement.
    // We will move this logic into the main POST block above.
    
    default:
        Response::error('Method not allowed', 405);
}
