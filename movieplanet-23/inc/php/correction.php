<?php
// error_reporting(E_ALL);
// ini_set('display_errors', 1);

// Getting Ready Api Request
$query = urldecode($_GET['q']);


// Convert the search term to lowercase
$search_lower = strtolower($query);

// Array of keywords to match
$category_keywords = array("movie", "hindi", "south movie", "bigg boss", "marathi", "adult movies", "scam", "new movie", "wwe", "new movies", "hollywood movies", "hollywood", "2023", "punjabi movie", "punjabi", "hot", "sexy", "kannada", "bollywood", "south", "from", "horror", "tamil", "punjabi movies", "malayalam", "gujarati", "tamil movies", "hindi movie", "telugu", "web series", "ullu web series", "telugu movie", "horror movie", "horror movies","comedy movie","bollywood movies");

// Check if the search term matches any of the category keywords
if (in_array($search_lower, array_map('strtolower', $category_keywords))) {
    header("Location: /category_search.php?q=" . urlencode($search_lower));
    exit;
}

function processQuery($query)
{
    $words_to_remove = array("full", "hd", "watch", "latest", "download");

    // Remove specific words
    $trimmed_query = str_ireplace($words_to_remove, '', $query);

    // Remove non-alphanumeric characters
    $trimmed_query = preg_replace('/[^a-zA-Z0-9 ]/', '', $trimmed_query);

    // Remove extra spaces
    $trimmed_query = preg_replace('/\s+/', ' ', trim($trimmed_query));

    // Trim to the first 50 characters
    $trimmed_query = substr($trimmed_query, 0, 50);

    return $trimmed_query;
}

// Process the query using the function
$trimmed_query = processQuery($query);


include('function.php');
include('mydb.php');

// Parse the URL
$url_parts = parse_url($func_search_url_prefix);

// Check if the query string exists
if (isset($url_parts['query'])) {
    // Parse the query string
    parse_str($url_parts['query'], $query_params);

    // Remove the "q" parameter if it exists
    if (isset($query_params['q'])) {
        unset($query_params['q']);
    }

    // Rebuild the query string
    $new_query = http_build_query($query_params);

    // Rebuild the URL with or without the query string
    $search_url = $url_parts['path'] . '?' . $new_query . "&q=";
    
} else {
    // If there was no query string, use the original URL
    $search_url = $url_parts['path'] . "?q=";
}

if ($query !== $trimmed_query) {
    $url = $search_url . urlencode($trimmed_query);
    header("Location: " . $url);
    die();
}

// Create connection
$conn = new mysqli($servername, $username, $password, $database);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// Use prepared statements to prevent SQL injection
$sql = "SELECT query, correct FROM queries WHERE query = ? AND correct is not null";
$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $query);
$stmt->execute();
$result = $stmt->get_result();

// Fetch the result
$row = $result->fetch_assoc();

if ($row) {
    if ($query != $row['correct']) {
        $url = $search_url . urlencode($row['correct']);
        header("Location: " . $url);
        //echo $url;
        exit();
    }
}

$stmt->close();
$conn->close();
