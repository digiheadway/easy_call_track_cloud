<?php
/**
 * Find Image in Database
 * Checks 'images' and 'queries' tables for existing image URLs
 */

require_once dirname(__DIR__) . '/core/db.php';

$searchquery = trim(strtolower($_GET['q'] ?? ''));
$query = urldecode($searchquery);

if (empty($query)) {
    return;
}

// 1. Check 'images' table
$imgData = dbFetchOne("SELECT imageUrl FROM images WHERE query = ?", "s", [$query]);

if ($imgData && !empty($imgData['imageUrl'])) {
    echo json_encode(['imageUrl' => $imgData['imageUrl']]);
    setcookie('from', 'data1', time() + 360000); 
    
    // Record hit
    recordHit("images", $query);
    die();
}

// 2. Check 'queries' table (approved only)
$queryData = dbFetchOne("SELECT imageUrl FROM queries WHERE approved = 1 AND query = ?", "s", [$query]);

if ($queryData && !empty($queryData['imageUrl'])) {
    echo json_encode(['imageUrl' => $queryData['imageUrl']]);
    setcookie('from', 'data2', time() + 360000);
    
    // Record hit
    recordHit("queries", $query);
    die();
}
?>
