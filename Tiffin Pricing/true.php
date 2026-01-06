<?php
// Enable HTTPS if not already enabled
if (empty($_SERVER['HTTPS']) || $_SERVER['HTTPS'] === 'off') {
    header("HTTP/1.1 301 Moved Permanently");
    header("Location: https://" . $_SERVER['HTTP_HOST'] . $_SERVER['REQUEST_URI']);
    exit;
}

// Database credentials
$servername = "localhost";
$username = "u240376517_imeal";
$password = "=YIlUR4/gfsvf553FS";
$dbname = "u240376517_imeal";

// Set content type to JSON
header('Content-Type: application/json');

// Receive request from JavaScript and validate input
$input = json_decode(file_get_contents('php://input'), true);

// Check if requestId is set
if (!isset($input['truecaller_request_id'])) {
    http_response_code(400); // Bad Request
    echo json_encode(["status" => "error", "message" => "Missing requestId."]);
    exit;
}

$requestId = $input['truecaller_request_id'];

// Create connection to the database
$conn = new mysqli($servername, $username, $password, $dbname);

// Check connection
if ($conn->connect_error) {
    error_log("Database connection failed: " . $conn->connect_error); // Log error, don't expose it
    http_response_code(response_code: 500); // Internal Server Error
    echo json_encode(["status" => "error", "message" => "Database connection failed."]);
    exit;
}

// Prepare SQL query to fetch user info by requestId
$stmt = $conn->prepare("SELECT request_id, access_token, user_info, endpoint, timestamp FROM truecaller_info WHERE request_id = ?");
$stmt->bind_param("s", $requestId);

// Execute the query
$stmt->execute();
$result = $stmt->get_result();

// Check if user info was found
if ($result->num_rows > 0) {
    // Fetch the row as an associative array
    $userInfo = $result->fetch_assoc();
    $accessToken = $userInfo['access_token']; // Extract access_token from the fetched row
    if ($accessToken == 'not_set') {
        http_response_code(404); // Not Found
        echo json_encode(["status" => "error", "message" => "choosen_manually_fill"]);
        $stmt->close();
        $conn->close();
        exit;
    }

    $userInfo = json_decode($userInfo['user_info'], true); // Decode JSON to array

    $phoneNumber = $userInfo['phoneNumbers'][0];
    $fullName = $userInfo['name']['first'] . " " . $userInfo['name']['last'];
    $countryCode = $userInfo['addresses'][0]['countryCode'];
    $zipcode = $userInfo['addresses'][0]['zipcode'];
    $email = $userInfo['onlineIdentities']['email'];
    $gender = $userInfo['gender'];
    $avatarUrl = $userInfo['avatarUrl'];


    // Send the user info in JSON format
    http_response_code(200); // OK
    echo json_encode([
        "status" => "ok",
        "data" => [
            "phoneNumber" => $phoneNumber,
            "fullName" => $fullName,
            "countryCode" => $countryCode,
            "zipcode" => $zipcode,
            "email" => $email,
            "gender" => $gender,
            "avatarUrl" => $avatarUrl
        ]
    ]);
} else {
    // If no data found for the given requestId
    http_response_code(404); // Not Found
    echo json_encode(["status" => "error", "message" => "No data found for the provided requestId."]);
}

// Close statement and connection
$stmt->close();
$conn->close();
?>