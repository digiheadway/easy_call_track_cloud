<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);

$DB = [
    "host" => "localhost",
    "user" => "u542940820_easycalls",
    "pass" => "v7D5;Xsz!~I",
    "db"   => "u542940820_easycalls"
];

$conn = new mysqli($DB['host'], $DB['user'], $DB['pass'], $DB['db']);
if ($conn->connect_error) die("DB Failed");

$org_id = "UPTOWN";
$employee_id = 2;

$stmt = $conn->prepare("SELECT id, org_id, device_id, name, allow_personal_exclusion, allow_changing_tracking_start_date, allow_updating_tracking_sims, default_tracking_starting_date FROM employees WHERE id = ? AND org_id = ?");
if (!$stmt) die("Prepare failed: " . $conn->error);

$stmt->bind_param("is", $employee_id, $org_id);
$stmt->execute();
$res = $stmt->get_result();

echo "Rows found: " . $res->num_rows . "\n";

if ($res->num_rows > 0) {
    $employee = $res->fetch_assoc();
    print_r($employee);
} else {
    echo "No match for id=$employee_id and org_id=$org_id\n";
    
    // Check all employees to see what the values actually are
    $res2 = $conn->query("SELECT id, org_id FROM employees");
    while($row = $res2->fetch_assoc()) {
        echo "Found in DB: ID=" . $row['id'] . " Org=" . $row['org_id'] . "\n";
    }
}
