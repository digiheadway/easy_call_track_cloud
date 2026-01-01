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
    $stmt = $conn->prepare("SELECT id, org_id, device_id, name FROM employees WHERE id = ? AND org_id = ?");
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
            "employee_name" => $employee['name']
        ]);
    } else {
        // Employee already has a linked device
        if ($employee['device_id'] === $device_id) {
            // Same device - already linked
            out([
                "success" => true,
                "message" => "Device already verified",
                "employee_name" => $employee['name']
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
                "employee_name" => $employee['name']
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
    $type         = $_POST['type'] ?? 'unknown';   // Incoming, Outgoing, Missed
    
    // Map type if needed (App sends 'missed', DB might want 'Missed')
    // But DB holds varchar, so case insensitive usually fine, but let's standardize if we want.
    $type = ucfirst($type); 

    $created_at   = date("Y-m-d H:i:s");

    if ($unique_id === '') errorOut("unique_id required");
    if ($org_id === '' || $employee_id === '') errorOut("org_id and user_id required");

    $device_id = trim($_POST['device_id'] ?? '');
    if ($device_id === '') errorOut("device_id required");

    // Verify employee and device
    $stmtVerify = $conn->prepare("SELECT device_id FROM employees WHERE id = ? AND org_id = ?");
    $stmtVerify->bind_param("is", $employee_id, $org_id);
    $stmtVerify->execute();
    $resVerify = $stmtVerify->get_result();
    
    if ($resVerify->num_rows === 0) {
        errorOut("Invalid employee or organization");
    }
    
    $emp = $resVerify->fetch_assoc();
    if ($emp['device_id'] !== $device_id) {
        errorOut("Unauthorized: This device is not linked to this employee account.");
    }

    // Upload status logic
    $upload_status = 'pending';
    // If duration 0 or missed, no recording expected usually
    if ($duration <= 0 || strtolower($type) === 'missed') {
        $upload_status = 'completed';
    }

    // Insert into 'calls' table
    // Note: 'call_time' is the new datetime column. 'created_at' is timestamp default now()
    // We'll use $created_at for call_time.
    
    $stmt = $conn->prepare("
        INSERT INTO calls
        (unique_id, org_id, employee_id, device_phone, caller_name, caller_phone, duration, type, upload_status, call_time)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            device_phone  = VALUES(device_phone),
            caller_name   = VALUES(caller_name),
            caller_phone  = VALUES(caller_phone),
            duration      = VALUES(duration),
            type          = VALUES(type),
            upload_status = VALUES(upload_status),
            call_time     = VALUES(call_time),
            updated_at    = NOW()
    ");

    $stmt->bind_param(
        "ssssssisss",
        $unique_id,
        $org_id,
        $employee_id,
        $device_phone,
        $caller_name,
        $caller_phone,
        $duration,
        $type,
        $upload_status,
        $created_at
    );

    if (!$stmt->execute()) {
        errorOut("DB Error: " . $stmt->error, 500);
    }
    
    // The `after_call_insert` trigger now handles contact upsert and stats automatically.

    out([
        "success" => true,
        "unique_id" => $unique_id,
        "upload_status" => $upload_status,
        "created_ts" => $created_at
    ]);
}

/* =====================================================
   2️⃣ UPLOAD CHUNK
===================================================== */
if ($action === "upload_chunk") {

    if (!isset($_FILES['chunk']))
        errorOut("Chunk file missing");

    $unique_id = $_POST['unique_id'] ?? '';
    $index = intval($_POST['chunk_index'] ?? -1);

    if ($unique_id === '' || $index < 0) {
        errorOut("unique_id or chunk_index missing");
    }

    // Check calls table
    $check = $conn->prepare("SELECT upload_status FROM calls WHERE unique_id=?");
    $check->bind_param("s", $unique_id);
    $check->execute();
    $res = $check->get_result();
    
    if ($res->num_rows === 0) {
        // Optional: create placeholder call if not exists? No, start_call should be called first.
        errorOut("Call not found for this chunk");
    }
    
    $row = $res->fetch_assoc();
    if ($row && $row['upload_status'] === 'completed') {
        errorOut("No recording expected for this call (already completed)");
    }

    $path = $TMP_DIR . $unique_id . "/";
    if (!is_dir($path)) {
        if (!mkdir($path, 0777, true)) {
            errorOut("Failed to create temp directory", 500);
        }
    }

    if (!move_uploaded_file($_FILES['chunk']['tmp_name'], $path . $index)) {
        errorOut("Failed to save chunk", 500);
    }
    
    out(["success" => true, "chunk_saved" => $index]);
}

/* =====================================================
   3️⃣ FINALIZE UPLOAD
===================================================== */
if ($action === "finalize_upload") {

    $unique_id = $_POST['unique_id'] ?? '';
    $total = intval($_POST['total_chunks'] ?? 0);

    if ($unique_id === '' || $total <= 0)
        errorOut("Missing finalize data");

    $stmt = $conn->prepare("
        SELECT caller_phone, upload_status, device_phone, duration, org_id
        FROM calls WHERE unique_id=?
    ");
    $stmt->bind_param("s", $unique_id);
    $stmt->execute();
    $call = $stmt->get_result()->fetch_assoc();

    if (!$call)
        errorOut("Call not found");

    if ($call['upload_status'] === 'completed') {
        out(["success" => true, "message" => "No recording required/Already Done"]);
    }

    $callerSafe = safeCaller($call['caller_phone']);
    $device_phone = safeCaller($call['device_phone'] ?: 'unknown');
    $duration = intval($call['duration']);
    $orgId = $call['org_id'] ?: 'unknown_org';

    // Folder Structure: public/ORG_ID/DEVICE_PHONE/YYYY_MM/YYYYMMDD/
    $monthFolder = date("Y_m");
    $dateFolder = date("Ymd");
    $timeNow = date("Ymd_His");

    $relPath = "$orgId/$device_phone/$monthFolder/$dateFolder/";
    $finalDir = $PUBLIC_DIR . $relPath;
    
    if (!is_dir($finalDir)) {
        if (!mkdir($finalDir, 0777, true)) {
            errorOut("Failed to create recording directory: $finalDir", 500);
        }
    }

    $fileName = "{$callerSafe}_{$timeNow}_{$duration}.mp3";
    $finalPath = $finalDir . $fileName;

    $outFile = fopen($finalPath, "ab");
    if (!$outFile) errorOut("Failed to open output file", 500);
    
    for ($i = 0; $i < $total; $i++) {
        $chunkPath = $TMP_DIR . $unique_id . "/" . $i;
        if (!file_exists($chunkPath)) {
            fclose($outFile);
            errorOut("Missing chunk $i");
        }
        $in = fopen($chunkPath, "rb");
        stream_copy_to_stream($in, $outFile);
        fclose($in);
    }
    fclose($outFile);

    // Cleanup chunks
    array_map('unlink', glob($TMP_DIR . $unique_id . "/*"));
    rmdir($TMP_DIR . $unique_id);

    // URL to access file
    $recording_url = "$BASE_URL/$relPath$fileName";

    $upd = $conn->prepare("
        UPDATE calls
        SET recording_url=?, upload_status='completed', updated_at=NOW()
        WHERE unique_id=?
    ");
    $upd->bind_param("ss", $recording_url, $unique_id);
    $upd->execute();

    out(["success" => true, "recording_url" => $recording_url]);
}

/* =====================================================
   4️⃣ UPDATE NOTES (AND CONTACTS)
===================================================== */
if ($action === "update_note") {

    $unique_id = $_POST['unique_id'] ?? '';
    // App might send 'note' or 'call_note', let's check both
    $note = $_POST['note'] ?? $_POST['call_note'] ?? null; 
    $person_note = $_POST['person_note'] ?? null;

    if ($unique_id === '')
        errorOut("unique_id required");

    // 4.1 Update Call Note
    if ($note !== null) {
        $stmt = $conn->prepare("UPDATE calls SET note=?, updated_at=NOW() WHERE unique_id=?");
        $stmt->bind_param("ss", $note, $unique_id);
        $stmt->execute();
    }

    // 4.2 Update Person Note (in contacts table)
    if ($person_note !== null) {
        // Need to find the phone number associated with this unique_id
        $stmt = $conn->prepare("SELECT caller_phone FROM calls WHERE unique_id=?");
        $stmt->bind_param("s", $unique_id);
        $stmt->execute();
        $res = $stmt->get_result();
        
        if ($res->num_rows > 0) {
            $row = $res->fetch_assoc();
            $phone = $row['caller_phone'];
            
            if ($phone) {
                // Update contacts table (scoped by org_id for safety)
                // We fetch org_id first from the call
                $stmtOrg = $conn->prepare("SELECT org_id FROM calls WHERE unique_id=?");
                $stmtOrg->bind_param("s", $unique_id);
                $stmtOrg->execute();
                $resOrg = $stmtOrg->get_result();
                $orgId = ($resOrg->num_rows > 0) ? $resOrg->fetch_assoc()['org_id'] : null;

                if ($orgId) {
                    $updContact = $conn->prepare("UPDATE contacts SET notes=?, updated_at=NOW() WHERE phone=? AND org_id=?");
                    $updContact->bind_param("sss", $person_note, $phone, $orgId);
                    $updContact->execute();
                }
            }
        }
    }

    out(["success" => true, "message" => "Notes updated"]);
}

/* ============================
   INVALID ACTION
============================ */
errorOut("Invalid action");
?>
