<?php
/**
 * Find in Unapproved Queries
 * Checks 'queries' table for images even if not approved, with optional 24h limit
 */

require_once dirname(__DIR__) . '/core/db.php';

$query = urldecode($_GET['q'] ?? '');
$used_at = $used_at ?? null;

if (empty($query)) {
    return;
}

if ($used_at === 'all') {
    // Recent only within 24h
    $sql = "SELECT imageUrl FROM queries WHERE query = ? AND TIMESTAMPDIFF(hour, timestamp, NOW()) < 24";
} else {
    // Any time
    $sql = "SELECT imageUrl FROM queries WHERE query = ?";
}

$row = dbFetchOne($sql, "s", [$query]);

if ($row && !empty($row['imageUrl'])) {
    echo json_encode(['imageUrl' => $row['imageUrl']]);
    setcookie('from', 'unapproved', time() + 360000);
    
    // Save/update hits
    include __DIR__ . '/save_queries.php';
    die();
}
?>