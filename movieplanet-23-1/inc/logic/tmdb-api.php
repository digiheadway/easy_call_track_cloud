<?php
/**
 * TMDB API Proxy (Server-side)
 * Replaces the old /api/ folder. 
 * Provides a clean interface for frontend live search.
 */

header('Content-Type: application/json');
header("Access-Control-Allow-Origin: *");

require_once dirname(__DIR__) . '/core/tmdb-helper.php';

// Get the path from the query string (e.g., trending/all/week or search/multi)
$path = $_GET['path'] ?? '';
$query = $_GET['query'] ?? '';

if (empty($path)) {
    echo json_encode(['error' => 'No path provided']);
    exit;
}

// Construct the TMDB URL
$apiKey = TMDB_API_KEY;
$tmdbBaseUrl = "https://api.themoviedb.org/3/";
$finalUrl = "{$tmdbBaseUrl}{$path}?api_key={$apiKey}";

if (!empty($query)) {
    $finalUrl .= "&query=" . urlencode($query);
}

// Forward other query params if any
foreach ($_GET as $key => $value) {
    if (!in_array($key, ['path', 'query'])) {
        $finalUrl .= "&" . urlencode($key) . "=" . urlencode($value);
    }
}

// Fetch from TMDB
$response = @file_get_contents($finalUrl);

if (!$response) {
    http_response_code(500);
    echo json_encode(['error' => 'Failed to fetch from TMDB']);
    exit;
}

echo $response;
?>
