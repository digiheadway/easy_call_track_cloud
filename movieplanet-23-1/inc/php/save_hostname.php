<?php

error_reporting(E_ALL);
ini_set('display_errors', 1);

// Database creds
include ('mydb.php');

if (isset($_SERVER['HTTP_REFERER'])) {
    // Parse the URL to extract the domain
    $referer_url = parse_url($_SERVER['HTTP_REFERER']);
    // Get the domain from the parsed URL
    $domain = $referer_url['host'];
}else{
    $domain = isset($_SERVER['HTTP_HOST']) ? $_SERVER['HTTP_HOST'] : "https://yourdomain.com";
}

if ($domain) {
    // Create connection
    $conn = new mysqli($servername, $username, $password, $database);

    // Check connection
    if ($conn->connect_error) {
        die("Connection failed: " . $conn->connect_error);
    }

    // Prepare and bind
    $stmt = $conn->prepare("INSERT INTO unique_domains (domain_name) VALUES (?) ON DUPLICATE KEY UPDATE request_count = request_count + 1, last_request = CURRENT_TIMESTAMP");
    if ($stmt === false) {
        die("Prepare failed: " . $conn->error);
    }

    $stmt->bind_param("s", $domain);

    // Execute the statement
    if ($stmt->execute() === false) {
        die("Execute failed: " . $stmt->error);
    }

  //  echo "Domain recorded successfully.";

    // Close statement and connection
    $stmt->close();
    $conn->close();
} else {
    // Handle the case where domain could not be determined
  //  echo "Unable to determine the domain of the requesting client.";
}

?>
