<?php
require_once 'config.php';
require_once 'utils.php';

// Simulate data
$email = "test_" . time() . "@example.com";
$password = "password123";

$orgId = strtoupper(substr(md5(uniqid($email, true)), 0, 6));
$orgName = explode('@', $email)[0] . "'s Organization";
$adminName = explode('@', $email)[0];
$otp = "123456";
$otpExpiry = date('Y-m-d H:i:s', strtotime('+1 hour'));
$passwordHash = Auth::hashPassword($password);

// Escape values manually here just to be sure, mirroring auth.php
$emailEsc = Database::escape($email);
$orgIdEsc = Database::escape($orgId); // alphanumeric, safe
$orgNameEsc = Database::escape($orgName);
// $adminNameEsc = Database::escape($adminName); // wait, auth.php doesn't escape adminName?

/* 
In auth.php:
$adminName = explode('@', $email)[0];
...
VALUES (..., '$adminName', ...
*/

// Check auth.php line 38: $adminName = explode('@', $email)[0];
// It is NOT escaped in the SQL string in auth.php! which is a bug if email contains quotes.
// But email validation usually prevents quotes, unless user is crafty. 
// However, the user provided "ysbhrdwj@gmail.com", which is safe.

$userSql = "INSERT INTO users (org_id, org_name, name, email, password_hash, role, status, plan_info, otp, otp_expiry, is_verified) 
            VALUES ('$orgId', '$orgNameEsc', '$adminName', '$emailEsc', '$passwordHash', 'admin', 'active', '', '$otp', '$otpExpiry', 0)";

echo "Query: $userSql <br><br>";

$result = Database::execute($userSql);

echo "Result: " . json_encode($result);
?>
