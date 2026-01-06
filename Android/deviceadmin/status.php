<?php
// Allow traffic from anywhere
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");
header('Content-Type: application/json');

// Get deviceid from query parameter
$device_id = isset($_GET['deviceid']) ? $_GET['deviceid'] : "unknown";

// Mock Data for Dummy Devices
$devices = [
    "1" => [
        "device_id"     => "1120",
        "amount"        => 2000,
        "message"       => "Uninstall Karke Dikha",
        "hide_icon"     => false,
        "is_freezed"    => true,
        "call_to"       => "9068062563",
        "is_protected"  => true
    ],

    "device_001" => [
        "device_id"     => "1121",
        "amount"        => 5000,
        "message"       => "EMI Payment Pending - Device Locked",
        "hide_icon"     => false,
        "is_freezed"    => true,
        "call_to"       => "+919876543210",
        "is_protected"  => true
    ],

    "device_002" => [
        "device_id"     => "1122",
        "amount"        => 0,
        "message"       => "Device is unlocked and active",
        "hide_icon"     => true,
        "is_freezed"    => true,
        "call_to"       => "",
        "is_protected"  => false
    ],

    "device_003" => [
        "device_id"     => "1123",
        "amount"        => 0,
        "message"       => "Device Protected - Uninstall Blocked",
        "hide_icon"     => true,
        "is_freezed"    => true,
        "call_to"       => "+911122334455",
        "is_protected"  => true
    ]
];

// Check if the requested device exists
if (array_key_exists($device_id, $devices)) {
    $response = $devices[$device_id];
} else {
    // Default / Fallback response
    $response = [
        "device_id"     => $device_id,
        "amount"        => 0,
        "message"       => "Emi bhar di",
        "hide_icon"     => false,
        "is_freezed"    => true,
        "call_to"       => "+919876543210",
        "is_protected"  => true,
        "auto_uninstall"=> false
    ];
}

echo json_encode($response, JSON_PRETTY_PRINT);
?>
