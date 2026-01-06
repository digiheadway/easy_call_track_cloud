<?php
// Enable error reporting for troubleshooting
ini_set('display_errors', 1);
error_reporting(E_ALL);

// Allow requests from any origin
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST");
header("Access-Control-Allow-Headers: Content-Type");

// Database connection details
$servername = "localhost";
$username = "fmyfzvvwud";
$password = "MG5xCnA8Pt";
$dbname = "fmyfzvvwud";

// Create connection
$conn = new mysqli($servername, $username, $password, $dbname);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . htmlspecialchars($conn->connect_error));
}

// Get data from GET request
$surl = isset($_GET['surl']) ? htmlspecialchars($_GET['surl']) : '';
$link_id = isset($_GET['link_id']) ? htmlspecialchars($_GET['link_id']) : '';

// For POST request, process the data from the body
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents('php://input'), true);

    // Check if "text" is present in the POST data
    if (isset($data['text'])) {
        parse_str($data['text'], $params);
        $surl = isset($params['surl']) ? htmlspecialchars($params['surl']) : '';
        $link_id = isset($params['tera_link_id']) ? htmlspecialchars($params['tera_link_id']) : '';
    }
}

// Check if surl and link_id are empty
if (empty($surl) || empty($link_id)) {
    http_response_code(400);
    echo json_encode(["error" => "Missing 'surl' or 'link_id'"]);
    exit;
}

// Check if the combination of surl and link_id already exists
$query = "SELECT COUNT(*) FROM webhook_link_ids WHERE surl = ? AND link_id = ?";
$stmt_check = $conn->prepare($query);
$stmt_check->bind_param("ss", $surl, $link_id);
$stmt_check->execute();
$stmt_check->bind_result($count);
$stmt_check->fetch();
$stmt_check->close();

if ($count > 0) {
    // If a duplicate is found, return a conflict error
    http_response_code(409);
    echo json_encode(["error" => "Duplicate entry: The link_id already exists"]);
    exit;
}

// Insert data into the database if no duplicates are found
$stmt = $conn->prepare("INSERT INTO webhook_link_ids (surl, link_id, created_time, is_used) VALUES (?, ?, ?, ?)");
$created_time = date('Y-m-d H:i:s');
$is_used = 0;
$stmt->bind_param("sssi", $surl, $link_id, $created_time, $is_used);

if ($stmt->execute()) {
    // Data saved successfully
    echo json_encode(["status" => "success", "message" => "Data saved successfully"]);
} else {
    // Handle failure case
    http_response_code(500);  // Internal server error
    echo json_encode(["error" => "Failed to save data"]);
}

// Close resources after handling both success and failure
$stmt->close();
$conn->close();

?>
