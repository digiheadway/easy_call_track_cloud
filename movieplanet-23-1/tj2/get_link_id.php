<?php
// Check if the user has a link_id in cookies
if (isset($_COOKIE['link_id'])) {
    echo json_encode(["link_id" => $_COOKIE['link_id']]);
    $conn->close();
    exit;
}

// Database configuration
$host = "localhost";
$username = "fmyfzvvwud";
$password = "MG5xCnA8Pt";
$database = "fmyfzvvwud";

// Establish database connection
$conn = new mysqli($host, $username, $password, $database);

// Check connection
if ($conn->connect_error) {
    http_response_code(500);
    echo json_encode(["error" => "Database connection failed: " . $conn->connect_error]);
    exit;
}

// Query to fetch an unused link_id
$query = "SELECT id, link_id FROM webhook_link_ids WHERE is_used = 0 ORDER BY id ASC LIMIT 1";
$result = $conn->query($query);

if ($result && $result->num_rows > 0) {
    // Fetch the record
    $row = $result->fetch_assoc();
    $id = $row['id'];
    $link_id = $row['link_id'];

    // Set the link_id in the user's cookie
    setcookie('link_id', $link_id, time() + (86400 * 1), "/", "", true, true);

    // Update the record to set is_used = 1
    $updateQuery = "UPDATE webhook_link_ids SET is_used = 1 WHERE id = ?";
    $stmt = $conn->prepare($updateQuery);
    $stmt->bind_param("i", $id);
    $stmt->execute();

    if ($stmt->affected_rows > 0) {
        // Return the link_id as JSON response
        echo json_encode(["link_id" => $link_id]);
    } else {
        http_response_code(500);
        echo json_encode(["error" => "Failed to update record"]);
    }
} else {
    // No unused link_id found
    http_response_code(404);
    echo json_encode(["error" => "No unused link_id found"]);
}

// Close the database connection
$conn->close();
?>
