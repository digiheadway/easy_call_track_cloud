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

$BASE_URL = "https://calltrack.mylistings.in/public"; 
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
if (!$action) errorOut("Action is required");

/* =====================================================
   0️⃣ VERIFY PAIRING CODE
===================================================== */
if ($action === "verify_pairing_code") {
    $org_id = trim($_POST['org_id'] ?? '');
    $employee_id = trim($_POST['user_id'] ?? '');
    $device_id = trim($_POST['device_id'] ?? '');

    if ($org_id === '' || $employee_id === '') {
        errorOut("Pairing code invalid (ORGID and USERID required)");
    }
    if ($device_id === '') {
        errorOut("Device ID required");
    }

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
        // Link the device to this employee
        $upd = $conn->prepare("UPDATE employees SET device_id = ?, updated_at = NOW() WHERE id = ?");
        $upd->bind_param("si", $device_id, $employee_id);
        $upd->execute();
        
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
            // Force switch device
            $upd = $conn->prepare("UPDATE employees SET device_id = ?, updated_at = NOW() WHERE id = ?");
            $upd->bind_param("si", $device_id, $employee_id);
            $upd->execute();
            
            out([
                "success" => true,
                "message" => "Switched to new device",
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
    $employee_id  = trim($_POST['user_id'] ?? '');
    $device_phone = isset($_POST['device_phone']) ? preg_replace('/[^\d]/', '', $_POST['device_phone']) : null;
    $caller_name  = $_POST['caller_name'] ?? null;
    $caller_phone = isset($_POST['caller']) ? preg_replace('/[^\d]/', '', $_POST['caller']) : null;
    $duration     = intval($_POST['duration'] ?? 0);
    $type         = ucfirst($_POST['type'] ?? 'unknown');
    $call_time    = $_POST['call_time'] ?? date("Y-m-d H:i:s");
    $device_id    = trim($_POST['device_id'] ?? '');

    if ($unique_id === '' || $org_id === '' || $employee_id === '') errorOut("Missing fields");

    $stmt = $conn->prepare("INSERT INTO calls (unique_id, org_id, employee_id, device_phone, caller_name, caller_phone, duration, type, upload_status, call_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE device_phone=VALUES(device_phone), updated_at=NOW()");
    $status = ($duration <= 0 || strtolower($type) === 'missed') ? 'completed' : 'pending';
    $stmt->bind_param("ssssssisss", $unique_id, $org_id, $employee_id, $device_phone, $caller_name, $caller_phone, $duration, $type, $status, $call_time);
    $stmt->execute();

    out(["success" => true, "unique_id" => $unique_id, "upload_status" => $status]);
}

/* =====================================================
   2️⃣ UPLOAD CHUNK
===================================================== */
if ($action === "upload_chunk") {
    $unique_id = $_POST['unique_id'] ?? '';
    $index = intval($_POST['chunk_index'] ?? -1);
    if (!isset($_FILES['chunk']) || $unique_id === '' || $index < 0) errorOut("Missing data");

    $path = $TMP_DIR . $unique_id . "/";
    if (!is_dir($path)) mkdir($path, 0777, true);
    move_uploaded_file($_FILES['chunk']['tmp_name'], $path . $index);
    out(["success" => true, "chunk_saved" => $index]);
}

/* =====================================================
   3️⃣ FINALIZE UPLOAD
===================================================== */
if ($action === "finalize_upload") {
    $unique_id = $_POST['unique_id'] ?? '';
    $total = intval($_POST['total_chunks'] ?? 0);
    if ($unique_id === '' || $total <= 0) errorOut("Missing data");

    $stmt = $conn->prepare("SELECT caller_phone, device_phone, duration, org_id, call_time FROM calls WHERE unique_id=?");
    $stmt->bind_param("s", $unique_id);
    $stmt->execute();
    $call = $stmt->get_result()->fetch_assoc();
    if (!$call) errorOut("Call not found");

    $cSafe = safeCaller($call['caller_phone']);
    $dSafe = safeCaller($call['device_phone'] ?: 'unknown');
    $dur = intval($call['duration']);
    $org = $call['org_id'];
    $ts = strtotime($call['call_time'] ?: 'now');
    
    $rel = "$org/$dSafe/" . date("Y_m", $ts) . "/" . date("Ymd", $ts) . "/";
    $dir = $PUBLIC_DIR . $rel;
    if (!is_dir($dir)) mkdir($dir, 0777, true);

    $file = "{$cSafe}_" . date("Ymd_His", $ts) . "_{$dur}.mp3";
    $out = fopen($dir . $file, "ab");
    for ($i = 0; $i < $total; $i++) {
        $cp = $TMP_DIR . $unique_id . "/" . $i;
        if (file_exists($cp)) {
            $in = fopen($cp, "rb");
            stream_copy_to_stream($in, $out);
            fclose($in);
            unlink($cp);
        }
    }
    fclose($out);
    if (is_dir($TMP_DIR . $unique_id)) rmdir($TMP_DIR . $unique_id);

    $url = "$BASE_URL/$rel$file";
    $upd = $conn->prepare("UPDATE calls SET recording_url=?, upload_status='completed', updated_at=NOW() WHERE unique_id=?");
    $upd->bind_param("ss", $url, $unique_id);
    $upd->execute();
    out(["success" => true, "recording_url" => $url]);
}

/* =====================================================
   DELTA UPDATES (GET/FETCH)
===================================================== */
if ($action === "get_updates" || $action === "fetch_updates") {
    $org_id = trim($_POST['org_id'] ?? '');
    $last = max(0, intval($_POST['last_sync_time'] ?? 0)) / 1000;
    if ($org_id === '') errorOut("org_id required");

    $stmt = $conn->prepare("SELECT phone, name, notes, label, updated_at FROM contacts WHERE org_id = ? AND updated_at > FROM_UNIXTIME(?)");
    $stmt->bind_param("sd", $org_id, $last);
    $stmt->execute();
    $res = $stmt->get_result();
    $pu = [];
    while ($row = $res->fetch_assoc()) {
        $pu[] = ["phone" => $row['phone'], "name" => $row['name'] ?? '', "person_note" => $row['notes'], "label" => $row['label'], "updated_at" => strtotime($row['updated_at']) * 1000];
    }

    $stmt2 = $conn->prepare("SELECT unique_id, note, reviewed, caller_name, updated_at FROM calls WHERE org_id = ? AND updated_at > FROM_UNIXTIME(?)");
    $stmt2->bind_param("sd", $org_id, $last);
    $stmt2->execute();
    $res2 = $stmt2->get_result();
    $cu = [];
    while ($row = $res2->fetch_assoc()) {
        $cu[] = ["unique_id" => $row['unique_id'], "note" => $row['note'], "reviewed" => (int)($row['reviewed'] ?? 0), "caller_name" => $row['caller_name'], "updated_at" => strtotime($row['updated_at']) * 1000];
    }

    out(["success" => true, "person_updates" => $pu, "call_updates" => $cu, "server_time" => time() * 1000]);
}

if ($action === "fetch_config") {
    $org_id = trim($_POST['org_id'] ?? '');
    $user_id = trim($_POST['user_id'] ?? '');
    if ($org_id === '' || $user_id === '') errorOut("Missing ID");

    $stmt = $conn->prepare("SELECT phone FROM excluded_contacts WHERE org_id = ? AND is_active = 1");
    $stmt->bind_param("s", $org_id);
    $stmt->execute();
    $ex = [];
    $res = $stmt->get_result();
    while ($r = $res->fetch_assoc()) $ex[] = $r['phone'];

    $stmt2 = $conn->prepare("SELECT allow_personal_exclusion, allow_changing_tracking_start_date, allow_updating_tracking_sims, default_tracking_starting_date FROM employees WHERE id = ? AND org_id = ?");
    $stmt2->bind_param("is", $user_id, $org_id);
    $stmt2->execute();
    $s = $stmt2->get_result()->fetch_assoc();

    out([
        "success" => true,
        "excluded_contacts" => $ex,
        "settings" => [
            "allow_personal_exclusion" => (int)($s['allow_personal_exclusion'] ?? 0),
            "allow_changing_tracking_start_date" => (int)($s['allow_changing_tracking_start_date'] ?? 0),
            "allow_updating_tracking_sims" => (int)($s['allow_updating_tracking_sims'] ?? 0),
            "default_tracking_starting_date" => $s['default_tracking_starting_date'] ?? null
        ]
    ]);
}

errorOut("Invalid action");
?>
