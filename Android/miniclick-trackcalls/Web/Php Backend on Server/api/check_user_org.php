<?php
$conn = new mysqli("localhost", "u542940820_easycalls", "v7D5;Xsz!~I", "u542940820_easycalls");
$res = $conn->query("SELECT org_id, company_name FROM users WHERE org_id = 'uptown'");
echo json_encode($res->fetch_all(MYSQLI_ASSOC));
?>
