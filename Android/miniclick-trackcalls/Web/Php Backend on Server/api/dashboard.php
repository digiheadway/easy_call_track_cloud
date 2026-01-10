<?php
/* =====================================
   CallCloud Admin - Dashboard API
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

// Get Filters
$dateRange = $_GET['dateRange'] ?? '7days';
$employeeId = $_GET['employeeId'] ?? '';
$timezoneOffset = isset($_GET['tzOffset']) ? (int)$_GET['tzOffset'] : 0; 
// timezoneOffset is in minutes (e.g. -330 for IST).
// JS returns inverted offset (UTC - Local). So user in IST (UTC+5:30) gives -330.
// To convert UTC to Local, we Add ( -1 * offset ).
// e.g. UTC + ( -1 * -330 ) = UTC + 330 mins = Local.

$dbOffsetMinutes = -1 * $timezoneOffset;

// Helper to convert DB UTC time to User Local Time in SQL
// This is used for Grouping and Date comparisons
$sqlLocalTime = "DATE_ADD(c.call_time, INTERVAL $dbOffsetMinutes MINUTE)";

// Also need 'Now' in User Local Time for calculating ranges
$sqlLocalNow = "DATE_ADD(UTC_TIMESTAMP(), INTERVAL $dbOffsetMinutes MINUTE)";


// Build Base WHERE array for `calls` table
$where = ["c.org_id = '$orgId'"];

// Exclusion Filter: Hide calls from contacts marked as exclude_from_list
$where[] = "NOT EXISTS (
    SELECT 1 FROM excluded_contacts exc_list 
    WHERE exc_list.phone = c.caller_phone 
    AND exc_list.org_id = c.org_id 
    AND exc_list.exclude_from_list = 1
)";

// Apply Employee Filter
if ($employeeId && $employeeId !== 'all') {
    $empId = (int)$employeeId;
    $where[] = "c.employee_id = $empId";
}

// Apply Date Filter & Calculate Range
// We will generate the date range in PHP (server time) just for the loop, 
// but we need to match the user's perception of "Today".
$endDate = new DateTime("now", new DateTimeZone("UTC")); 
$startDate = new DateTime("now", new DateTimeZone("UTC"));

// Adjust PHP DateTimes to match User's Local Time just for the "Backfill" loop
// Note: This only affects the empty rows generation.
if ($timezoneOffset != 0) {
    // Convert current server time to user time
    // $dbOffsetMinutes is POSITIVE for IST (e.g. 330)
    $intervalSpec = "PT" . abs($dbOffsetMinutes) . "M";
    $interval = new DateInterval($intervalSpec);
    if ($dbOffsetMinutes < 0) $interval->invert = 1;

    $startDate->add($interval);
    $endDate->add($interval);
}

// Reset Start/End to midnight boundaries logic if needed, but for "Last X Days"
// we usually just want the date part.
// Actually, simple subtraction for range is fine.

switch ($dateRange) {
    case 'today':
        // SQL: DATE(LocalTime) = DATE(LocalNow)
        $where[] = "DATE($sqlLocalTime) = DATE($sqlLocalNow)";
        break;
    case 'yesterday':
        $where[] = "DATE($sqlLocalTime) = DATE(DATE_SUB($sqlLocalNow, INTERVAL 1 DAY))";
        $startDate->modify('-1 days');
        $endDate->modify('-1 days');
        break;
    case '3days':
        $where[] = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 3 DAY)";
        $startDate->modify('-2 days'); 
        break;
    case '7days':
        $where[] = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 7 DAY)";
        $startDate->modify('-6 days');
        break;
    case '14days':
        $where[] = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 14 DAY)";
        $startDate->modify('-13 days');
        break;
    case '30days':
        $where[] = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 30 DAY)";
        $startDate->modify('-29 days');
        break;
    case '60days':
        $where[] = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 60 DAY)";
        $startDate->modify('-59 days');
        break;
    case '90days':
        $where[] = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 90 DAY)";
        $startDate->modify('-89 days');
        break;
    case '180days':
        $where[] = "$sqlLocalTime >= DATE_SUB($sqlLocalNow, INTERVAL 180 DAY)";
        $startDate->modify('-179 days');
        break;
    case 'this_week':
        $where[] = "YEARWEEK($sqlLocalTime, 1) = YEARWEEK($sqlLocalNow, 1)";
        $startDate->modify('monday this week');
        break;
    case 'last_week':
        $where[] = "YEARWEEK($sqlLocalTime, 1) = YEARWEEK(DATE_SUB($sqlLocalNow, INTERVAL 1 WEEK), 1)";
        $startDate->modify('monday last week');
        $endDate->modify('sunday last week');
        break;
    case 'this_month':
        $where[] = "DATE_FORMAT($sqlLocalTime, '%Y-%m') = DATE_FORMAT($sqlLocalNow, '%Y-%m')";
        $startDate->modify('first day of this month');
        break;
    case 'last_month':
        $where[] = "DATE_FORMAT($sqlLocalTime, '%Y-%m') = DATE_FORMAT(DATE_SUB($sqlLocalNow, INTERVAL 1 MONTH), '%Y-%m')";
        $startDate->modify('first day of last month');
        $endDate->modify('last day of last month');
        break;
    case 'all_time':
        // No date filter. 
        // We will set startDate dynamically based on data later.
        $isAllTime = true;
        break;
    case 'custom':
        // Expect startDate and endDate in YYYY-MM-DD
        if (isset($_GET['startDate']) && isset($_GET['endDate'])) {
            $s = Database::escape($_GET['startDate']);
            $e = Database::escape($_GET['endDate']);
            $where[] = "DATE($sqlLocalTime) BETWEEN '$s' AND '$e'";
            $startDate = new DateTime($s);
            $endDate = new DateTime($e);
        }
        break;
}

$whereClause = implode(' AND ', $where);

try {
    // 1. Fetch Metrics
    $metrics = Database::getOne("
        SELECT 
            COUNT(*) as total_calls,
            COUNT(DISTINCT c.caller_phone) as total_persons,
            SUM(CASE WHEN c.duration > 0 THEN 1 ELSE 0 END) as connected,
            SUM(CASE WHEN c.duration = 0 THEN 1 ELSE 0 END) as not_connected,
            SUM(c.duration) as total_duration_seconds
        FROM call_log c
        WHERE $whereClause
    ");

    // Format Duration
    $seconds = (int)($metrics['total_duration_seconds'] ?? 0);
    $hrs = floor($seconds / 3600);
    $mins = floor(($seconds % 3600) / 60);
    $secs = $seconds % 60;
    if ($hrs > 0) {
        $metrics['formatted_duration'] = sprintf('%dh %02dm %02ds', $hrs, $mins, $secs);
    } else {
        $metrics['formatted_duration'] = sprintf('%02dm %02ds', $mins, $secs);
    }

    // 2. Fetch Aggregated Table Data (Dates as rows)
    // GROUP BY Local Date
    $dbData = Database::select("
        SELECT 
            DATE($sqlLocalTime) as date,
            COUNT(*) as total_calls,
            SUM(CASE WHEN c.duration > 0 THEN 1 ELSE 0 END) as connected,
            SUM(CASE WHEN c.duration = 0 THEN 1 ELSE 0 END) as not_connected,
            SUM(c.duration) as duration_seconds
        FROM call_log c
        WHERE $whereClause
        GROUP BY DATE($sqlLocalTime)
    ");

    // Process Date Range Filling
    $dataMap = [];
    foreach ($dbData as $row) {
        $dataMap[$row['date']] = $row;
    }

    // Handle All Time Start Date
    if (isset($isAllTime) && $isAllTime) {
        if (!empty($dataMap)) {
            // Find earliest date in data
            $minDate = min(array_keys($dataMap));
            $startDate = new DateTime($minDate); // Already Y-m-d
        } else {
            // Fallback if no data
            $startDate->modify('-30 days');
        }
    }

    $fullBreakdown = [];
    if ($dateRange === 'today') {
        $period = [$endDate]; 
    } else {
        $period = new DatePeriod(
            $startDate,
            new DateInterval('P1D'),
            (clone $endDate)->modify('+1 day') 
        );
    }

    foreach ($period as $dt) {
        $key = $dt->format("Y-m-d"); // User's Date
        
        if (isset($dataMap[$key])) {
            $row = $dataMap[$key];
            // Format duration
            $s = (int)$row['duration_seconds'];
            $m = floor($s / 60);
            $sc = $s % 60;
            $row['formatted_duration'] = sprintf('%dm %02ds', $m, $sc);
            $fullBreakdown[] = $row;
        } else {
            // Empty day
            $fullBreakdown[] = [
                'date' => $key,
                'total_calls' => 0,
                'connected' => 0,
                'not_connected' => 0,
                'duration_seconds' => 0,
                'formatted_duration' => '0m 00s'
            ];
        }
    }
    
    // Reverse to show latest date first
    $breakdown = array_reverse($fullBreakdown);

    // 3. Fetch Active Employees for Dropdown
    $employees = Database::select("SELECT id, name FROM employees WHERE org_id = '$orgId' AND status = 'active'");

    Response::success([
        'metrics' => $metrics,
        'breakdown' => $breakdown,
        'employees' => $employees
    ], 'Dashboard data retrieved');

} catch (Exception $e) {
    Response::error($e->getMessage());
}
