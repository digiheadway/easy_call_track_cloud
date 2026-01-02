<?php
/* =====================================
   CallCloud Admin - Data Export API
   ===================================== */

require_once '../config.php';
require_once '../utils.php';

// Set headers
header('Access-Control-Allow-Origin: ' . CORS_ALLOWED_ORIGINS);
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Authorization, Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Require authentication
$currentUser = Auth::requireAuth();
$orgId = $currentUser['org_id'];

// Get parameters
$action = $_GET['action'] ?? '';
$type = $_GET['type'] ?? '';

if ($action !== 'export') {
    Response::error('Invalid action');
}

/**
 * Convert array to CSV and output
 */
function outputCSV($data, $filename, $headers) {
    // Set CSV headers
    header('Content-Type: text/csv; charset=utf-8');
    header('Content-Disposition: attachment; filename="' . $filename . '"');
    header('Pragma: no-cache');
    header('Expires: 0');
    
    // Open output stream
    $output = fopen('php://output', 'w');
    
    // Add BOM for Excel compatibility with UTF-8
    fprintf($output, chr(0xEF).chr(0xBB).chr(0xBF));
    
    // Write header row
    fputcsv($output, $headers);
    
    // Write data rows
    foreach ($data as $row) {
        $csvRow = [];
        foreach ($headers as $header) {
            $key = strtolower(str_replace(' ', '_', $header));
            $csvRow[] = $row[$key] ?? '';
        }
        fputcsv($output, $csvRow);
    }
    
    fclose($output);
    exit;
}

