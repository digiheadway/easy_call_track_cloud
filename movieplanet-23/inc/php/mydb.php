<?php
// Database connection parameters
$servername = "localhost"; // Replace with your server name or IP address
$username = "fmyfzvvwud";
$password = "MG5xCnA8Pt";
$database = "fmyfzvvwud";


// // 2nd Server
// $username = "nwbfpgkywc";
// $password = "MZckmJkD5W";
// $database = "nwbfpgkywc";

// Secondary server
$servername2 = "localhost";
$username2 = "nwbfpgkywc";
$password2 = "MZckmJkD5W";
$database2 = "nwbfpgkywc";

// Test primary
$conn = @new mysqli($servername, $username, $password, $database);

if ($conn->connect_error) {
    // Switch to secondary if primary fails
    $servername = $servername2;
    $username = $username2;
    $password = $password2;
    $database = $database2;
}


?>

