<?php
/* =====================================
   CallCloud Admin - Reports API
   ===================================== */

require_once '../config.php';
require_once '../utils.php';

// Set headers
header('Access-Control-Allow-Origin: ' . CORS_ALLOWED_ORIGINS);
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Authorization, Content-Type');
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Require authentication
$currentUser = Auth::requireAuth();
$orgId = $currentUser['org_id'];

// Get request parameters
$reportType = $_GET['type'] ?? 'all'; // Default to all if not specified, or handle individual
$dateRange = $_GET['dateRange'] ?? '7days';

// Build date filter
$timezoneOffset = isset($_GET['tzOffset']) ? (int)$_GET['tzOffset'] : 0;
$dbOffsetMinutes = -1 * $timezoneOffset;
$sqlLocalTime = "DATE_ADD(call_time, INTERVAL $dbOffsetMinutes MINUTE)";
$sqlLocalNow = "DATE_ADD(UTC_TIMESTAMP(), INTERVAL $dbOffsetMinutes MINUTE)";

$dateFilter = '';
switch ($dateRange) {
    case 'today':
        $dateFilter = "AND DATE($sqlLocalTime) = DATE($sqlLocalNow)";
        break;
    case 'yesterday':
    case 'Yesterday':
        $dateFilter = "AND DATE($sqlLocalTime) = DATE(DATE_SUB($sqlLocalNow, INTERVAL 1 DAY))";
        break;
    case '3days':
        $dateFilter = "AND $sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 3 DAY)";
        break;
    case 'week':
    case '7days':
        $dateFilter = "AND $sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 7 DAY)";
        break;
    case '14days':
        $dateFilter = "AND $sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 14 DAY)";
        break;
    case 'month':
    case '30days':
        $dateFilter = "AND $sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 30 DAY)";
        break;
    case 'this_month':
        $dateFilter = "AND MONTH($sqlLocalTime) = MONTH($sqlLocalNow) AND YEAR($sqlLocalTime) = YEAR($sqlLocalNow)";
        break;
    case 'quarter':
        $dateFilter = "AND $sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 90 DAY)";
        break;
    case 'year':
        $dateFilter = "AND $sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 365 DAY)";
        break;
    case 'custom':
        if (isset($_GET['startDate']) && isset($_GET['endDate'])) {
            $s = Database::escape($_GET['startDate']);
            $e = Database::escape($_GET['endDate']);
            // Ensure formatting
            $dateFilter = "AND DATE($sqlLocalTime) BETWEEN '$s' AND '$e'";
        }
        break;
    default:
        $dateFilter = "AND $sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 7 DAY)"; // Default fallback
}

// Helper to format duration
function formatDuration($seconds) {
    if (!$seconds) return '0m';
    $h = floor($seconds / 3600);
    $m = floor(($seconds % 3600) / 60);
    $s = $seconds % 60;
    if ($h > 0) return "{$h}h {$m}m";
    return "{$m}m {$s}s";
}

$response = [];

