<?php
$file = 'api/sync_app.php';
$content = file_get_contents($file);
$result = shell_exec("php -l " . escapeshellarg($file));
echo json_encode(["status" => true, "output" => $result]);
