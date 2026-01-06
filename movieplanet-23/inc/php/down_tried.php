<?php

// Database creds
include('mydb.php');
// Create a database connection
$conn = new mysqli($servername, $username, $password, $database);

// Check connection
if ($conn->connect_error) {
   die("Connection failed: " . $conn->connect_error);
}
$searchquery = trim(strtolower($_GET['q']));
$query = urldecode($searchquery);

// Check if the query already exists in the 'queries' table
$checkQuery = "SELECT * FROM queries WHERE query = ?";
$stmtCheck = $conn->prepare($checkQuery);
$stmtCheck->bind_param("s", $query);
$stmtCheck->execute();
$result = $stmtCheck->get_result();

if ($result->num_rows > 0) {
   // Update not_this
   $updateQuery = "UPDATE queries SET down_tried = down_tried + 1 WHERE query = ?";
   $stmtUpdate = $conn->prepare($updateQuery);
   $stmtUpdate->bind_param("s", $query);

   // Execute the update statement
   if ($stmtUpdate->execute()) {
      // echo "Update success";
   } else {
      // echo "Update failed: " . $stmtUpdate->error;
   }
} else {
   // echo "Query not found";
}

// Close the statements and database connection
$stmtCheck->close();
$stmtUpdate->close();
$conn->close();

?>
