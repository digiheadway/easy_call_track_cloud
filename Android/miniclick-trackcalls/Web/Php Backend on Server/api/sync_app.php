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
$BASE_URL = "https://api.miniclickcrm.com/public"; 

// Directory Setup
// Go up one level from 'api' to root, then 'public'
$PUBLIC_DIR = dirname(__DIR__) . "/public/";
$TMP_DIR    = dirname(__DIR__) . "/public/tmp_chunks/";

/* ============================
   HELPERS
============================ */
function out($data, $code = 200, $message = "Success")
{
    http_response_code($code);
    // If $data already has success/error keys, we might be in a transition phase.
    // For the "generic" style, we wrap everything in data if it's not already there.
    $response = [
        "success" => true,
        "message" => $message,
        "error" => null,
        "data" => $data
    ];
    echo json_encode($response, JSON_UNESCAPED_UNICODE);
    exit;
}

function errorOut($msg, $code = 400)
{
    http_response_code($code);
    echo json_encode([
        "success" => false,
        "message" => $msg,
        "error" => $msg,
        "data" => null
    ], JSON_UNESCAPED_UNICODE);
    exit;
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

    // Capture device info
    $device_model = trim($_POST['device_model'] ?? '');
    $os_version = trim($_POST['os_version'] ?? '');
    $battery_level = isset($_POST['battery_level']) ? intval($_POST['battery_level']) : null;
    $device_phone = trim($_POST['device_phone'] ?? '');

    // Check if employee exists and get organization plan info
    $stmt = $conn->prepare("
        SELECT 
            e.id, 
            e.org_id, 
            e.device_id, 
            e.name, 
            e.allow_personal_exclusion, 
            e.allow_changing_tracking_start_date, 
            e.allow_updating_tracking_sims, 
            e.default_tracking_starting_date,
            e.call_track,
            e.call_record_crm,
            u.plan_expiry_date,
            u.allowed_storage_gb,
            u.storage_used_bytes
        FROM employees e
        JOIN users u ON e.org_id = u.org_id
        WHERE e.id = ? AND e.org_id = ?
        LIMIT 1
    ");
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
        // If this device is already linked to ANOTHER employee, unlink them first (Takeover)
        $unlink = $conn->prepare("UPDATE employees SET device_id = NULL WHERE org_id = ? AND device_id = ? AND id != ?");
        $unlink->bind_param("ssi", $org_id, $device_id, $employee_id);
        $unlink->execute();

        // Link the device to this employee
        $upd = $conn->prepare("UPDATE employees SET device_id = ?, device_model = ?, os_version = ?, battery_level = ?, updated_at = NOW() WHERE id = ?");
        if (!$upd) errorOut("DB Error (Link Device): " . $conn->error, 500);
        $upd->bind_param("sssii", $device_id, $device_model, $os_version, $battery_level, $employee_id);
        if (!$upd->execute()) {
            errorOut("Failed to link device: " . $conn->error, 500);
        }
        out([
            "employee_name" => $employee['name'],
            "settings" => [
                "allow_personal_exclusion" => (int)$employee['allow_personal_exclusion'],
                "allow_changing_tracking_start_date" => (int)$employee['allow_changing_tracking_start_date'],
                "allow_updating_tracking_sims" => (int)$employee['allow_updating_tracking_sims'],
                "default_tracking_starting_date" => $employee['default_tracking_starting_date'],
                "call_track" => (int)$employee['call_track'],
                "call_record_crm" => (int)$employee['call_record_crm']
            ],
            "plan" => [
                "expiry_date" => $employee['plan_expiry_date'] ?? null,
                "allowed_storage_gb" => (float)($employee['allowed_storage_gb'] ?? 0),
                "storage_used_bytes" => (int)($employee['storage_used_bytes'] ?? 0)
            ]
        ], 200, "Pairing successful - Device linked");
    }
    
    // Employee already has a linked device
    if ($employee['device_id'] === $device_id) {
        // Update device info even if already linked
        $upd = $conn->prepare("UPDATE employees SET device_model = ?, os_version = ?, battery_level = ?, updated_at = NOW() WHERE id = ?");
        if (!$upd) errorOut("DB Error (Update Device Info): " . $conn->error, 500);
        $upd->bind_param("ssii", $device_model, $os_version, $battery_level, $employee_id);
        $upd->execute();

        out([
                "employee_name" => $employee['name'],
                "settings" => [
                    "allow_personal_exclusion" => (int)$employee['allow_personal_exclusion'],
                    "allow_changing_tracking_start_date" => (int)$employee['allow_changing_tracking_start_date'],
                    "allow_updating_tracking_sims" => (int)$employee['allow_updating_tracking_sims'],
                    "default_tracking_starting_date" => $employee['default_tracking_starting_date'],
                    "call_track" => (int)$employee['call_track'],
                    "call_record_crm" => (int)$employee['call_record_crm']
                ],
                "plan" => [
                    "expiry_date" => $employee['plan_expiry_date'] ?? null,
                    "allowed_storage_gb" => (float)($employee['allowed_storage_gb'] ?? 0),
                    "storage_used_bytes" => (int)($employee['storage_used_bytes'] ?? 0)
                ]
            ], 200, "Device already verified");
        } else {
            // Different device - BLOCK (employee must unpair old device first)
            // TESTER BYPASS: Allow uptown-5 to switch freely
            if (strtolower($org_id) === 'uptown' && $employee_id == 5) {
                // Tester account - allow device switch
                $upd = $conn->prepare("UPDATE employees SET device_id = ?, device_model = ?, os_version = ?, battery_level = ?, updated_at = NOW() WHERE id = ?");
                $upd->bind_param("sssii", $device_id, $device_model, $os_version, $battery_level, $employee_id);
                $upd->execute();
                out([
                    "employee_name" => $employee['name'],
                    "settings" => [
                        "allow_personal_exclusion" => (int)$employee['allow_personal_exclusion'],
                        "allow_changing_tracking_start_date" => (int)$employee['allow_changing_tracking_start_date'],
                        "allow_updating_tracking_sims" => (int)$employee['allow_updating_tracking_sims'],
                        "default_tracking_starting_date" => $employee['default_tracking_starting_date'],
                        "call_track" => (int)$employee['call_track'],
                        "call_record_crm" => (int)$employee['call_record_crm']
                    ],
                    "plan" => [
                        "expiry_date" => $employee['plan_expiry_date'] ?? null,
                        "allowed_storage_gb" => (float)($employee['allowed_storage_gb'] ?? 0),
                        "storage_used_bytes" => (int)($employee['storage_used_bytes'] ?? 0)
                    ]
                ], 200, "Tester device switched");
            }
            errorOut("Your account is already paired to another device. Please unpair from the old device first (Settings > Disconnect Organisation) or contact your administrator.", 403);
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
    $call_time_param = $_POST['call_time'] ?? null;
    
    // Map type if needed (App sends 'missed', DB might want 'Missed')
    // But DB holds varchar, so case insensitive usually fine, but let's standardize if we want.
    $type = ucfirst($type); 

    $created_at   = date("Y-m-d H:i:s");
    
    // Use provided call_time if valid, else default to created_at
    $call_time_to_insert = $created_at;
    if ($call_time_param && strtotime($call_time_param)) {
        $call_time_to_insert = $call_time_param;
    }

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
    
    // Update last_sync for the employee
    $updSync = $conn->prepare("UPDATE employees SET last_sync = NOW() WHERE id = ?");
    if (!$updSync) errorOut("DB Error (Update Sync): " . $conn->error, 500);
    $updSync->bind_param("i", $employee_id);
    $updSync->execute();

    // Upload status logic
    $upload_status = $_POST['upload_status'] ?? 'pending';
    // If duration 0 or missed, no recording expected usually, force completed in those cases
    if ($duration <= 0 || strtolower($type) === 'missed') {
        $upload_status = 'completed';
    }

    // Insert into 'calls' table
    
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
        $call_time_to_insert
    );

    if (!$stmt->execute()) {
        errorOut("DB Error: " . $stmt->error, 500);
    }
    
    // The `after_call_insert` trigger now handles contact upsert and stats automatically.

    out([
        "unique_id" => $unique_id,
        "upload_status" => $upload_status,
        "created_ts" => $created_at
    ], 200, "Call started");
}

