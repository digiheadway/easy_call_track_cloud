<?php
$servername = "localhost";
$username = "fmyfzvvwud";
$password = "MG5xCnA8Pt";
$dbname = "fmyfzvvwud";

// Get the link_id from the request body (should be sent as JSON)
$data = json_decode(file_get_contents("php://input"), true);
$link_id = $data['link_id'] ?? ''; // Check if 'link_id' exists

if (empty($link_id)) {
    echo json_encode(["success" => false, "message" => "Link ID is required"]);
    exit();
}

$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// Update the link as used
$sql = "UPDATE webhook_link_ids SET is_used = 1 WHERE link_id = ?";
$stmt = $conn->prepare($sql);

if ($stmt === false) {
    echo json_encode(["success" => false, "message" => "Failed to prepare statement"]);
    $conn->close();
    exit();
}

$stmt->bind_param("s", $link_id);

if ($stmt->execute()) {
    echo json_encode(["success" => true, "message" => "Link updated successfully"]);
} else {
    echo json_encode(["success" => false, "message" => "Failed to update link"]);
}

$stmt->close();
$conn->close();
?>
