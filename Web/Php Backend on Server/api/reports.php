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
$reportType = $_GET['type'] ?? 'overview';
$dateRange = $_GET['dateRange'] ?? 'week';

// Build date filter
$dateFilter = '';
switch ($dateRange) {
    case 'today':
        $dateFilter = "AND DATE(call_timestamp) = CURDATE()";
        break;
    case 'week':
        $dateFilter = "AND call_timestamp >= DATE_SUB(NOW(), INTERVAL 7 DAY)";
        break;
    case 'month':
        $dateFilter = "AND call_timestamp >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
        break;
    case 'quarter':
        $dateFilter = "AND call_timestamp >= DATE_SUB(NOW(), INTERVAL 90 DAY)";
        break;
    case 'year':
        $dateFilter = "AND call_timestamp >= DATE_SUB(NOW(), INTERVAL 365 DAY)";
        break;
    default:
        $dateFilter = '';
}

switch ($reportType) {
    
    /* ===== OVERVIEW REPORT ===== */
    case 'overview':
        // Key metrics
        $metrics = Database::getOne("
            SELECT 
                COUNT(*) as total_calls,
                SUM(CASE WHEN direction = 'inbound' THEN 1 ELSE 0 END) as inbound_calls,
                SUM(CASE WHEN direction = 'outbound' THEN 1 ELSE 0 END) as outbound_calls,
                SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END) as completed_calls,
                SUM(CASE WHEN status = 'missed' THEN 1 ELSE 0 END) as missed_calls,
                AVG(CASE WHEN status = 'completed' THEN TIME_TO_SEC(duration) ELSE NULL END) as avg_duration,
                (SELECT COUNT(*) FROM recordings WHERE org_id = '$orgId') as recordings_count,
                (SELECT COUNT(*) FROM employees WHERE org_id = '$orgId' AND status = 'active') as active_employees
            FROM calls 
            WHERE org_id = '$orgId' $dateFilter
        ");
        
        // Success rate
        if ($metrics['total_calls'] > 0) {
            $metrics['success_rate'] = round(($metrics['completed_calls'] / $metrics['total_calls']) * 100, 1);
        } else {
            $metrics['success_rate'] = 0;
        }
        
        // Format average duration
        if ($metrics['avg_duration']) {
            $minutes = floor($metrics['avg_duration'] / 60);
            $seconds = $metrics['avg_duration'] % 60;
            $metrics['avg_duration_formatted'] = sprintf('%d:%02d', $minutes, $seconds);
        } else {
            $metrics['avg_duration_formatted'] = '0:00';
        }
        
        // Call trends by day (last 7 days)
        $trends = Database::select("
            SELECT 
                DATE(call_timestamp) as date,
                COUNT(*) as call_count
            FROM calls 
            WHERE org_id = '$orgId' 
            AND call_timestamp >= DATE_SUB(NOW(), INTERVAL 7 DAY)
            GROUP BY DATE(call_timestamp)
            ORDER BY date ASC
        ");
        
        Response::success([
            'metrics' => $metrics,
            'trends' => $trends
        ], 'Overview report generated');
        break;
    
    /* ===== EMPLOYEE PERFORMANCE REPORT ===== */
    case 'employee':
        $employees = Database::select("
            SELECT 
                e.id,
                e.name,
                e.department,
                e.role,
                COUNT(c.id) as total_calls,
                SUM(CASE WHEN c.status = 'completed' THEN 1 ELSE 0 END) as completed_calls,
                AVG(CASE WHEN c.status = 'completed' THEN TIME_TO_SEC(c.duration) ELSE NULL END) as avg_duration,
                (SELECT COUNT(*) FROM recordings r WHERE r.employee_id = e.id) as recordings_count
            FROM employees e
            LEFT JOIN calls c ON e.id = c.employee_id $dateFilter
            WHERE e.org_id = '$orgId' AND e.status = 'active'
            GROUP BY e.id
            ORDER BY total_calls DESC
            LIMIT 10
        ");
        
        // Calculate performance scores
        foreach ($employees as &$employee) {
            $callScore = min(100, ($employee['total_calls'] / 10) * 40);
            $completionScore = $employee['total_calls'] > 0 
                ? ($employee['completed_calls'] / $employee['total_calls']) * 35 
                : 0;
            $recordingScore = min(25, ($employee['recordings_count'] / $employee['total_calls']) * 25);
            
            $employee['score'] = round($callScore + $completionScore + $recordingScore);
            
            // Format average duration
            if ($employee['avg_duration']) {
                $minutes = floor($employee['avg_duration'] / 60);
                $seconds = $employee['avg_duration'] % 60;
                $employee['avg_duration_formatted'] = sprintf('%d:%02d', $minutes, $seconds);
            } else {
                $employee['avg_duration_formatted'] = '0:00';
            }
        }
        
        Response::success($employees, 'Employee performance report generated');
        break;
    
    /* ===== DEPARTMENT BREAKDOWN ===== */
    case 'department':
        $departments = Database::select("
            SELECT 
                e.department,
                COUNT(c.id) as total_calls,
                SUM(CASE WHEN c.status = 'completed' THEN 1 ELSE 0 END) as completed_calls,
                COUNT(DISTINCT e.id) as employee_count
            FROM employees e
            LEFT JOIN calls c ON e.id = c.employee_id $dateFilter
            WHERE e.org_id = '$orgId'
            GROUP BY e.department
            ORDER BY total_calls DESC
        ");
        
        // Calculate total calls for percentage
        $totalCalls = array_sum(array_column($departments, 'total_calls'));
        
        foreach ($departments as &$dept) {
            $dept['percentage'] = $totalCalls > 0 
                ? round(($dept['total_calls'] / $totalCalls) * 100) 
                : 0;
        }
        
        Response::success($departments, 'Department breakdown generated');
        break;
    
    /* ===== CALL ANALYTICS ===== */
    case 'calls':
        // Hourly distribution
        $hourly = Database::select("
            SELECT 
                HOUR(call_timestamp) as hour,
                COUNT(*) as call_count,
                AVG(TIME_TO_SEC(duration)) as avg_duration
            FROM calls 
            WHERE org_id = '$orgId' $dateFilter
            GROUP BY HOUR(call_timestamp)
            ORDER BY hour ASC
        ");
        
        // Day of week distribution
        $weekly = Database::select("
            SELECT 
                DAYNAME(call_timestamp) as day_name,
                DAYOFWEEK(call_timestamp) as day_num,
                COUNT(*) as call_count
            FROM calls 
            WHERE org_id = '$orgId' $dateFilter
            GROUP BY day_name, day_num
            ORDER BY day_num ASC
        ");
        
        // Direction breakdown
        $direction = Database::getOne("
            SELECT 
                SUM(CASE WHEN direction = 'inbound' THEN 1 ELSE 0 END) as inbound,
                SUM(CASE WHEN direction = 'outbound' THEN 1 ELSE 0 END) as outbound
            FROM calls 
            WHERE org_id = '$orgId' $dateFilter
        ");
        
        Response::success([
            'hourly' => $hourly,
            'weekly' => $weekly,
            'direction' => $direction
        ], 'Call analytics generated');
        break;
    
    default:
        Response::error('Invalid report type');
}
