<?php
/**
 * Query Correction & Redirects
 * Cleans user queries and redirects to categories if needed
 */

require_once dirname(__DIR__) . '/core/db.php';
require_once dirname(__DIR__) . '/core/function.php';

$query = urldecode($_GET['q'] ?? '');
if (empty($query)) return;

$search_lower = strtolower($query);

// 1. Category redirects
$category_keywords = [
    "movie", "hindi", "south movie", "bigg boss", "marathi", "adult movies", 
    "scam", "new movie", "wwe", "new movies", "hollywood movies", "hollywood", 
    "2023", "punjabi movie", "punjabi", "hot", "sexy", "kannada", "bollywood", 
    "south", "from", "horror", "tamil", "punjabi movies", "malayalam", 
    "gujarati", "tamil movies", "hindi movie", "telugu", "web series", 
    "ullu web series", "telugu movie", "horror movie", "horror movies", 
    "comedy movie", "bollywood movies"
];

if (in_array($search_lower, $category_keywords)) {
    header("Location: /category_search.php?q=" . urlencode($search_lower));
    exit;
}

/**
 * Clean and normalize query
 */
function processQuery($query) {
    $words_to_remove = ["full", "hd", "watch", "latest", "download"];
    $trimmed = str_ireplace($words_to_remove, '', $query);
    $trimmed = preg_replace('/[^a-zA-Z0-9 ]/', '', $trimmed);
    $trimmed = preg_replace('/\s+/', ' ', trim($trimmed));
    return substr($trimmed, 0, 50);
}

// 2. Query normalization redirect
$trimmed_query = processQuery($query);

// Rebuild search URL helper
function getSearchUrl($prefix, $newQ) {
    $url_parts = parse_url($prefix);
    $query_params = [];
    if (isset($url_parts['query'])) {
        parse_str($url_parts['query'], $query_params);
    }
    unset($query_params['q']);
    $new_query = http_build_query($query_params);
    return $url_parts['path'] . '?' . ($new_query ? $new_query . '&' : '') . 'q=' . urlencode($newQ);
}

if ($query !== $trimmed_query) {
    header("Location: " . getSearchUrl($func_search_url_prefix, $trimmed_query));
    exit;
}

// 3. Database-based correction redirect
$row = dbFetchOne(
    "SELECT correct FROM queries WHERE query = ? AND correct IS NOT NULL", 
    "s", 
    [$query]
);

if ($row && $query !== $row['correct']) {
    header("Location: " . getSearchUrl($func_search_url_prefix, $row['correct']));
    exit;
}
?>
