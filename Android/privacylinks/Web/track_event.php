<?php
// track_event.php - simple endpoint for ajax tracking
require_once __DIR__ . '/track.php';

$uniqueId = $_GET['uniqueid'] ?? null;
$type = $_GET['type'] ?? 'view';
$landing = $_GET['landing'] ?? '1';

if (!$uniqueId) { die('no_uid'); }

if ($type == 'click') {
    trackClick($uniqueId, $landing);
    echo 'click_tracked';
} elseif ($type == 'view') {
    trackView($uniqueId, $landing);
    echo 'view_tracked';
}
?>
