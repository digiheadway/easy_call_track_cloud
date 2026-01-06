<?php
// Set CORS headers to allow all origins and methods
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: *");

// Configuration
$apiKey = '5622cafbfe8f8cfe358a29c53e19bba0'; // Replace with your actual API key
$baseUrl = 'https://api.themoviedb.org/3'; // API base URL you are targeting

// Get the current request URL and query parameters
$requestUri = $_SERVER['REQUEST_URI'];
$queryString = $_SERVER['QUERY_STRING'];

// Remove the base path from the request URI to get the endpoint
$endpoint = str_replace('/api', '', strtok($requestUri, '?'));

// Construct the final URL
$finalUrl = "{$baseUrl}{$endpoint}?api_key={$apiKey}&{$queryString}";

// Initialize cURL to forward the request
$ch = curl_init($finalUrl);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $_SERVER['REQUEST_METHOD']); // Forward the request method (GET, POST, etc.)
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Connection: keep-alive',
    'Accept-Encoding: gzip, deflate' // Enable compression
]);
curl_setopt($ch, CURLOPT_FORBID_REUSE, false);
curl_setopt($ch, CURLOPT_FRESH_CONNECT, false);

// Forward headers and any POST data if applicable
foreach (getallheaders() as $key => $value) {
    curl_setopt($ch, CURLOPT_HTTPHEADER, ["$key: $value"]);
}
if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    curl_setopt($ch, CURLOPT_POSTFIELDS, file_get_contents('php://input'));
}

// Execute the request and capture the response
$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

// Send the API response back to the client
http_response_code($httpCode);
echo $response;
?>
