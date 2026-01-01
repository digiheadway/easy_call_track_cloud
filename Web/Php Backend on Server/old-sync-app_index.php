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
    "user" => "u542940820_callcloud",
    "pass" => "@7/A4I7dV8mW",
    "db" => "u542940820_callcloud"
];

$BASE_URL = "https://api.mylistings.in/callcloud/public";
$PUBLIC_DIR = __DIR__ . "/public/";
$TMP_DIR = __DIR__ . "/tmp_chunks/";

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
    if (!$caller)
        return 'unknown';
    return preg_replace('/[^0-9]/', '', $caller);
}

/* ============================
   DB CONNECT
============================ */
$conn = new mysqli($DB['host'], $DB['user'], $DB['pass'], $DB['db']);
if ($conn->connect_error) {
    errorOut("Database connection failed", 500);
}
$conn->set_charset("utf8mb4");

/* ============================
   ACTION
============================ */
$action = $_POST['action'] ?? '';
if (!$action)
    errorOut("Action is required");

/* =====================================================
   1️⃣ START CALL (TEXT org_id & user_id)
===================================================== */
if ($action === "start_call") {

    $unique_id = trim($_POST['unique_id'] ?? '');
    $org_id = trim($_POST['org_id'] ?? '');     // TEXT
    $user_id = trim($_POST['user_id'] ?? '');    // TEXT
    $device_phone = $_POST['device_phone'] ?? null;
    $caller_name = $_POST['caller_name'] ?? null;
    $caller = safeCaller($_POST['caller'] ?? null);
    $duration = intval($_POST['duration'] ?? 0);
    $type = strtolower($_POST['type'] ?? 'unknown');
    $created_at = date("Y-m-d H:i:s");

    if ($unique_id === '')
        errorOut("unique_id required");
    if ($org_id === '' || $user_id === '')
        errorOut("org_id and user_id required");

    // upload status logic
    $upload_status = 'pending';
    if ($duration <= 0 || $type === 'missed') {
        $upload_status = 'completed';
    }

    $stmt = $conn->prepare("
        INSERT INTO call_logs
        (unique_id, org_id, user_id, device_phone, caller_name, caller, duration, type, upload_status, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            device_phone = VALUES(device_phone),
            caller_name  = VALUES(caller_name),
            caller       = VALUES(caller),
            duration     = VALUES(duration),
            type         = VALUES(type),
            upload_status= VALUES(upload_status),
            updated_at   = NOW()
    ");

    $stmt->bind_param(
        "sssississs",
        $unique_id,
        $org_id,
        $user_id,
        $device_phone,
        $caller_name,
        $caller,
        $duration,
        $type,
        $upload_status,
        $created_at
    );

    if (!$stmt->execute()) {
        errorOut($stmt->error, 500);
    }

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

    $check = $conn->prepare("SELECT upload_status FROM call_logs WHERE unique_id=?");
    $check->bind_param("s", $unique_id);
    $check->execute();
    $row = $check->get_result()->fetch_assoc();

    if ($row && $row['upload_status'] === 'completed') {
        errorOut("No recording expected for this call");
    }

    $path = $TMP_DIR . $unique_id . "/";
    if (!is_dir($path) && !mkdir($path, 0777, true)) {
        errorOut("Failed to create temp directory", 500);
    }

    move_uploaded_file($_FILES['chunk']['tmp_name'], $path . $index);
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
        SELECT caller, upload_status, device_phone, duration
        FROM call_logs WHERE unique_id=?
    ");
    $stmt->bind_param("s", $unique_id);
    $stmt->execute();
    $call = $stmt->get_result()->fetch_assoc();

    if (!$call)
        errorOut("Call not found");

    if ($call['upload_status'] === 'completed') {
        out(["success" => true, "message" => "No recording required"]);
    }

    $caller = safeCaller($call['caller']);
    $device_phone = $call['device_phone'] ?: 'unknown';
    $duration = intval($call['duration']);

    $orgFolder = "ORG_" . date("ym");
    $monthFolder = date("Y_m");
    $dateFolder = date("Ymd");
    $timeNow = date("Ymd_His");

    $finalDir = $PUBLIC_DIR . "$orgFolder/$device_phone/$monthFolder/$dateFolder/";
    if (!is_dir($finalDir) && !mkdir($finalDir, 0777, true)) {
        errorOut("Failed to create recording directory", 500);
    }

    $fileName = "{$caller}_{$timeNow}_{$duration}.mp3";
    $finalPath = $finalDir . $fileName;

    $outFile = fopen($finalPath, "ab");
    for ($i = 0; $i < $total; $i++) {
        $chunkPath = $TMP_DIR . $unique_id . "/" . $i;
        if (!file_exists($chunkPath))
            errorOut("Missing chunk $i");
        $in = fopen($chunkPath, "rb");
        stream_copy_to_stream($in, $outFile);
        fclose($in);
    }
    fclose($outFile);

    array_map('unlink', glob($TMP_DIR . $unique_id . "/*"));
    rmdir($TMP_DIR . $unique_id);

    $recording_url = "$BASE_URL/$orgFolder/$device_phone/$monthFolder/$dateFolder/$fileName";

    $upd = $conn->prepare("
        UPDATE call_logs
        SET recording_url=?, upload_status='completed', updated_at=NOW()
        WHERE unique_id=?
    ");
    $upd->bind_param("ss", $recording_url, $unique_id);
    $upd->execute();

    out(["success" => true, "recording_url" => $recording_url]);
}

/* =====================================================
   4️⃣ UPDATE NOTES
===================================================== */
if ($action === "update_note") {

    $unique_id = $_POST['unique_id'] ?? '';
    $note = $_POST['note'] ?? null;
    $person_note = $_POST['person_note'] ?? null;

    if ($unique_id === '')
        errorOut("unique_id required");

    $stmt = $conn->prepare("
        UPDATE call_logs 
        SET note=?, person_note=?, updated_at=NOW()
        WHERE unique_id=?
    ");
    $stmt->bind_param("sss", $note, $person_note, $unique_id);
    $stmt->execute();

    out(["success" => true, "message" => "Notes updated"]);
}

/* ============================
   INVALID ACTION
============================ */
errorOut("Invalid action");
