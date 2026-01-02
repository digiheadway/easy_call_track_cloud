<?php
$DB = [
    "host" => "localhost",
    "user" => "u542940820_easycalls",
    "pass" => "v7D5;Xsz!~I",
    "db"   => "u542940820_easycalls"
];
$conn = new mysqli($DB['host'], $DB['user'], $DB['pass'], $DB['db']);
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}
$employee_id = 2;
$org_id = "UPTOWN";
$stmt = $conn->prepare("SELECT id, org_id FROM employees WHERE id = ? AND org_id = ?");
$stmt->bind_param("is", $employee_id, $org_id);
$stmt->execute();
$res = $stmt->get_result();
echo "Found: " . $res->num_rows;