/* =====================================================
   1️⃣.5️⃣ BATCH SYNC CALLS
   ===================================================== */
if ($action === "batch_sync_calls") {
    $org_id = trim($_POST['org_id'] ?? '');
    $employee_id = trim($_POST['user_id'] ?? '');
    $device_id = trim($_POST['device_id'] ?? '');
    $calls_json = $_POST['calls_json'] ?? '[]';

    if ($org_id === '' || $employee_id === '') errorOut("org_id and user_id required");
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

    // Update last_sync for the employee
    $updSync = $conn->prepare("UPDATE employees SET last_sync = NOW() WHERE id = ?");
    $updSync->bind_param("i", $employee_id);
    $updSync->execute();

    $calls = json_decode($calls_json, true);
    if (!is_array($calls)) {
        errorOut("Invalid calls_json format");
    }

    $synced_ids = [];
    $created_at = date("Y-m-d H:i:s");

    // Prepare Statement outside loop for efficiency
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

    $conn->begin_transaction();

    try {
        foreach ($calls as $call) {
            $unique_id = $call['unique_id'] ?? '';
            if (!$unique_id) continue;

            $caller_name = $call['caller_name'] ?? null;
            $caller_phone = isset($call['caller']) ? preg_replace('/[^\d]/', '', $call['caller']) : null;
            $duration = intval($call['duration'] ?? 0);
            $type = ucfirst($call['type'] ?? 'unknown');
            $call_time_param = $call['call_time'] ?? null;
            
            $device_phone_call = isset($call['device_phone']) ? preg_replace('/[^\d]/', '', $call['device_phone']) : null;

            $call_time_to_insert = $created_at;
            if ($call_time_param && strtotime($call_time_param)) {
                $call_time_to_insert = $call_time_param;
            }

            // Determine upload status
            $upload_status = $call['upload_status'] ?? 'pending';
            if ($duration <= 0 || strtolower($type) === 'missed') {
                $upload_status = 'completed';
            }

            $stmt->bind_param(
                "ssssssisss",
                $unique_id,
                $org_id,
                $employee_id,
                $device_phone_call,
                $caller_name,
                $caller_phone,
                $duration,
                $type,
                $upload_status,
                $call_time_to_insert
            );
            $stmt->execute();
            $synced_ids[] = $unique_id;
        }
        $conn->commit();
    } catch (Exception $e) {
        $conn->rollback();
        errorOut("Batch Insert Failed: " . $e->getMessage(), 500);
    }

    out([
        "synced_ids" => $synced_ids,
        "server_time" => time() * 1000
    ], 200, "Batch sync completed");
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

    // Check calls table and get org/device info for folder structure
    $check = $conn->prepare("SELECT upload_status, org_id, employee_id FROM calls WHERE unique_id=?");
    $check->bind_param("s", $unique_id);
    $check->execute();
    $res = $check->get_result();
    
    if ($res->num_rows === 0) {
        errorOut("Call not found for this chunk");
    }
    
    $row = $res->fetch_assoc();
    if ($row && $row['upload_status'] === 'completed') {
        errorOut("No recording expected for this call (already completed)");
    }

    // Store chunks in user-specific folder: public/{ORG_ID}/{EMP_ID}/chunks/{unique_id}/
    $orgId = $row['org_id'] ?: 'unknown_org';
    $empId = $row['employee_id'] ?: 'unknown_emp';
    
    $chunkDir = $PUBLIC_DIR . "$orgId/$empId/chunks/$unique_id/";
    if (!is_dir($chunkDir)) {
        if (!mkdir($chunkDir, 0777, true)) {
            errorOut("Failed to create chunk directory", 500);
        }
    }

    if (!move_uploaded_file($_FILES['chunk']['tmp_name'], $chunkDir . $index)) {
        errorOut("Failed to save chunk", 500);
    }
    
    out(["chunk_saved" => $index]);
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
        SELECT caller_phone, upload_status, employee_id, duration, org_id, call_time
        FROM calls WHERE unique_id=?
    ");
    $stmt->bind_param("s", $unique_id);
    $stmt->execute();
    $call = $stmt->get_result()->fetch_assoc();

    if (!$call)
        errorOut("Call not found");

    if ($call['upload_status'] === 'completed') {
        out([], 200, "No recording required/Already Done");
    }

    $callerSafe = safeCaller($call['caller_phone']);
    $empId = $call['employee_id'] ?: 'unknown_emp';
    $duration = intval($call['duration']);
    $orgId = $call['org_id'] ?: 'unknown_org';

    // Folder Structure: public/ORG_ID/EMP_ID/YYYY_MM/YYYYMMDD/
    // Use call_time for the folder structure and filename timestamp
    $deployTimestamp = (!empty($call['call_time']) && strtotime($call['call_time']) > 0) 
        ? strtotime($call['call_time']) 
        : time();

    $monthFolder = date("Y_m", $deployTimestamp);
    $dateFolder = date("Ymd", $deployTimestamp);
    $timeNow = date("Ymd_His", $deployTimestamp);

    $relPath = "$orgId/$empId/$monthFolder/$dateFolder/";
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
    
    // Chunks are stored in user-specific folder: public/{ORG_ID}/{EMP_ID}/chunks/{unique_id}/
    $chunkBaseDir = $PUBLIC_DIR . "$orgId/$empId/chunks/$unique_id/";
    
    for ($i = 0; $i < $total; $i++) {
        $chunkPath = $chunkBaseDir . $i;
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
    array_map('unlink', glob($chunkBaseDir . "*"));
    @rmdir($chunkBaseDir);

    // URL to access file
    $recording_url = "$BASE_URL/$relPath$fileName";

    $upd = $conn->prepare("
        UPDATE calls
        SET recording_url=?, upload_status='completed', updated_at=NOW()
        WHERE unique_id=?
    ");
    $upd->bind_param("ss", $recording_url, $unique_id);
    $upd->execute();

    out(["recording_url" => $recording_url], 200, "Upload finalized");
}

/* =====================================================
   5️⃣ GET UPDATES (Sync Cloud -> Phone)
   ===================================================== */
if ($action === "get_updates") {
    $org_id = trim($_POST['org_id'] ?? '');
    $last_sync = max(0, intval($_POST['last_sync_time'] ?? 0));
    
    if ($org_id === '') errorOut("org_id required");

    // Convert millis to seconds for SQL (keep as float/double for precision if needed, but int is usually fine)
    $last_sync_sec = $last_sync / 1000;

    // 1. Get Person Updates (Notes/Labels)
    $stmt = $conn->prepare("
        SELECT phone, notes, label, updated_at
        FROM contacts 
        WHERE org_id = ? AND updated_at > FROM_UNIXTIME(?)
    ");
    // "sd": string, double
    $stmt->bind_param("sd", $org_id, $last_sync_sec);
    $stmt->execute();
    $res = $stmt->get_result();
    
    $updates = [];
    while ($row = $res->fetch_assoc()) {
        $updates[] = [
            "phone" => $row['phone'],
            "person_note" => $row['notes'],
            "label" => $row['label'],
            "updated_at" => strtotime($row['updated_at']) * 1000
        ];
    }
    
    // 2. Get Call Note Updates
    $stmt2 = $conn->prepare("
        SELECT unique_id, note, updated_at
        FROM calls
        WHERE org_id = ? AND updated_at > FROM_UNIXTIME(?) AND note IS NOT NULL
    ");
    $stmt2->bind_param("sd", $org_id, $last_sync_sec);
    $stmt2->execute();
    $res2 = $stmt2->get_result();
    
    $call_updates = [];
    while ($row = $res2->fetch_assoc()) {
        // Only return if note is not null (already filtered by query)
        $call_updates[] = [
            "unique_id" => $row['unique_id'],
            "note" => $row['note'],
            "updated_at" => strtotime($row['updated_at']) * 1000
        ];
    }

    out([
        "success" => true,
        "person_updates" => $updates,
        "call_updates" => $call_updates,
        "server_time" => time() * 1000
    ]);
}

/* =====================================================
   4️⃣ UPDATE NOTES (AND CONTACTS)
===================================================== */
if ($action === "update_note") {

    $unique_id = $_POST['unique_id'] ?? '';
    // App might send 'note' or 'call_note', let's check both
    $note = $_POST['note'] ?? $_POST['call_note'] ?? null; 
    $person_note = $_POST['person_note'] ?? null;
    $label = $_POST['label'] ?? null;

    if ($unique_id === '')
        errorOut("unique_id required");

    // 4.1 Update Call Note
    if ($note !== null) {
        $stmt = $conn->prepare("UPDATE calls SET note=?, updated_at=NOW() WHERE unique_id=?");
        $stmt->bind_param("ss", $note, $unique_id);
        $stmt->execute();
    }

    // 4.2 Update Person Note & Label (in contacts table)
    if ($person_note !== null || $label !== null) {
        // Need to find the phone number associated with this unique_id
        $stmt = $conn->prepare("SELECT caller_phone, org_id FROM calls WHERE unique_id=?");
        $stmt->bind_param("s", $unique_id);
        $stmt->execute();
        $res = $stmt->get_result();
        
        if ($res->num_rows > 0) {
            $row = $res->fetch_assoc();
            $phone = $row['caller_phone'];
            $orgId = $row['org_id'];
            
            if ($phone && $orgId) {
                // Update contacts table
                if ($person_note !== null && $label !== null) {
                    $upd = $conn->prepare("UPDATE contacts SET notes=?, label=?, updated_at=NOW() WHERE phone=? AND org_id=?");
                    $upd->bind_param("ssss", $person_note, $label, $phone, $orgId);
                    $upd->execute();
                } elseif ($person_note !== null) {
                    $upd = $conn->prepare("UPDATE contacts SET notes=?, updated_at=NOW() WHERE phone=? AND org_id=?");
                    $upd->bind_param("sss", $person_note, $phone, $orgId);
                    $upd->execute();
                } elseif ($label !== null) {
                    $upd = $conn->prepare("UPDATE contacts SET label=?, updated_at=NOW() WHERE phone=? AND org_id=?");
                    $upd->bind_param("sss", $label, $phone, $orgId);
                    $upd->execute();
                }
                
                // Also update calls.labels for all calls from this phone
                if ($label !== null) {
                    $callsLabelUpd = $conn->prepare("UPDATE calls SET labels=?, updated_at=NOW() WHERE caller_phone=? AND org_id=?");
                    $callsLabelUpd->bind_param("sss", $label, $phone, $orgId);
                    $callsLabelUpd->execute();
                }
            }
        }
    }

    out(["success" => true, "message" => "Notes/Labels updated"]);
}

/* =====================================================
   6️⃣ FETCH UPDATES (Delta Sync - App pulls changes)
   Returns all call/person updates since last_sync_time
===================================================== */
if ($action === "fetch_updates") {
    $org_id = trim($_POST['org_id'] ?? '');
    $user_id = trim($_POST['user_id'] ?? '');
    $device_id = trim($_POST['device_id'] ?? '');
    $last_sync = max(0, intval($_POST['last_sync_time'] ?? 0));
    
    if ($org_id === '') errorOut("org_id required");

    // Update last_sync for the employee
    if ($user_id !== '') {
        $updSync = $conn->prepare("UPDATE employees SET last_sync = NOW() WHERE id = ? AND org_id = ?");
        if (!$updSync) errorOut("DB Error (Fetch Updates Sync): " . $conn->error, 500);
        $updSync->bind_param("is", $user_id, $org_id);
        $updSync->execute();
    }

    // Convert millis to seconds for SQL
    $last_sync_sec = $last_sync / 1000;

    // 1. Get Call Updates (note, reviewed, caller_name)
    $stmt = $conn->prepare("
        SELECT unique_id, note, reviewed, caller_name, 
               UNIX_TIMESTAMP(updated_at) * 1000 as updated_at
        FROM calls 
        WHERE org_id = ? AND updated_at > FROM_UNIXTIME(?)
    ");
    $stmt->bind_param("sd", $org_id, $last_sync_sec);
    $stmt->execute();
    $res = $stmt->get_result();
    
    $call_updates = [];
    while ($row = $res->fetch_assoc()) {
        $call_updates[] = [
            "unique_id" => $row['unique_id'],
            "note" => $row['note'],
            "reviewed" => (int)$row['reviewed'],
            "caller_name" => $row['caller_name'],
            "updated_at" => (int)$row['updated_at']
        ];
    }
    
    // 2. Get Person/Contact Updates (name, person_note, label)
    $stmt2 = $conn->prepare("
        SELECT phone, name, notes as person_note, label,
               UNIX_TIMESTAMP(updated_at) * 1000 as updated_at
        FROM contacts 
        WHERE org_id = ? AND updated_at > FROM_UNIXTIME(?)
    ");
    $stmt2->bind_param("sd", $org_id, $last_sync_sec);
    $stmt2->execute();
    $res2 = $stmt2->get_result();
    
    $person_updates = [];
    while ($row = $res2->fetch_assoc()) {
        $person_updates[] = [
            "phone" => $row['phone'],
            "name" => $row['name'],
            "person_note" => $row['person_note'],
            "label" => $row['label'],
            "updated_at" => (int)$row['updated_at']
        ];
    }

    out([
        "call_updates" => $call_updates,
        "person_updates" => $person_updates,
        "server_time" => time() * 1000
    ], 200, "Updates retrieved");
}

/* =====================================================
   7️⃣ UPDATE CALL (App pushes call metadata changes)
   Updates reviewed, note, caller_name with conflict resolution
===================================================== */
if ($action === "update_call") {
    $unique_id = trim($_POST['unique_id'] ?? '');
    $reviewed = isset($_POST['reviewed']) ? ($_POST['reviewed'] === 'true' || $_POST['reviewed'] === '1' ? 1 : 0) : null;
    $note = $_POST['note'] ?? null;
    $caller_name = $_POST['caller_name'] ?? null;
    $upload_status = $_POST['upload_status'] ?? null;
    $updated_at = intval($_POST['updated_at'] ?? 0); // Client's update timestamp in millis
    
    if ($unique_id === '') errorOut("unique_id required");
    
    // Build dynamic update query
    $updates = [];
    $params = [];
    $types = "";
    
    if ($reviewed !== null) {
        $updates[] = "reviewed = ?";
        $params[] = $reviewed;
        $types .= "i";
    }
    
    if ($note !== null) {
        $updates[] = "note = ?";
        $params[] = $note;
        $types .= "s";
    }
    
    if ($caller_name !== null) {
        $updates[] = "caller_name = ?";
        $params[] = $caller_name;
        $types .= "s";
    }
    
    if ($upload_status !== null) {
        $updates[] = "upload_status = ?";
        $params[] = $upload_status;
        $types .= "s";
    }
    
    if (empty($updates)) {
        out(["success" => true, "message" => "Nothing to update", "server_time" => time() * 1000]);
    }
    
    // Add updated_at = NOW() 
    $updates[] = "updated_at = NOW()";
    
    // Add unique_id to params
    $params[] = $unique_id;
    $types .= "s";
    
    $sql = "UPDATE calls SET " . implode(", ", $updates) . " WHERE unique_id = ?";
    $stmt = $conn->prepare($sql);
    // Use execute with array instead of bind_param for dynamic args
    if ($stmt->execute($params)) {
        out([
            "success" => true, 
            "message" => "Call updated",
            "server_time" => time() * 1000
        ]);
    } else {
        errorOut("Failed to update call: " . $conn->error, 500);
    }
}

/* =====================================================
   8️⃣ UPDATE PERSON (App pushes person metadata changes)
   Updates person_note, label, name in contacts table
===================================================== */
if ($action === "update_person") {
    $phone = isset($_POST['phone']) ? preg_replace('/[^\d]/', '', $_POST['phone']) : '';
    $org_id = trim($_POST['org_id'] ?? '');
    $person_note = $_POST['person_note'] ?? null;
    $label = $_POST['label'] ?? null;
    $name = $_POST['name'] ?? null;
    $updated_at = intval($_POST['updated_at'] ?? 0);
    
    if ($phone === '' || $org_id === '') {
        errorOut("phone and org_id required");
    }
    
    // Check if contact exists
    $checkStmt = $conn->prepare("SELECT id FROM contacts WHERE phone = ? AND org_id = ?");
    $checkStmt->bind_param("ss", $phone, $org_id);
    $checkStmt->execute();
    $exists = $checkStmt->get_result()->num_rows > 0;
    
    if ($exists) {
        // Build update query
        $updates = [];
        $params = [];
        $types = "";
        
        if ($person_note !== null) {
            $updates[] = "notes = ?";
            $params[] = $person_note;
            $types .= "s";
        }
        
        if ($label !== null) {
            $updates[] = "label = ?";
            $params[] = $label;
            $types .= "s";
        }
        
        if ($name !== null) {
            $updates[] = "name = ?";
            $params[] = $name;
            $types .= "s";
        }
        
        if (!empty($updates)) {
            $updates[] = "updated_at = NOW()";
            $params[] = $phone;
            $params[] = $org_id;
            $types .= "ss";
            
            $sql = "UPDATE contacts SET " . implode(", ", $updates) . " WHERE phone = ? AND org_id = ?";
            $stmt = $conn->prepare($sql);
            $stmt->execute($params);
        }
    } else {
        // Insert new contact
        $stmt = $conn->prepare("
            INSERT INTO contacts (phone, org_id, name, notes, label, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, NOW(), NOW())
        ");
        $stmt->bind_param("sssss", $phone, $org_id, $name, $person_note, $label);
        $stmt->execute();
    }
    
    // If label was updated, also update calls.labels for all calls from this phone
    if ($label !== null) {
        $labelUpdate = $conn->prepare("UPDATE calls SET labels = ?, updated_at = NOW() WHERE caller_phone = ? AND org_id = ?");
        $labelUpdate->bind_param("sss", $label, $phone, $org_id);
        $labelUpdate->execute();
    }
    
    out([
        "success" => true,
        "message" => "Person updated",
        "server_time" => time() * 1000
    ]);
}

/* =====================================================
   9️⃣ FETCH CONFIG (App pulls excluded contacts and settings)
   ===================================================== */
if ($action === "fetch_config") {
    $org_id = trim($_POST['org_id'] ?? '');
    $user_id = trim($_POST['user_id'] ?? ''); // employee_id
    
    if ($org_id === '' || $user_id === '') errorOut("org_id and user_id required");

    // Optional: Update device info during config fetch (heartbeat)
    $battery_level = isset($_POST['battery_level']) ? intval($_POST['battery_level']) : null;
    $os_version = trim($_POST['os_version'] ?? '');
    $device_model = trim($_POST['device_model'] ?? '');
    
    if ($battery_level !== null || $os_version !== '' || $device_model !== '') {
        $updParts = [];
        $updP = [];
        $updT = "";
        if ($battery_level !== null) { $updParts[] = "battery_level = ?"; $updP[] = $battery_level; $updT .= "i"; }
        if ($os_version !== '') { $updParts[] = "os_version = ?"; $updP[] = $os_version; $updT .= "s"; }
        if ($device_model !== '') { $updParts[] = "device_model = ?"; $updP[] = $device_model; $updT .= "s"; }
        
        if (!empty($updParts)) {
            $updP[] = $user_id;
            $updT .= "i";
            $sql = "UPDATE employees SET " . implode(", ", $updParts) . ", last_sync = NOW() WHERE id = ?";
            $upd = $conn->prepare($sql);
            if (!$upd) errorOut("DB Error (Config Update): " . $conn->error, 500);
            $upd->execute($updP);
        }
    }

    // 1. Get Excluded Contacts
    $stmt = $conn->prepare("SELECT phone FROM excluded_contacts WHERE org_id = ? AND exclude_from_sync = 1");
    $stmt->bind_param("s", $org_id);
    $stmt->execute();
    $res = $stmt->get_result();
    
    $excluded = [];
    while ($row = $res->fetch_assoc()) {
        $excluded[] = $row['phone'];
    }

    // 2. Get Employee Settings and Organization Plan Info
    $stmt2 = $conn->prepare("
        SELECT 
            e.allow_personal_exclusion, 
            e.allow_changing_tracking_start_date, 
            e.allow_updating_tracking_sims, 
            e.default_tracking_starting_date,
            e.call_track,
            e.call_record_crm,
            u.plan_expiry_date,
            u.allowed_storage_gb,
            u.storage_used_bytes
        FROM employees e
        JOIN users u ON e.org_id = u.org_id
        WHERE e.id = ? AND e.org_id = ?
        LIMIT 1
    ");
    $stmt2->bind_param("is", $user_id, $org_id);
    $stmt2->execute();
    $res2 = $stmt2->get_result();
    $settings = $res2->fetch_assoc();

    out([
        "excluded_contacts" => $excluded,
        "settings" => [
            "allow_personal_exclusion" => (int)($settings['allow_personal_exclusion'] ?? 0),
            "allow_changing_tracking_start_date" => (int)($settings['allow_changing_tracking_start_date'] ?? 1),
            "allow_updating_tracking_sims" => (int)($settings['allow_updating_tracking_sims'] ?? 0),
            "default_tracking_starting_date" => $settings['default_tracking_starting_date'] ?: null,
            "call_track" => (int)($settings['call_track'] ?? 1),
            "call_record_crm" => (int)($settings['call_record_crm'] ?? 1)
        ],
        "plan" => [
            "expiry_date" => $settings['plan_expiry_date'] ?? null,
            "allowed_storage_gb" => (float)($settings['allowed_storage_gb'] ?? 0),
            "storage_used_bytes" => (int)($settings['storage_used_bytes'] ?? 0)
        ]
    ], 200, "Configuration retrieved");
}
/* =====================================================
   🔟 CHECK RECORDINGS STATUS (Batch Check)
   Avoids re-uploading existing files
   ===================================================== */
if ($action === "check_recordings_status") {
    $unique_ids_json = $_POST['unique_ids'] ?? '[]';
    $unique_ids = json_decode($unique_ids_json, true);
    
    if (!is_array($unique_ids) || empty($unique_ids)) {
        out(["completed_ids" => []]);
    }
    
    // Sanitize IDs
    $safe_ids = array_map(function($id) use ($conn) {
        return "'" . $conn->real_escape_string($id) . "'";
    }, $unique_ids);
    
    $id_list = implode(",", $safe_ids);
    
    $stmt = $conn->prepare("SELECT unique_id FROM calls WHERE unique_id IN ($id_list) AND upload_status = 'completed'");
    $stmt->execute();
    $res = $stmt->get_result();
    
    $completed = [];
    while ($row = $res->fetch_assoc()) {
        $completed[] = $row['unique_id'];
    }
    
    out(["completed_ids" => $completed]);
}
/* =====================================================
   ADD DEMO CALL (For testing - bypasses device verification)
===================================================== */
if ($action === "add_demo_call") {
    // Simple secret for demo calls (change in production)
    $demo_secret = $_POST['demo_secret'] ?? '';
    if ($demo_secret !== 'demo123') {
        errorOut("Invalid demo secret");
    }

    $org_id       = trim($_POST['org_id'] ?? '');
    $employee_id  = trim($_POST['user_id'] ?? ''); 
    $caller_phone = isset($_POST['caller']) ? preg_replace('/[^\d]/', '', $_POST['caller']) : null;
    $caller_name  = $_POST['caller_name'] ?? 'Demo Caller';
    $duration     = intval($_POST['duration'] ?? 60);
    $type         = $_POST['type'] ?? 'Incoming';
    $call_time    = $_POST['call_time'] ?? date("Y-m-d H:i:s");
    
    if ($org_id === '' || $employee_id === '') {
        errorOut("org_id and user_id required");
    }
    
    // Verify employee exists (without device check)
    $stmtVerify = $conn->prepare("SELECT id, name FROM employees WHERE id = ? AND org_id = ?");
    $stmtVerify->bind_param("is", $employee_id, $org_id);
    $stmtVerify->execute();
    $resVerify = $stmtVerify->get_result();
    
    if ($resVerify->num_rows === 0) {
        errorOut("Invalid employee or organization");
    }
    
    // Generate unique ID
    $unique_id = "demo_" . time() . "_" . rand(1000, 9999);
    
    // Map type
    $type = ucfirst(strtolower($type));
    
    // Upload status
    $upload_status = 'completed'; // Demo calls don't have recordings
    
    // Insert call
    $stmt = $conn->prepare("
        INSERT INTO calls
        (unique_id, org_id, employee_id, caller_name, caller_phone, duration, type, upload_status, call_time)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    ");
    
    $stmt->bind_param(
        "sssssisss",
        $unique_id,
        $org_id,
        $employee_id,
        $caller_name,
        $caller_phone,
        $duration,
        $type,
        $upload_status,
        $call_time
    );
    
    if (!$stmt->execute()) {
        errorOut("DB Error: " . $stmt->error, 500);
    }
    
    out([
        "message" => "Demo call added successfully",
        "unique_id" => $unique_id,
        "org_id" => $org_id,
        "employee_id" => $employee_id,
        "caller_phone" => $caller_phone,
        "caller_name" => $caller_name,
        "duration" => $duration,
        "type" => $type,
        "call_time" => $call_time
    ]);
}

/* ============================
   INVALID ACTION
============================ */
errorOut("Invalid action");
?>
