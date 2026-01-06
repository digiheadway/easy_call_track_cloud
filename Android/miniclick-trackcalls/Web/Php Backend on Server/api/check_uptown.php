<?php
$conn = new mysqli("localhost", "u542940820_easycalls", "v7D5;Xsz!~I", "u542940820_easycalls");
$res = $conn->query("SELECT id, name, org_id FROM employees WHERE org_id = 'uptown'");
echo json_encode($res->fetch_all(MYSQLI_ASSOC));
?>
