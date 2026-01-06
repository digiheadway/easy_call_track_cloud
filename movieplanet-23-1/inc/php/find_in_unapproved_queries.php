<?php
// Database connection parameters
include('mydb.php');

// Create a database connection
$conn = new mysqli($servername, $username, $password, $database);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

$query = urldecode($_GET['q']);
$used_at = isset($used_at) ? $used_at : null; // Set $used_at to null if not set

if ($used_at == 'all') {
    // Prepare the SQL statement
    $stmt = $conn->prepare("SELECT imageUrl FROM queries WHERE query = ? AND TIMESTAMPDIFF(hour, timestamp, NOW()) < 24");
}else{
    // Prepare the SQL statement
    $stmt = $conn->prepare("SELECT imageUrl FROM queries WHERE query = ?");
}

$stmt->bind_param("s", $query);


// Execute the query
if (!$stmt->execute()) {
    die("Error executing query: " . $stmt->error);
}

// Store the result
$stmt->store_result();

$num_results = $stmt->num_rows;

if ($num_results > 0) {
    // Bind the result to a variable
    $stmt->bind_result($imageUrl);
    $stmt->fetch();
    $stmt->close();
    echo json_encode(array('imageUrl' => $imageUrl));
    setcookie('from', 'unapproved', time() + 360000); // Cache for 1 hour
    include('save_queries.php');
    die();
}


// Close the connection
// $conn->close();
?>