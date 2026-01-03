<?php
/* =====================================
   CallCloud Admin - Auth API
   ===================================== */

require_once '../config.php';
require_once '../utils.php';

// Set headers
header('Access-Control-Allow-Origin: ' . CORS_ALLOWED_ORIGINS);
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Authorization, Content-Type');
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Get request data
$data = json_decode(file_get_contents('php://input'), true);
$action = $_GET['action'] ?? '';

switch ($action) {
    
    /* ===== SIGNUP ===== */
    case 'signup':
        // Validate required fields
        Validator::required($data, ['email', 'password']);
        Validator::email($data['email']);
        
        $email = Database::escape($data['email']);
        $password = Auth::hashPassword($data['password']);
        
        // Generate Org ID (random 6 char alphanumeric)
        $orgId = strtoupper(substr(md5(uniqid($email, true)), 0, 6));
        $orgName = explode('@', $email)[0] . "'s Organization";
        $adminName = explode('@', $email)[0];
        
        // EMAIL CHECK
        $existingUser = Database::getOne("SELECT id FROM users WHERE email = '$email'");
        if ($existingUser) {
            Response::error('Email already registered');
        }
        
        // Generate OTP
        $otp = rand(100000, 999999);
        $otpExpiry = date('Y-m-d H:i:s', strtotime('+1 hour'));

        // Create user (User is the Organization)
        $userSql = "INSERT INTO users (org_id, org_name, name, email, password_hash, role, status, plan_info, otp, otp_expiry, is_verified) 
                    VALUES ('$orgId', '$orgName', '$adminName', '$email', '$password', 'admin', 'active', '', '$otp', '$otpExpiry', 0)";
        $userId = Database::insert($userSql);
        
        if (!$userId) {
            Response::error('Failed to create user/organization');
        }
        
        // Send Verification Email
        $subject = "Verify your email - CallTrack";
        $message = "Your verification code is: $otp";
        Mailer::send($email, $subject, $message);

        // Create session
        $token = Auth::createSession($userId);
        
        // Get user data
        $user = Database::getOne("SELECT * FROM users WHERE id = $userId");
        
        if ($user) {
            $user['organizationName'] = $user['org_name']; // Map for frontend compatibility
            unset($user['password_hash']);
        }
        
        Response::success([
            'token' => $token,
            'user' => $user,
            'verification_required' => true
        ], 'Registration successful. Please verify your email.');
        break;

    /* ===== VERIFY EMAIL ===== */
    case 'verify_email':
        Validator::required($data, ['email', 'otp']);
        $email = Database::escape($data['email']);
        $otp = Database::escape($data['otp']);

        $user = Database::getOne("SELECT * FROM users WHERE email = '$email'");
        if (!$user) {
            Response::error('User not found');
        }

        if ($user['is_verified'] == 1) {
            Response::success([], 'Email already verified');
        }

        if ($user['otp'] !== $otp) {
            Response::error('Invalid OTP');
        }

        if (strtotime($user['otp_expiry']) < time()) {
            Response::error('OTP expired. Please request a new one.');
        }

        Database::execute("UPDATE users SET is_verified = 1, otp = NULL, otp_expiry = NULL WHERE id = {$user['id']}");

        Response::success([], 'Email verified successfully');
        break;

    /* ===== RESEND OTP ===== */
    case 'resend_otp':
        Validator::required($data, ['email']);
        $email = Database::escape($data['email']);
        
        $user = Database::getOne("SELECT * FROM users WHERE email = '$email'");
        if (!$user) {
            Response::error('User not found');
        }
        
        if ($user['is_verified'] == 1) {
            Response::error('Account already verified');
        }
        
        $otp = rand(100000, 999999);
        $otpExpiry = date('Y-m-d H:i:s', strtotime('+1 hour'));
        
        Database::execute("UPDATE users SET otp = '$otp', otp_expiry = '$otpExpiry' WHERE id = {$user['id']}");
        
        $subject = "Verify your email - CallTrack";
        $message = "Your new verification code is: $otp";
        Mailer::send($email, $subject, $message);
        
        Response::success([], 'Verification code sent');
        break;

    /* ===== FORGOT PASSWORD ===== */
    case 'forgot_password':
        Validator::required($data, ['email']);
        $email = Database::escape($data['email']);

        $user = Database::getOne("SELECT * FROM users WHERE email = '$email'");
        
        // Always return success for security, but if found send email
        if ($user) {
            $otp = rand(100000, 999999);
            $otpExpiry = date('Y-m-d H:i:s', strtotime('+1 hour'));

            Database::execute("UPDATE users SET otp = '$otp', otp_expiry = '$otpExpiry' WHERE id = {$user['id']}");

            $subject = "Reset Password - CallTrack";
            $message = "Your password reset code is: $otp";
            Mailer::send($email, $subject, $message);
        }

        Response::success([], 'If an account exists with this email, a reset code has been sent.');
        break;

    /* ===== RESET PASSWORD ===== */
    case 'reset_password':
        Validator::required($data, ['email', 'otp', 'new_password']);
        $email = Database::escape($data['email']);
        $otp = Database::escape($data['otp']);
        $newPass = Auth::hashPassword($data['new_password']);

        $user = Database::getOne("SELECT * FROM users WHERE email = '$email'");
        if (!$user) {
            Response::error('Invalid request');
        }

        if ($user['otp'] !== $otp) {
            Response::error('Invalid OTP');
        }

        if (strtotime($user['otp_expiry']) < time()) {
            Response::error('OTP expired');
        }

        Database::execute("UPDATE users SET password_hash = '$newPass', otp = NULL, otp_expiry = NULL WHERE id = {$user['id']}");

        Response::success([], 'Password reset successfully. You can now login.');
        break;
    
    /* ===== LOGIN ===== */
    case 'login':
        // Validate required fields
        Validator::required($data, ['email', 'password']);
        Validator::email($data['email']);
        
        $email = Database::escape($data['email']);
        
        // Get user
        $user = Database::getOne("SELECT * FROM users WHERE email = '$email'");
        
        if (!$user || !Auth::verifyPassword($data['password'], $user['password_hash'])) {
            Response::error('Invalid email or password', 401);
        }
        
        // Create session
        $token = Auth::createSession($user['id']);
        
        $user['organizationName'] = $user['org_name']; // Map for compatibility
        unset($user['password_hash']);
        
        Response::success([
            'token' => $token,
            'user' => $user
        ], 'Login successful');
        break;
    
    /* ===== LOGOUT ===== */
    case 'logout':
        $headers = getallheaders();
        $auth = $headers['Authorization'] ?? '';
        
        if (preg_match('/Bearer\s(.+)/', $auth, $matches)) {
            Auth::deleteSession($matches[1]);
        }
        
        Response::success([], 'Logout successful');
        break;
    
    /* ===== VERIFY TOKEN ===== */
    case 'verify':
        $user = Auth::requireAuth();
        
        // User data comes from session join in keys, but let's refresh it properly
        $userData = Database::getOne("SELECT * FROM users WHERE id = {$user['user_id']}");
        
        if ($userData) {
            $userData['organizationName'] = $userData['org_name'];
            unset($userData['password_hash']);
        }

        Response::success(['user' => $userData], 'Token valid');
        break;
    
    default:
        Response::error('Invalid action');
}
