<?php
// Database connection parameters
include('mydb.php');

// Create a database connection
$conn = new mysqli($servername, $username, $password, $database);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

$searchquery = trim(strtolower($_GET['q']));
$query = urldecode($searchquery);

// Prepare the SQL statement
$stmt = $conn->prepare("SELECT imageUrl FROM images WHERE query = ?");
$stmt->bind_param("s", $query);

// Execute the query
$stmt->execute();

// Bind the result to a variable
$stmt->bind_result($img);

// Fetch the result
$stmt->fetch();
// Close the statement and database connection
$stmt->close();

// Store hit value in db


if ($img) {
    echo json_encode(array('imageUrl' => $img));
    setcookie('from', 'data1', time() + 360000); // Cache for 1 hour
    $tableName = "images";
    include('record_hits_for_find_in_db.php');
    die();
}

// quering queries table approved only


// Prepare the SQL statement
$stmt = $conn->prepare("SELECT imageUrl FROM queries WHERE approved is true and query = ?");
$stmt->bind_param("s", $query);

// Execute the query
$stmt->execute();

// Bind the result to a variable
$stmt->bind_result($img1);

// Fetch the result
$stmt->fetch();
// Close the statement and database connection
$stmt->close();
$conn->close();

if ($img1) {
    echo json_encode(array('imageUrl' => $img1));
    setcookie('from', 'data2', time() + 360000); // Cache for 1 hour
    $tableName = "queries";
    include('record_hits_for_find_in_db.php');
    die();
}
?>
