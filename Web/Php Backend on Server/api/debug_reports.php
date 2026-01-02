<?php
require_once '../config.php';
require_once '../utils.php';

header('Content-Type: text/plain');

echo "Debug Report Data\n";
echo "=================\n";

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}
echo "DB Connected successfully.\n\n";

// 1. Check Total Calls
$total = Database::getOne("SELECT COUNT(*) as c FROM calls");
echo "Total Calls in DB: " . $total['c'] . "\n";

// 2. Check Date Range of Calls
$dates = Database::getOne("SELECT MIN(call_time) as first, MAX(call_time) as last FROM calls");
echo "First Call: " . $dates['first'] . "\n";
echo "Last Call: " . $dates['last'] . "\n";
echo "Current Server Time (UTC): " . gmdate('Y-m-d H:i:s') . "\n";
echo "Current Server Time (Local): " . date('Y-m-d H:i:s') . "\n\n";

// 3. Check Org IDs
echo "Org IDs in calls table:\n";
$orgs = Database::select("SELECT org_id, COUNT(*) as c FROM calls GROUP BY org_id");
foreach ($orgs as $o) {
    echo " - Org: " . $o['org_id'] . " (Count: " . $o['c'] . ")\n";
}
echo "\n";

// 4. Check Employees
echo "Active Employees:\n";
$emps = Database::select("SELECT id, name, org_id FROM employees WHERE status='active'");
foreach ($emps as $e) {
    echo " - ID: " . $e['id'] . ", Name: " . $e['name'] . ", Org: " . $e['org_id'] . "\n";
}

?>
