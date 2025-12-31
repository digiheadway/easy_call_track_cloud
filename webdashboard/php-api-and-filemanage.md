Manage Files Using => https://cornflowerblue-chinchilla-284008.hostingersite.com/ai_file_manager.php

This is the php code of this file to manage files on server.
"<?php
/* ===============================
   AI FILE MANAGER – ROOT LEVEL
   =============================== */

/* ===== CONFIG ===== */
$SECRET_TOKEN = "CHANGE_THIS_SECRET_TOKEN"; // must match AI IDE token
$BASE_PATH = realpath(__DIR__); // restrict to server root

/* ===== HEADERS ===== */
header("Content-Type: application/json; charset=utf-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Authorization, Content-Type");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

/* ===== AUTH CHECK ===== */
$auth = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
if (!preg_match('/Bearer\s(.+)/', $auth, $m) || $m[1] !== $SECRET_TOKEN) {
    http_response_code(401);
    echo json_encode(["status" => false, "message" => "Unauthorized"]);
    exit;
}

/* ===== HELPERS ===== */
function safePath($path)
{
    global $BASE_PATH;
    $full = realpath($BASE_PATH . '/' . ltrim($path, '/')) ?: $BASE_PATH . '/' . ltrim($path, '/');
    if (strpos(realpath(dirname($full)), $BASE_PATH) !== 0) {
        die(json_encode(["status" => false, "message" => "Invalid path"]));
    }
    return $full;
}

function deleteFolder($dir)
{
    if (!is_dir($dir)) return;
    foreach (scandir($dir) as $item) {
        if ($item == '.' || $item == '..') continue;
        $path = "$dir/$item";
        is_dir($path) ? deleteFolder($path) : unlink($path);
    }
    rmdir($dir);
}

/* ===== INPUT ===== */
$data = json_decode(file_get_contents("php://input"), true);
$action = $data['action'] ?? '';

/* ===== ACTION HANDLER ===== */
switch ($action) {

    case "list":
        $path = safePath($data['path'] ?? '');
        $files = [];
        foreach (scandir($path) as $f) {
            if ($f == '.' || $f == '..') continue;
            $files[] = [
                "name" => $f,
                "type" => is_dir("$path/$f") ? "folder" : "file"
            ];
        }
        echo json_encode(["status" => true, "data" => $files]);
        break;

    case "read":
        $file = safePath($data['path']);
        echo json_encode([
            "status" => true,
            "content" => file_exists($file) ? file_get_contents($file) : ""
        ]);
        break;

    case "create_file":
        $file = safePath($data['path']);
        file_put_contents($file, $data['content'] ?? '');
        echo json_encode(["status" => true, "message" => "File created"]);
        break;

    case "update_file":
        $file = safePath($data['path']);
        file_put_contents($file, $data['content'] ?? '');
        echo json_encode(["status" => true, "message" => "File updated"]);
        break;

    case "delete_file":
        $file = safePath($data['path']);
        if (file_exists($file)) unlink($file);
        echo json_encode(["status" => true, "message" => "File deleted"]);
        break;

    case "create_folder":
        $folder = safePath($data['path']);
        if (!is_dir($folder)) mkdir($folder, 0777, true);
        echo json_encode(["status" => true, "message" => "Folder created"]);
        break;

    case "delete_folder":
        $folder = safePath($data['path']);
        deleteFolder($folder);
        echo json_encode(["status" => true, "message" => "Folder deleted"]);
        break;

    default:
        echo json_encode(["status" => false, "message" => "Invalid action"]);
}
"