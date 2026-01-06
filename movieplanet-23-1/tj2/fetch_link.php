<?php
$servername = "localhost";
$username = "fmyfzvvwud";
$password = "MG5xCnA8Pt";
$dbname = "fmyfzvvwud";

// Create connection
$conn = new mysqli($servername, $username, $password, $dbname);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// Set the current timestamp
$current_time = date("Y-m-d H:i:s");

// Step 1: Reset reserved links that have not been used for more than 1 hour
$resetSql = "UPDATE webhook_link_ids SET is_reserved = 0 WHERE is_reserved = 1 AND is_used = 0 AND TIMESTAMPDIFF(HOUR, reserved_time, ?) > 1";
$stmt = $conn->prepare($resetSql);
$stmt->bind_param("s", $current_time);
$stmt->execute();
$stmt->close();

// Step 2: Fetch an unused and non-reserved link_id
$sql = "SELECT link_id FROM webhook_link_ids WHERE is_used = 0 AND is_reserved = 0 LIMIT 1";
$result = $conn->query($sql);

if ($result->num_rows > 0) {
    // Get the link_id
    $row = $result->fetch_assoc();
    $link_id = $row['link_id'];

    // Mark the link_id as reserved and store the current time as reserved_time
    $updateSql = "UPDATE webhook_link_ids SET is_reserved = 1, reserved_time = ? WHERE link_id = ?";
    $stmt = $conn->prepare($updateSql);
    $stmt->bind_param("ss", $current_time, $link_id);
    $stmt->execute();

    // Return the link_id to the user
    echo json_encode(["success" => true, "link_id" => $link_id]);

    $stmt->close();
} else {
    echo json_encode(["success" => false, "message" => "No available link ID found."]);
}

$conn->close();
?>
