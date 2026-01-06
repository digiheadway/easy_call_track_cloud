<?php

//  error_reporting(E_ALL);
//  ini_set('display_errors', 1);
header("Access-Control-Allow-Origin: *");

// Getting Ready Api Request
$searchquery = trim(strtolower($_GET['q']));

$query = urlencode($searchquery);
setcookie('q', $query, time() + 360000); // Cache for 1 hour

// Check if image is already cached in a cookie
if (isset($_COOKIE[$query])) {
    $imageUrl = $_COOKIE[$query];
    echo json_encode(array('imageUrl' => $imageUrl));
    setcookie('from', 'cookies', time() + 360000); // Cache for 1 hour
    die();
}
include('save_hostname.php');

// Skipping Api request if featured img click
include('find_in_db.php');

$used_at = 'all'; // set the parameter value
include('find_in_unapproved_queries.php'); // query in unapprooved images
$used_at = null; // Set $used_at to null

// Create connection
$conn = new mysqli($servername, $username, $password, $database);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}


// Cycle through the API keys ordered by requests_made in ascending order
$sql = "SELECT * FROM api_keys WHERE status NOT IN ('exhausted', 'blocked') ORDER BY requests_made ASC";
$result = $conn->query($sql);

// Check the number of rows
if ($result->num_rows < 10) {
    $log_the_event = "INSERT INTO extra_info (value) VALUES ('Active Api Less then " . $result->num_rows . "')";
    // $log_the_event = "INSERT INTO extra_info (value) VALUES ('Active Api Less then 10')";
    $log_the_event_result = $conn->query($log_the_event);

    $reset_exhausted_status = "UPDATE api_keys SET status = 'restored_20_MIN' WHERE status = 'exhausted' AND TIMESTAMPDIFF(Minute, update_timestamp, NOW()) > 20";
    $reset_exhausted_status_result = $conn->query($reset_exhausted_status);

    include('find_in_unapproved_queries.php'); // query in unapprooved images
}

// Define the search engine ID
$searchEngineId = '7117b921e333a4c36';
// Getting Ready Api Request
$query = urlencode($searchquery);
$query_suffix = "movie+or+web+series+full+hd+poster";

while ($row = $result->fetch_assoc()) {
    $apiKey = $row["api_key"];

    // Make API request
    $url = "https://www.googleapis.com/customsearch/v1?key=$apiKey&cx=$searchEngineId&q=$query+$query_suffix&searchType=image";
    $response = file_get_contents($url);
    $responseData = json_decode($response, true);
    setcookie('ad_gauid', 'gfgfbAA' . $apiKey . 'BBdfvdfv', time() + 300); // Cache for 5 min

    if (isset($responseData['items'][0]['link'])) {
        // If result found
        $imageUrl = $responseData['items'][0]['link'];
        setcookie($query, $imageUrl, time() + 360000); // Cache for 1 hour
        setcookie('_gauid', 'gfgfbFF' . $apiKey . 'DDfvdfv', time() + 300); // Cache for 5 min

        // Update requests_made and status in the database
        $conn->query("UPDATE api_keys SET requests_made = requests_made + 1, status = 'active' WHERE api_key = '$apiKey'");

        echo json_encode(array('imageUrl' => $imageUrl));
        setcookie('from', 'api', time() + 360000); // Cache for 1 hour
        include('save_queries.php');
        break; // Exit the loop if result is found

        // die if found the url
    } elseif ($http_response_header[0] == "HTTP/1.1 429 Too Many Requests") {
        // If response code is 429
        $conn->query("UPDATE api_keys SET status = 'exhausted' WHERE api_key = '$apiKey'");
    } elseif (isset($responseData['searchInformation']['totalResults']) && $responseData['searchInformation']['totalResults'] === '0') {
        // If totalResults is 0
        setcookie('results', '0 Results', time() + 360000); // Cache for 1 hour
        $conn->query("UPDATE api_keys SET requests_made = requests_made + 1, status = 'active' WHERE api_key = '$apiKey'");
    } elseif ($http_response_header[0] == "HTTP/1.1 400 Bad Request") {
        // If totalResults is 0
        setcookie('req', '400 bad', time() + 360000); // Cache for 1 hour
        $conn->query("UPDATE api_keys SET requests_made = requests_made + 1, status = 'blocked' WHERE api_key = '$apiKey'");
        break; // Exit the loop if result is found

    } elseif (isset($responseData['spelling']) && isset($responseData['spelling']['correctedQuery'])) {
        // Corrected query exists
        $correctedQuery = $responseArray['spelling']['correctedQuery'];
        // Subtract the query suffix from the corrected query
        $correctedQuery = str_replace($query_suffix, '', $correctedQuery);
        $correctedQuery = trim($correctedQuery);
        // $conn->query("UPDATE queries SET correct = '$correctedQuery' WHERE query = '$query'");
        echo json_encode(array('imageUrl' => 'https://upload.wikimedia.org/wikipedia/commons/b/bc/Refresh_icon.png'));
    } else {
        // If response is not satisfying
        $stmt = $conn->prepare("INSERT INTO extra_info (value) VALUES (?)");
        // Encode the JSON data
        $jsonData = json_encode($responseData);
        // Bind the parameter by reference
        $stmt->bind_param("s", $jsonData);
        $stmt->execute();
        $stmt->close();
    }
}



if (!isset($imageUrl)) {
    include('find_in_unapproved_queries.php'); // query in unapprooved images
    $no_result_found = $conn->query("INSERT INTO extra_info (value) VALUES ('No Result Found: $query')");
    $conn->close();
    echo json_encode(array('imageUrl' => '/assets/img/not-found.jpg'));
    setcookie('from', 'error_handler', time() + 360000); // Cache for 1 hour
    die();
}

die();