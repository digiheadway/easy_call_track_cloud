Manage Files Using => https://calltrack.mylistings.in/ai_file_manager.php

This is the php code of this file to manage files on server.
"<?php
/* ===============================
   AI FILE MANAGER – ROOT LEVEL
   NO AUTH / NO TOKEN
   =============================== */

/* ===== CONFIG ===== */
$BASE_PATH = realpath(__DIR__); // Server root restriction

/* ===== HEADERS ===== */
header("Content-Type: application/json; charset=utf-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

/* ===== HELPERS ===== */
function safePath($path)
{
    global $BASE_PATH;

    $path = trim($path, "/");
    $full = $BASE_PATH . '/' . $path;

    $real = realpath($full) ?: $full;

    if (strpos(realpath(dirname($real)), $BASE_PATH) !== 0) {
        echo json_encode([
            "status" => false,
            "message" => "Invalid path access"
        ]);
        exit;
    }

    return $real;
}

function deleteFolder($dir)
{
    if (!is_dir($dir)) return;

    foreach (scandir($dir) as $item) {
        if ($item === '.' || $item === '..') continue;

        $path = $dir . '/' . $item;
        is_dir($path) ? deleteFolder($path) : unlink($path);
    }
    rmdir($dir);
}

/* ===== INPUT ===== */
$data = json_decode(file_get_contents("php://input"), true) ?? [];
$action = $data['action'] ?? '';

/* ===== ACTION HANDLER ===== */
switch ($action) {

    case "list":
        $path = safePath($data['path'] ?? '');
        $files = [];

        if (!is_dir($path)) {
            echo json_encode(["status" => false, "message" => "Folder not found"]);
            exit;
        }

        foreach (scandir($path) as $f) {
            if ($f === '.' || $f === '..') continue;

            $files[] = [
                "name" => $f,
                "type" => is_dir("$path/$f") ? "folder" : "file"
            ];
        }

        echo json_encode(["status" => true, "data" => $files]);
        break;

    case "read":
        $file = safePath($data['path'] ?? '');
        echo json_encode([
            "status" => true,
            "content" => file_exists($file) ? file_get_contents($file) : ""
        ]);
        break;

    case "create_file":
        $file = safePath($data['path'] ?? '');
        file_put_contents($file, $data['content'] ?? '');
        echo json_encode(["status" => true, "message" => "File created"]);
        break;

    case "update_file":
        $file = safePath($data['path'] ?? '');
        file_put_contents($file, $data['content'] ?? '');
        echo json_encode(["status" => true, "message" => "File updated"]);
        break;

    case "delete_file":
        $file = safePath($data['path'] ?? '');
        if (file_exists($file)) unlink($file);
        echo json_encode(["status" => true, "message" => "File deleted"]);
        break;

    case "create_folder":
        $folder = safePath($data['path'] ?? '');
        if (!is_dir($folder)) mkdir($folder, 0777, true);
        echo json_encode(["status" => true, "message" => "Folder created"]);
        break;

    case "delete_folder":
        $folder = safePath($data['path'] ?? '');
        deleteFolder($folder);
        echo json_encode(["status" => true, "message" => "Folder deleted"]);
        break;

    default:
        echo json_encode([
            "status" => false,
            "message" => "Invalid action"
        ]);
}

"