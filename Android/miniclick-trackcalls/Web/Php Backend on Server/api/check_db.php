<?php
$conn = new mysqli("localhost", "u542940820_easycalls", "v7D5;Xsz!~I", "u542940820_easycalls");
$res = $conn->query("SELECT org_id, id, name, device_id FROM employees WHERE id = 5");
echo json_encode($res->fetch_all(MYSQLI_ASSOC));
?>
