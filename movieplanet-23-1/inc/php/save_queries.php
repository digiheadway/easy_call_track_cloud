<?php

// Create a database connection
$conn = new mysqli($servername, $username, $password, $database);

// Check connection
if ($conn->connect_error) {
   // die("Connection failed: " . $conn->connect_error);
}

// Your query and image URL values
$searchquery = trim(strtolower($_GET['q']));
$query = urldecode($searchquery);


// Check if the query already exists in the 'queries' table
$checkQuery = "SELECT * FROM queries WHERE query = ?";
$stmtCheck = $conn->prepare($checkQuery);
$stmtCheck->bind_param("s", $query);
$stmtCheck->execute();
$result = $stmtCheck->get_result();

if ($result->num_rows > 0) {
   // Update 'hits' and 'imageUrl' columns in one query
   $updateQuery = "UPDATE queries SET hits = hits + 1, imageUrl = ?,approved = Null WHERE query = ?";
   $stmtUpdate = $conn->prepare($updateQuery);
   $stmtUpdate->bind_param("ss", $imageUrl, $query);
   $stmtUpdate->execute();
   $stmtUpdate->close();

   // echo "Query already exists. Hits incremented.";
} else {
   // If the query doesn't exist, insert it into the 'queries' table
   $insertQuery = "INSERT INTO queries (query, imageUrl, hits) VALUES (?, ?, 1)";
   $stmtInsert = $conn->prepare($insertQuery);
   $stmtInsert->bind_param("ss", $query, $imageUrl);

   if ($stmtInsert->execute()) {
      // echo "Data inserted successfully into 'queries' table.";
   } else {
      // echo "Error inserting data: " . $stmtInsert->error;
   }
   $stmtInsert->close();
}

// Close the statements and database connection
$stmtCheck->close();
$conn->close();
?>
