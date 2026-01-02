<?php
/* ============================
   CORS + JSON
============================ */
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");
header("Content-Type: application/json; charset=utf-8");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

/* ============================
   ERRORS (DEV MODE)
============================ */
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

/* ============================
   CONFIG
============================ */
$DB = [
    "host" => "localhost",
    "user" => "u542940820_easycalls",
    "pass" => "v7D5;Xsz!~I",
    "db"   => "u542940820_easycalls"
];

// Updated Base URL for the new server/domain
$BASE_URL = "https://calltrack.mylistings.in/public"; 

// Directory Setup
// Go up one level from 'api' to root, then 'public'
$PUBLIC_DIR = realpath(__DIR__ . "/../public") . "/";
$TMP_DIR    = realpath(__DIR__ . "/../public") . "/tmp_chunks/";

/* ============================
   HELPERS
============================ */
function out($data, $code = 200)
{
    http_response_code($code);
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}

function errorOut($msg, $code = 400)
{
    out(["success" => false, "error" => $msg], $code);
}

function safeCaller($caller)
{
    if (!$caller) return 'unknown';
    // Keep + sign if present, remove other chars? Or just digits?
    // Old code: preg_replace('/[^0-9]/', '', $caller); 
    // Let's keep it safe for file names but preserve + for DB if needed.
    // For file system, best to strip special chars.
    return preg_replace('/[^0-9]/', '', $caller);
}

/* ============================
   DB CONNECT
============================ */
$conn = new mysqli($DB['host'], $DB['user'], $DB['pass'], $DB['db']);
if ($conn->connect_error) {
    errorOut("Database connection failed: " . $conn->connect_error, 500);
}
$conn->set_charset("utf8mb4");

/* ============================
   ACTION
============================ */
$action = $_POST['action'] ?? '';
if (!$action)
    errorOut("Action is required");

/* =====================================================
   0️⃣ VERIFY PAIRING CODE
===================================================== */
if ($action === "verify_pairing_code") {
    $org_id = trim($_POST['org_id'] ?? '');
    $employee_id = trim($_POST['user_id'] ?? ''); // user_id in app maps to employee_id
    $device_id = trim($_POST['device_id'] ?? '');

    if ($org_id === '' || $employee_id === '') {
        errorOut("Pairing code invalid (ORGID and USERID required)");
    }
    if ($device_id === '') {
        errorOut("Device ID required");
    }

    // Check if employee exists
    $stmt = $conn->prepare("SELECT id, org_id, device_id, name, allow_personal_exclusion, allow_changing_tracking_start_date, allow_updating_tracking_sims, default_tracking_starting_date FROM employees WHERE id = ? AND org_id = ?");
    $stmt->bind_param("is", $employee_id, $org_id);
    $stmt->execute();
    $res = $stmt->get_result();

    if ($res->num_rows === 0) {
        errorOut("Employee not found or invalid organization");
    }

    $employee = $res->fetch_assoc();

    // Enforce one employee per device rule
    if ($employee['device_id'] === null || $employee['device_id'] === '') {
        // Device not yet linked to this employee.
        // Check if THIS device is already linked to ANOTHER employee in the same ORG
        $stmt2 = $conn->prepare("SELECT id, name FROM employees WHERE org_id = ? AND device_id = ? AND id != ?");
        $stmt2->bind_param("ssi", $org_id, $device_id, $employee_id);
        $stmt2->execute();
        $res2 = $stmt2->get_result();

        if ($res2->num_rows > 0) {
            $otherEmployee = $res2->fetch_assoc();
            errorOut("This device is already registered to " . $otherEmployee['name'] . " (ID: " . $otherEmployee['id'] . "). Please contact admin.");
        }

        // Link the device to this employee
        $upd = $conn->prepare("UPDATE employees SET device_id = ?, updated_at = NOW() WHERE id = ?");
        $upd->bind_param("si", $device_id, $employee_id);
        if (!$upd->execute()) {
            errorOut("Failed to link device: " . $conn->error, 500);
        }
        out([
            "success" => true,
            "message" => "Pairing successful - Device linked",
            "employee_name" => $employee['name'],
            "settings" => [
                "allow_personal_exclusion" => (int)$employee['allow_personal_exclusion'],
                "allow_changing_tracking_start_date" => (int)$employee['allow_changing_tracking_start_date'],
                "allow_updating_tracking_sims" => (int)$employee['allow_updating_tracking_sims'],
                "default_tracking_starting_date" => $employee['default_tracking_starting_date']
            ]
        ]);
    } else {
        // Employee already has a linked device
        if ($employee['device_id'] === $device_id) {
            out([
                "success" => true,
                "message" => "Device already verified",
                "employee_name" => $employee['name'],
                "settings" => [
                    "allow_personal_exclusion" => (int)$employee['allow_personal_exclusion'],
                    "allow_changing_tracking_start_date" => (int)$employee['allow_changing_tracking_start_date'],
                    "allow_updating_tracking_sims" => (int)$employee['allow_updating_tracking_sims'],
                    "default_tracking_starting_date" => $employee['default_tracking_starting_date']
                ]
            ]);
        } else {
            // Different device - FORCE LOGOUT from old device by switching to new device
            // This effectively logs out the old device
            
            // Check if the NEW device is already linked to another employee
            $stmt3 = $conn->prepare("SELECT id, name FROM employees WHERE org_id = ? AND device_id = ? AND id != ?");
            $stmt3->bind_param("ssi", $org_id, $device_id, $employee_id);
            $stmt3->execute();
            $res3 = $stmt3->get_result();

            if ($res3->num_rows > 0) {
                $otherEmployee = $res3->fetch_assoc();
                errorOut("This new device is already registered to " . $otherEmployee['name'] . " (ID: " . $otherEmployee['id'] . "). Please use a different device.");
            }

            // Switch device - unlink old, link new
            $upd = $conn->prepare("UPDATE employees SET device_id = ?, updated_at = NOW() WHERE id = ?");
            $upd->bind_param("si", $device_id, $employee_id);
            if (!$upd->execute()) {
                errorOut("Failed to switch device: " . $conn->error, 500);
            }
            out([
                "success" => true,
                "message" => "Switched to new device - Previous device logged out",
                "employee_name" => $employee['name'],
                "settings" => [
                    "allow_personal_exclusion" => (int)$employee['allow_personal_exclusion'],
                    "allow_changing_tracking_start_date" => (int)$employee['allow_changing_tracking_start_date'],
                    "allow_updating_tracking_sims" => (int)$employee['allow_updating_tracking_sims'],
                    "default_tracking_starting_date" => $employee['default_tracking_starting_date']
                ]
            ]);
        }
    }
}

/* =====================================================
   1️⃣ START CALL
===================================================== */
if ($action === "start_call") {

    $unique_id    = trim($_POST['unique_id'] ?? '');
    $org_id       = trim($_POST['org_id'] ?? '');
    $employee_id  = trim($_POST['user_id'] ?? ''); // user_id from app maps to employee_id
    $device_phone = isset($_POST['device_phone']) ? preg_replace('/[^\d]/', '', $_POST['device_phone']) : null;
    $caller_name  = $_POST['caller_name'] ?? null;
    $caller_phone = isset($_POST['caller']) ? preg_replace('/[^\d]/', '', $_POST['caller']) : null;      // 'caller' from app maps to caller_phone
    $duration     = intval($_POST['duration'] ?? 0);
