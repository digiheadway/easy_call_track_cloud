Give Compands here for mysql management.
https://cornflowerblue-chinchilla-284008.hostingersite.com/ai_mysql_manager.php

The code of this file is given below.
"<?php
/* =====================================
   AI MYSQL MANAGER – DIRECT SQL EXEC
   ===================================== */

/* ===== CONFIG ===== */
$SECRET_TOKEN = "CHANGE_THIS_SECRET_TOKEN"; // MUST CHANGE

$DB_HOST = "localhost";
$DB_USER = "u542940820_easycalls";
$DB_PASS = "v7D5;Xsz!~I";
$DB_NAME = "u542940820_easycalls";

/* ===== HEADERS ===== */
header("Content-Type: application/json; charset=utf-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
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

/* ===== CONNECT DB ===== */
$conn = new mysqli($DB_HOST, $DB_USER, $DB_PASS, $DB_NAME);
if ($conn->connect_error) {
    echo json_encode(["status" => false, "message" => "DB Connection Failed"]);
    exit;
}

$conn->set_charset("utf8mb4");

/* ===== INPUT ===== */
$data = json_decode(file_get_contents("php://input"), true);
$sql = trim($data['sql'] ?? '');

if ($sql === '') {
    echo json_encode(["status" => false, "message" => "SQL is required"]);
    exit;
}

/* ===== EXECUTE SQL ===== */
$result = $conn->query($sql);

if ($result === false) {
    echo json_encode([
        "status" => false,
        "error" => $conn->error
    ]);
    exit;
}

/* ===== HANDLE SELECT ===== */
if ($result instanceof mysqli_result) {
    $rows = [];
    while ($row = $result->fetch_assoc()) {
        $rows[] = $row;
    }

    echo json_encode([
        "status" => true,
        "type" => "select",
        "rows" => $rows,
        "count" => count($rows)
    ]);
    exit;
}

/* ===== HANDLE INSERT / UPDATE / DELETE ===== */
echo json_encode([
    "status" => true,
    "type" => "write",
    "affected_rows" => $conn->affected_rows,
    "insert_id" => $conn->insert_id
]);
"