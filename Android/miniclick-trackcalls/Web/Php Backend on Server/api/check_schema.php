<?php
$conn = new mysqli("localhost", "u542940820_easycalls", "v7D5;Xsz!~I", "u542940820_easycalls");
$res = $conn->query("DESCRIBE employees");
echo json_encode($res->fetch_all(MYSQLI_ASSOC));
?>
