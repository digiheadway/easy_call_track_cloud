<?php
/**
 * Main Search API (TMDB/Google Proxy)
 * Manages API keys, caching, and image fetching
 */

header("Access-Control-Allow-Origin: *");
require_once dirname(__DIR__) . '/core/db.php';

// Prepare query
$searchquery = trim(strtolower($_GET['q'] ?? ''));
if (empty($searchquery)) {
    die(json_encode(['error' => 'No query provided']));
}

$query = urlencode($searchquery);
setcookie('q', $query, time() + 360000);

// 1. Check Cookie Cache
if (isset($_COOKIE[$query])) {
    echo json_encode(['imageUrl' => $_COOKIE[$query]]);
    setcookie('from', 'cookies', time() + 360000);
    die();
}

// 2. Log access
logHostname();

// 3. Check Database Cache (Approved images)
include __DIR__ . '/find_in_db.php';

// 4. Check Unapproved Recently (within 24h)
$used_at = 'all'; 
include __DIR__ . '/find_in_unapproved_queries.php';
$used_at = null; 

// 5. Fetch API Keys
$apiKeys = dbFetchAll(
    "SELECT * FROM api_keys WHERE status NOT IN ('exhausted', 'blocked') ORDER BY requests_made ASC"
);

// If running low on keys, try to recover exhausted ones
if (count($apiKeys) < 10) {
    dbQuery("INSERT INTO extra_info (value) VALUES (?)", "s", ["Active Api Less than " . count($apiKeys)]);
    dbQuery("UPDATE api_keys SET status = 'restored_20_MIN' WHERE status = 'exhausted' AND TIMESTAMPDIFF(MINUTE, update_timestamp, NOW()) > 20");
}

$searchEngineId = '7117b921e333a4c36';
$query_suffix = "movie+or+web+series+full+hd+poster";

// 6. Loop through API keys until a result is found
foreach ($apiKeys as $row) {
    $apiKey = $row["api_key"];
    $url = "https://www.googleapis.com/customsearch/v1?key=$apiKey&cx=$searchEngineId&q=" . urlencode($searchquery) . "+$query_suffix&searchType=image";
    
    $response = @file_get_contents($url);
    $responseData = json_decode($response, true);
    
    setcookie('ad_gauid', 'gfgfbAA' . $apiKey . 'BBdfvdfv', time() + 300);

    if (!empty($responseData['items'][0]['link'])) {
        $imageUrl = $responseData['items'][0]['link'];
        setcookie($query, $imageUrl, time() + 360000);
        setcookie('_gauid', 'gfgfbFF' . $apiKey . 'DDfvdfv', time() + 300);

        // Success: increment requests and set status
        dbQuery("UPDATE api_keys SET requests_made = requests_made + 1, status = 'active' WHERE api_key = ?", "s", [$apiKey]);

        echo json_encode(['imageUrl' => $imageUrl]);
        setcookie('from', 'api', time() + 360000);
        
        include __DIR__ . '/save_queries.php';
        exit();
    } 
    
    // Check for errors
    $headerLine = $http_response_header[0] ?? '';
    if (strpos($headerLine, "429") !== false) {
        dbQuery("UPDATE api_keys SET status = 'exhausted' WHERE api_key = ?", "s", [$apiKey]);
    } elseif (isset($responseData['searchInformation']['totalResults']) && $responseData['searchInformation']['totalResults'] === '0') {
        setcookie('results', '0 Results', time() + 360000);
        dbQuery("UPDATE api_keys SET requests_made = requests_made + 1, status = 'active' WHERE api_key = ?", "s", [$apiKey]);
    } elseif (strpos($headerLine, "400") !== false) {
        setcookie('req', '400 bad', time() + 360000);
        dbQuery("UPDATE api_keys SET status = 'blocked' WHERE api_key = ?", "s", [$apiKey]);
        break; 
    } elseif (isset($responseData['spelling']['correctedQuery'])) {
        // Spelling correction found
        echo json_encode(['imageUrl' => 'https://upload.wikimedia.org/wikipedia/commons/b/bc/Refresh_icon.png']);
        exit();
    } else {
        // Unknown error, log it
        dbQuery("INSERT INTO extra_info (value) VALUES (?)", "s", [json_encode($responseData)]);
    }
}

// 7. Final fallback
if (!isset($imageUrl)) {
    include __DIR__ . '/find_in_unapproved_queries.php';
    dbQuery("INSERT INTO extra_info (value) VALUES (?)", "s", ["No Result Found: $searchquery"]);
    
    echo json_encode(['imageUrl' => '/assets/img/not-found.jpg']);
    setcookie('from', 'error_handler', time() + 360000);
    exit();
}
?>