<?php
/**
 * Query Counter API
 * Increments counters for user actions (not_this, down_tried, hits)
 * 
 * Usage: /inc/query-counter.php?q=QUERY&action=not_this|down_tried|hits
 */

require_once __DIR__ . '/db.php';

// Get parameters
$query = isset($_GET['q']) ? urldecode(trim(strtolower($_GET['q']))) : '';
$action = isset($_GET['action']) ? $_GET['action'] : 'hits';

// Validate
if (empty($query)) {
    http_response_code(400);
    echo json_encode(['error' => 'Missing query parameter']);
    exit;
}

// Whitelist allowed actions
$allowedActions = ['hits', 'not_this', 'down_tried'];
if (!in_array($action, $allowedActions)) {
    http_response_code(400);
    echo json_encode(['error' => 'Invalid action']);
    exit;
}

// Increment counter
$success = incrementQueryCounter($action, $query);

// Return result
header('Content-Type: application/json');
echo json_encode([
    'success' => $success,
    'action' => $action,
    'query' => $query
]);
?>
