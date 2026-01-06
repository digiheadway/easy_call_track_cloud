<?php
/**
 * Save or Update Query
 * Keeps track of search queries and their associated image URLs
 */

require_once dirname(__DIR__) . '/core/db.php';

$searchquery = trim(strtolower($_GET['q'] ?? ''));
$query = urldecode($searchquery);
$imageUrl = $imageUrl ?? ''; // Expecting $imageUrl to be set before include

if (empty($query)) {
    return;
}

// Check if query exists
$exists = dbFetchOne("SELECT query FROM queries WHERE query = ?", "s", [$query]);

if ($exists) {
    // Update hits and imageUrl, reset approval
    dbQuery(
        "UPDATE queries SET hits = hits + 1, imageUrl = ?, approved = NULL WHERE query = ?",
        "ss",
        [$imageUrl, $query]
    );
} else {
    // Insert new query
    dbQuery(
        "INSERT INTO queries (query, imageUrl, hits) VALUES (?, ?, 1)",
        "ss",
        [$query, $imageUrl]
    );
}
?>
