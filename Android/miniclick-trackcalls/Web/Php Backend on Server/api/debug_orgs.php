<?php
$conn = new mysqli("localhost", "u542940820_easycalls", "v7D5;Xsz!~I", "u542940820_easycalls");
$res1 = $conn->query("SELECT DISTINCT org_id FROM employees");
$res2 = $conn->query("SELECT DISTINCT org_id FROM users");
$data = [
    "employees_orgs" => $res1->fetch_all(MYSQLI_ASSOC),
    "users_orgs" => $res2->fetch_all(MYSQLI_ASSOC)
];
echo json_encode($data);
?>