// ---------------------------------------------------------
// 1. Employee Performance Report
// ---------------------------------------------------------
if ($reportType === 'all' || $reportType === 'employee_performance') {
    // Note: We use LEFT JOIN calls so we still get employees even if they have no calls in the period
    // Filtering by employee_id if provided is done in the WHERE clause of the sub-selection or main query
    
    $employeeFilter = "";
    if (isset($_GET['employeeId']) && $_GET['employeeId'] !== 'all' && $_GET['employeeId'] !== '') {
        $empId = Database::escape($_GET['employeeId']);
        $employeeFilter = "AND e.id = '$empId'";
    }

    $employees = Database::select("
        SELECT 
            e.id,
            e.name,
            COUNT(c.id) as total_calls,
            SUM(CASE WHEN (c.type = 'Inbound' OR c.type = 'Incoming') THEN 1 ELSE 0 END) as inbound_calls,
            SUM(CASE WHEN (c.type = 'Outbound' OR c.type = 'Outgoing') THEN 1 ELSE 0 END) as outbound_calls,
            SUM(CASE WHEN (c.type = 'Outbound' OR c.type = 'Outgoing') AND c.duration > 0 THEN 1 ELSE 0 END) as outbound_connected,
            SUM(c.duration) as total_duration,
            AVG(NULLIF(c.duration, 0)) as avg_handle_time, -- Avg duration of non-zero calls
            AVG(c.duration) as avg_duration_all
        FROM employees e
        LEFT JOIN calls c ON e.id = c.employee_id AND c.org_id = '$orgId' $dateFilter
        WHERE e.org_id = '$orgId' AND e.status = 'active' $employeeFilter
        GROUP BY e.id, e.name
        ORDER BY total_calls DESC
    ");
    
    $performanceData = [];
    foreach ($employees as $emp) {
        // Connection Rate: Outbound Connected / Total Outbound
        $outboundTotal = (int)$emp['outbound_calls'];
        $outboundConnected = (int)$emp['outbound_connected'];
        $connectionRate = $outboundTotal > 0 ? round(($outboundConnected / $outboundTotal) * 100, 1) : 0;
        
        $aht = (float)$emp['avg_handle_time'];

        $performanceData[] = [
            'employee_id' => $emp['id'],
            'name' => $emp['name'],
            'total_calls' => (int)$emp['total_calls'],
            'inbound' => (int)$emp['inbound_calls'],
            'outbound' => (int)$emp['outbound_calls'],
            'outbound_connected' => $outboundConnected,
            'connection_rate' => $connectionRate,
            'total_duration_sec' => (int)$emp['total_duration'],
            'formatted_duration' => formatDuration((int)$emp['total_duration']),
            'avg_handle_time_sec' => round($aht),
            'formatted_aht' => formatDuration(round($aht))
        ];
    }
    
    $response['employee_performance'] = $performanceData;
}

// ---------------------------------------------------------
// 2. Missed Call / Opportunity Report
// ---------------------------------------------------------
if ($reportType === 'all' || $reportType === 'missed_opportunities') {
    
    // Get missed inbound calls
    // Optimally, we would check if they were called back.
    // For "Unreturned", we look for an outbound call to the same number AFTER the missed call time.
    
    $missedCalls = Database::select("
        SELECT 
            c.id,
            c.caller_phone,
            c.caller_name,
            c.call_time,
            c.employee_id,
            e.name as employee_name
        FROM calls c
        LEFT JOIN employees e ON c.employee_id = e.id
        WHERE c.org_id = '$orgId' 
        AND (c.type = 'Inbound' OR c.type = 'Incoming') 
        AND c.duration = 0 
        $dateFilter
        ORDER BY c.call_time DESC
        LIMIT 100
    ");
    
    $analysis = [];
    foreach ($missedCalls as $missed) {
        $phone = $missed['caller_phone'];
        $missedTime = $missed['call_time'];
        
        // Check for callback
        // Look for ANY outbound call to this phone number that happened AFTER the missed call
        $callback = Database::getOne("
            SELECT call_time, employee_id, duration 
            FROM calls 
            WHERE org_id = '$orgId' 
            AND caller_phone = '$phone' 
            AND (type = 'Outbound' OR type = 'Outgoing')
            AND call_time > '$missedTime'
            LIMIT 1
        ");
        
        $status = 'Unreturned';
        $callbackTime = null;
        
        if ($callback) {
            $status = 'Returned';
            $callbackTime = $callback['call_time'];
        }
        
        $analysis[] = [
            'id' => $missed['id'],
            'phone' => $phone,
            'name' => $missed['caller_name'] ?: 'Unknown',
            'missed_at' => $missed['call_time'],
            'missed_at_formatted' => date('M j, g:i a', strtotime($missed['call_time'])),
            'originally_routed_to' => $missed['employee_name'] ?: 'Unknown',
            'status' => $status,
            'callback_time' => $callbackTime
        ];
    }
    
    $response['missed_opportunities'] = $analysis;
}

// ---------------------------------------------------------
// 5. Operational Insights
// ---------------------------------------------------------
if ($reportType === 'all' || $reportType === 'operational_insights') {
    
    // Peak Activity Times (Heatmap style logic)
    // Group by Day of Week and Hour
    // We'll just return data suitable for a heatmap: [{day: 1, hour: 14, count: 5}, ...]
    
    // Note: DAYOFWEEK returns 1 for Sunday, 2 for Monday...
    $activity = Database::select("
        SELECT 
            DAYOFWEEK($sqlLocalTime) as day_num,
            HOUR($sqlLocalTime) as hour_num,
            COUNT(*) as call_volume
        FROM calls
        WHERE org_id = '$orgId' $dateFilter
        GROUP BY day_num, hour_num
        ORDER BY day_num, hour_num
    ");
    
    // Device Health Status
    // Employees whose last_sync is old (> 24 hours) are "Needs Attention"
    $deviceHealth = Database::select("
        SELECT 
            name, 
            phone,
            last_sync,
            device_id
        FROM employees
        WHERE org_id = '$orgId' AND status = 'active'
    ");
    
    $healthData = [];
    $now = new DateTime("now", new DateTimeZone("UTC")); // Server time matches DB UTC usually
    
    foreach ($deviceHealth as $dev) {
        $status = 'Healthy';
        $lastSyncStr = $dev['last_sync'];
        $timeDiff = 'Never';
        $hours_ago = 9999;
        
        if ($lastSyncStr) {
            $lastSync = new DateTime($lastSyncStr);
            $diff = $now->diff($lastSync);
            $totalHours = $diff->h + ($diff->days * 24);
            
            if ($totalHours > 24) {
                 $status = 'Offline'; // > 24 hours
            } else if ($totalHours > 1) {
                 $status = 'Away'; // > 1 hour
            } else {
                 $status = 'Online'; // < 1 hour
            }
            
            // Format time ago
            if ($diff->days > 0) $timeDiff = $diff->days . 'd ago';
            else if ($diff->h > 0) $timeDiff = $diff->h . 'h ago';
            else $timeDiff = $diff->i . 'm ago';
            
            $hours_ago = $totalHours;
        } else {
            $status = 'Never Synced';
            $hours_ago = 99999;
        }
        
        $healthData[] = [
            'name' => $dev['name'],
            'status' => $status,
            'last_active' => $timeDiff,
            'last_sync_raw' => $lastSyncStr,
            'device_id' => $dev['device_id']
        ];
    }
    
    // Sort health data: Offline/Never first
    usort($healthData, function($a, $b) {
        // Define priority: Offline(1), Never Synced(2), Away(3), Online(4)
        $pA = ($a['status'] == 'Offline') ? 1 : (($a['status'] == 'Never Synced') ? 2 : (($a['status'] == 'Away') ? 3 : 4));
        $pB = ($b['status'] == 'Offline') ? 1 : (($b['status'] == 'Never Synced') ? 2 : (($b['status'] == 'Away') ? 3 : 4));
        return $pA <=> $pB;
    });

    $response['operational_insights'] = [
        'activity_heatmap' => $activity,
        'device_health' => $healthData
    ];
}

// Summary Metrics (Generic, useful for top cards)
if ($reportType === 'all' || $reportType === 'summary') {
    $summary = Database::getOne("
        SELECT 
            COUNT(*) as total_calls,
            SUM(duration) as total_duration,
            SUM(CASE WHEN duration > 0 THEN 1 ELSE 0 END) as connected,
            SUM(CASE WHEN duration = 0 THEN 1 ELSE 0 END) as missed
        FROM calls 
        WHERE org_id = '$orgId' $dateFilter
    ");
    
    $summary['formatted_duration'] = formatDuration($summary['total_duration']);
    $response['summary'] = $summary;
}

Response::success($response);