if ($type === 'calls') {
    // Build Base WHERE
    $where = ["c.org_id = '$orgId'"];
    
    // Get Filters (Mirror calls.php)
    $dateFilter = $_GET['dateRange'] ?? 'all';
    $employeeId = $_GET['employeeId'] ?? '';
    $direction = $_GET['direction'] ?? '';
    $reviewedFilter = $_GET['reviewed'] ?? '';
    $connectedFilter = $_GET['connected'] ?? 'all';
    $noteFilter = $_GET['noteFilter'] ?? 'all';
    $recordingFilter = $_GET['recordingFilter'] ?? 'all';
    $durationFilter = $_GET['durationFilter'] ?? 'all';
    $labelFilter = $_GET['label'] ?? 'all';
    $search = $_GET['search'] ?? '';
    
    // Custom Filters (JSON)
    $customFiltersJson = $_GET['customFilters'] ?? '';
    $customFilters = [];
    if ($customFiltersJson) {
        $customFilters = json_decode($customFiltersJson, true);
    }

    // Date Filter logic
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
        case '7days':
            $where[] = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 7 DAY)";
            break;
        case '14days':
            $where[] = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 14 DAY)";
            break;
        case '30days':
            $where[] = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 30 DAY)";
            break;
        case 'custom':
            if (isset($_GET['startDate']) && isset($_GET['endDate'])) {
                $s = Database::escape($_GET['startDate']);
                $e = Database::escape($_GET['endDate']);
                $where[] = "DATE($sqlLocalTime) BETWEEN '$s' AND '$e'";
            }
            break;
    }

    // Employee Filter
    if ($employeeId && $employeeId !== 'all') {
        $where[] = "c.employee_id = " . (int)$employeeId;
    }
    
    // Direction / Type Filter
    if ($direction && $direction !== 'all') {
        $dirMap = [
            'inbound' => 'Inbound',
            'outbound' => 'Outbound',
            'missed' => 'Missed',
            'rejected' => 'Rejected',
            'blocked' => 'Blocked'
        ];
        $dbType = $dirMap[$direction] ?? ucfirst($direction);
        if ($dbType === 'Inbound') {
            $where[] = "(c.type = 'Inbound' OR c.type = 'Incoming')";
        } else if ($dbType === 'Outbound') {
            $where[] = "(c.type = 'Outbound' OR c.type = 'Outgoing')";
        } else {
            $where[] = "c.type = '" . Database::escape($dbType) . "'";
        }
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
    if ($connectedFilter === 'connected') {
        $where[] = "c.duration > 0";
    } else if ($connectedFilter === 'not_connected') {
        $where[] = "c.duration = 0";
    }

    // Note Filter
    switch ($noteFilter) {
        case 'has_call_note':
            $where[] = "(c.note IS NOT NULL AND c.note != '')";
            break;
        case 'no_call_note':
            $where[] = "(c.note IS NULL OR c.note = '')";
            break;
        case 'has_person_note':
            $where[] = "(ct.notes IS NOT NULL AND ct.notes != '')";
            break;
        case 'no_person_note':
            $where[] = "(ct.notes IS NULL OR ct.notes = '')";
            break;
        case 'has_any_note':
            $where[] = "((c.note IS NOT NULL AND c.note != '') OR (ct.notes IS NOT NULL AND ct.notes != ''))";
            break;
    }

    // Recording Filter
    if ($recordingFilter === 'has_recording') {
        $where[] = "(c.recording_url IS NOT NULL AND c.recording_url != '')";
    } else if ($recordingFilter === 'no_recording') {
        $where[] = "(c.recording_url IS NULL OR c.recording_url = '')";
    }

    // Duration Filter
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
    if ($labelFilter !== 'all') {
        $labelFilter = Database::escape($labelFilter);
        $where[] = "(c.labels LIKE '%$labelFilter%' OR ct.label LIKE '%$labelFilter%')";
    }

    // Advanced/Custom Filters
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
                'person_note' => 'ct.notes',
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
                $personKey = 'ct.label';
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

    // Search
    if ($search) {
        $search = Database::escape($search);
        $where[] = "(c.caller_name LIKE '%$search%' OR c.caller_phone LIKE '%$search%' OR c.note LIKE '%$search%')";
    }
    
    $whereClause = implode(' AND ', $where);
    
    $calls = Database::select("
        SELECT 
            c.id,
            c.call_time,
            c.caller_name as contact_name,
            c.caller_phone as phone_number,
            c.type,
            c.duration,
            c.note,
            c.labels,
            c.reviewed,
            c.is_liked,
            c.device_phone,
            c.recording_url,
            e.name as employee_name,
            ct.notes as person_notes,
            ct.label as person_label,
            ct.email as person_email,
            ct.incomings as total_incomings,
            ct.incoming_connected,
            ct.outgoings as total_outgoings,
            ct.outgoing_connected
        FROM calls c
        LEFT JOIN employees e ON c.employee_id = e.id
        LEFT JOIN contacts ct ON (c.caller_phone = ct.phone AND c.org_id = ct.org_id)
        WHERE $whereClause
        ORDER BY c.call_time DESC
    ");
    
    // Format the data for CSV
    $formattedData = [];
    foreach ($calls as $call) {
        // Format duration to MM:SS
        $duration = (int)($call['duration'] ?? 0);
        $minutes = floor($duration / 60);
        $seconds = $duration % 60;
        $durationFormatted = sprintf('%d:%02d', $minutes, $seconds);
        
        $formattedData[] = [
            'id' => $call['id'],
            'call_time' => $call['call_time'],
            'contact_name' => $call['contact_name'] ?? '',
            'phone_number' => $call['phone_number'] ?? '',
            'type' => $call['type'] ?? '',
            'duration_seconds' => $duration,
            'duration_formatted' => $durationFormatted,
            'call_note' => $call['note'] ?? '',
            'labels' => $call['labels'] ?? '',
            'reviewed' => $call['reviewed'] ? 'Yes' : 'No',
            'is_liked' => $call['is_liked'] ? 'Yes' : 'No',
            'device_phone' => $call['device_phone'] ?? '',
            'employee_name' => $call['employee_name'] ?? '',
            'recording_url' => $call['recording_url'] ?? '',
            'person_notes' => $call['person_notes'] ?? '',
            'person_label' => $call['person_label'] ?? '',
            'person_email' => $call['person_email'] ?? '',
            'total_incomings' => $call['total_incomings'] ?? 0,
            'incoming_connected' => $call['incoming_connected'] ?? 0,
            'total_outgoings' => $call['total_outgoings'] ?? 0,
            'outgoing_connected' => $call['outgoing_connected'] ?? 0
        ];
    }
    
    $headers = [
        'ID',
        'Call_Time',
        'Contact_Name',
        'Phone_Number',
        'Type',
        'Duration_Seconds',
        'Duration_Formatted',
        'Call_Note',
        'Labels',
        'Reviewed',
        'Is_Liked',
        'Device_Phone',
        'Employee_Name',
        'Recording_URL',
        'Person_Notes',
        'Person_Label',
        'Person_Email',
        'Total_Incomings',
        'Incoming_Connected',
        'Total_Outgoings',
        'Outgoing_Connected'
    ];
    
    $filename = 'calls_export_' . date('Y-m-d_His') . '.csv';
    outputCSV($formattedData, $filename, $headers);
    
} else if ($type === 'callers') {
    // Export Callers/Contacts
    $contacts = Database::select("
        SELECT 
            c.id,
            c.phone,
            c.name,
            c.label,
            c.email,
            c.notes,
            c.incomings,
            c.incoming_connected,
            c.outgoings,
            c.outgoing_connected,
            c.last_call_type,
            c.last_call_duration,
            c.last_call_time,
            c.created_at,
            c.updated_at,
            e.name as employee_name
        FROM contacts c
        LEFT JOIN employees e ON c.employee_id = e.id
        WHERE c.org_id = '$orgId'
        ORDER BY c.name ASC, c.phone ASC
    ");
    
    // Format the data for CSV
    $formattedData = [];
    foreach ($contacts as $contact) {
        // Format last call duration to MM:SS
        $duration = (int)($contact['last_call_duration'] ?? 0);
        $minutes = floor($duration / 60);
        $seconds = $duration % 60;
        $durationFormatted = sprintf('%d:%02d', $minutes, $seconds);
        
        // Calculate total calls and connected calls
        $totalCalls = ($contact['incomings'] ?? 0) + ($contact['outgoings'] ?? 0);
        $totalConnected = ($contact['incoming_connected'] ?? 0) + ($contact['outgoing_connected'] ?? 0);
        
        $formattedData[] = [
            'id' => $contact['id'],
            'name' => $contact['name'] ?? '',
            'phone' => $contact['phone'] ?? '',
            'label' => $contact['label'] ?? '',
            'email' => $contact['email'] ?? '',
            'notes' => $contact['notes'] ?? '',
            'total_calls' => $totalCalls,
            'total_connected' => $totalConnected,
            'total_incomings' => $contact['incomings'] ?? 0,
            'incoming_connected' => $contact['incoming_connected'] ?? 0,
            'total_outgoings' => $contact['outgoings'] ?? 0,
            'outgoing_connected' => $contact['outgoing_connected'] ?? 0,
            'last_call_type' => $contact['last_call_type'] ?? '',
            'last_call_duration_seconds' => $duration,
            'last_call_duration_formatted' => $durationFormatted,
            'last_call_time' => $contact['last_call_time'] ?? '',
            'employee_name' => $contact['employee_name'] ?? '',
            'created_at' => $contact['created_at'] ?? '',
            'updated_at' => $contact['updated_at'] ?? ''
        ];
    }
    
    $headers = [
        'ID',
        'Name',
        'Phone',
        'Label',
        'Email',
        'Notes',
        'Total_Calls',
        'Total_Connected',
        'Total_Incomings',
        'Incoming_Connected',
        'Total_Outgoings',
        'Outgoing_Connected',
        'Last_Call_Type',
        'Last_Call_Duration_Seconds',
        'Last_Call_Duration_Formatted',
        'Last_Call_Time',
        'Employee_Name',
        'Created_At',
        'Updated_At'
    ];
    
    $filename = 'callers_export_' . date('Y-m-d_His') . '.csv';
    outputCSV($formattedData, $filename, $headers);
    
} else {
    Response::error('Invalid export type. Use "calls" or "callers".');
}
