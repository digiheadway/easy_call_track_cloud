<?php

// error_reporting(E_ALL);
// ini_set('display_errors', 1);

// Database connection parameters
include('mydb.php');
// Create a database connection
$conn = new mysqli($servername, $username, $password, $database);

// Check connection
if ($conn->connect_error) {
   // die("Connection failed: " . $conn->connect_error);
}


// Check if the query already exists in the 'queries' table
$checkQuery = "SELECT * FROM $tableName WHERE query = ?";
$stmtCheck = $conn->prepare($checkQuery);
$stmtCheck->bind_param("s", $query);
$stmtCheck->execute();
$result = $stmtCheck->get_result();

if ($result->num_rows > 0) {
   // Update 'hits' and 'imageUrl' columns in one query
   $updateQuery = "UPDATE $tableName SET hits = hits + 1 WHERE query = ?";
   $stmtUpdate = $conn->prepare($updateQuery);
   $stmtUpdate->bind_param("s", $query);
   $stmtUpdate->execute();
   // echo "Query already exists. Hits incremented.";
} 

// Close the statements and database connection
$stmtCheck->close();
$stmtUpdate->close();
$conn->close();
?>
