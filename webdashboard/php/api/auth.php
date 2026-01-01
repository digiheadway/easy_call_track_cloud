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
        Validator::required($data, ['organizationName', 'organizationId', 'adminName', 'email', 'password']);
        Validator::email($data['email']);
        Validator::orgId($data['organizationId']);
        
        $orgName = Database::escape($data['organizationName']);
        $orgId = Database::escape($data['organizationId']);
        $adminName = Database::escape($data['adminName']);
        $email = Database::escape($data['email']);
        $password = Auth::hashPassword($data['password']);
        
        // ORG CHECK: Check if org_id is already taken by another user
        // Since user table holds org info, we check users table.
        $existingOrg = Database::getOne("SELECT id FROM users WHERE org_id = '$orgId'");
        if ($existingOrg) {
            Response::error('Organization ID already taken');
        }
        
        // EMAIL CHECK
        $existingUser = Database::getOne("SELECT id FROM users WHERE email = '$email'");
        if ($existingUser) {
            Response::error('Email already registered');
        }
        
        // Create user (User is the Organization)
        $userSql = "INSERT INTO users (org_id, org_name, name, email, password_hash, role, status, plan_info) 
                    VALUES ('$orgId', '$orgName', '$adminName', '$email', '$password', 'admin', 'active', '')";
        $userId = Database::insert($userSql);
        
        if (!$userId) {
            Response::error('Failed to create user/organization');
        }
        
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
            'user' => $user
        ], 'Registration successful');
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
