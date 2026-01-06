<?php
// Enable error reporting to display errors and warnings
error_reporting(E_ALL);
ini_set('display_errors', 1);

// Database connection parameters
include ('php/mydb.php');

// Create a connection
$conn = new mysqli($servername, $username, $password, $database);

// Check the connection
if ($conn->connect_error) {
    die ("Connection failed: " . $conn->connect_error);
}

// SQL query to fetch images in descending order by priority
$sql = "SELECT * FROM images ORDER BY priority DESC Limit 16";

// Execute the query
$result = $conn->query($sql);

// Initialize a counter for the 'movie' value
$movieCounter = 0;

// Check if there are results
if ($result->num_rows > 0) {
    while ($row = $result->fetch_assoc()) {
        // Increment the counter
        $movieCounter++;

        // Display the image using the HTML structure you provided
        echo '<a class="category_list" href="' . $func_search_url_prefix . $row['query'] . '" onclick="record(\'popular_movie_click\', \'image' . $movieCounter . '\');" class="movies_item_box">';
        echo '<span class="catogry_list_label">'. $row['anchor']. '</span>';
        echo '<span><img src="/assets/img/arrow-right.svg" alt=""></span></a>';
    }
} else {
    echo "No featured images found.";
}

// Close the database connection
$conn->close();
?>