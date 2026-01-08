<?php
// log_flow.php - Tracks installations and app flow events

// Include tracking logic
require_once __DIR__ . '/track.php';

header('Content-Type: application/json');

// Get action type
$action = isset($_GET['action']) ? $_GET['action'] : '';

// Get referrer info
$referrer = isset($_GET['referrer']) ? $_GET['referrer'] : '';

// Parse referrer to get uniqueID (and other params)
$uniqueId = null;
$landing = null;
if ($referrer) {
    parse_str(urldecode($referrer), $params);
    if (isset($params['uniqueid'])) {
        $uniqueId = $params['uniqueid'];
    }
    if (isset($params['landing'])) {
        $landing = $params['landing'];
    }
}

if ($action === 'install' && $uniqueId) {
    // Track installation
    // We assume 'install' action means a new install unless specified otherwise
    // You could pass another param &is_new=false if you want to track re-opens
    $isNew = true; // Default to true for 'install' action
    
    trackInstall($uniqueId, $landing ?: 'unknown', $isNew);
    
    echo json_encode(['status' => 'success', 'message' => 'Install tracked']);
} else {
    // Log other events or handle missing params
    echo json_encode(['status' => 'ignored', 'message' => 'Invalid action or missing uniqueId']);
}
?>
