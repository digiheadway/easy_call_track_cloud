<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);
echo "Hello from PHP " . phpversion();
$conn = new mysqli("localhost", "u542940820_easycalls", "v7D5;Xsz!~I", "u542940820_easycalls");
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}
echo " | DB Connected";
?>
