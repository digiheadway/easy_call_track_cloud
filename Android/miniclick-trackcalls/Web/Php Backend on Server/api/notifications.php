<?php
/* =====================================
   CallCloud Admin - Notifications API
   ===================================== */

// Handle CORS
if (isset($_SERVER['HTTP_ORIGIN'])) {
    header("Access-Control-Allow-Origin: {$_SERVER['HTTP_ORIGIN']}");
    header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
    header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");
    header("Access-Control-Allow-Credentials: true");
} else {
    header("Access-Control-Allow-Origin: *");
}

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once '../config.php';
require_once '../utils.php';

header('Content-Type: application/json');

// Require authentication
$currentUser = Auth::requireAuth();
$orgId = $currentUser['org_id'];

// Lazy initialization of notifications table
$createTableSql = "CREATE TABLE IF NOT EXISTS notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    org_id VARCHAR(50) NOT NULL,
    type VARCHAR(20) DEFAULT 'info',
    title VARCHAR(255) NOT NULL,
    message TEXT,
    is_read TINYINT(1) DEFAULT 0,
    link VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX (org_id),
    INDEX (is_read),
    INDEX (created_at)
)";
Database::execute($createTableSql);

// Get request data
$data = json_decode(file_get_contents('php://input'), true);
$action = $_GET['action'] ?? $data['action'] ?? 'get';

if ($action === 'get') {
    $limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 50;
    $offset = isset($_GET['offset']) ? (int)$_GET['offset'] : 0;
    
// Fetch notifications
$notifications = Database::select("
    SELECT * FROM notifications 
    WHERE org_id = '$orgId' 
    ORDER BY created_at DESC 
    LIMIT $limit OFFSET $offset
");

// If no notifications exist, create a welcome one
if (empty($notifications) && $offset === 0) {
    Database::execute("
        INSERT INTO notifications (org_id, type, title, message, link) 
        VALUES ('$orgId', 'info', 'Welcome to CallCloud!', 'We hope you enjoy using our platform. Start by exploring your dashboard and calls.', '/dashboard')
    ");
    // Re-fetch
    $notifications = Database::select("
        SELECT * FROM notifications 
        WHERE org_id = '$orgId' 
        ORDER BY created_at DESC 
        LIMIT $limit OFFSET $offset
    ");
}
    
    // Get unread count
    $unreadCount = Database::getOne("
        SELECT COUNT(*) as count FROM notifications 
        WHERE org_id = '$orgId' AND is_read = 0
    ")['count'] ?? 0;
    
    Response::success([
        'notifications' => $notifications,
        'unread_count' => (int)$unreadCount
    ]);
}

if ($action === 'mark_read') {
    $id = $data['id'] ?? null;
    
    if ($id === 'all') {
        Database::execute("UPDATE notifications SET is_read = 1 WHERE org_id = '$orgId'");
    } elseif ($id) {
        $id = (int)$id;
        Database::execute("UPDATE notifications SET is_read = 1 WHERE id = $id AND org_id = '$orgId'");
    } else {
        Response::error("Notification ID required");
    }
    
    Response::success([], "Marked as read");
}

if ($action === 'delete') {
    $id = $data['id'] ?? null;
    
    if (!$id) {
        Response::error("Notification ID required");
    }
    
    $id = (int)$id;
    Database::execute("DELETE FROM notifications WHERE id = $id AND org_id = '$orgId'");
    
    Response::success([], "Deleted successfully");
}

Response::error("Invalid Action");
