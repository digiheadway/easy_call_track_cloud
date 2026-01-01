<?php
/* =====================================
   CallCloud Admin - Database Helper
   ===================================== */

class Database {
    
    /**
     * Execute SQL query via MySQL Manager
     */
    public static function query($sql) {
        $ch = curl_init(MYSQL_MANAGER_URL);
        
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_POST => true,
            CURLOPT_HTTPHEADER => [
                'Authorization: Bearer ' . API_SECRET_TOKEN,
                'Content-Type: application/json'
            ],
            CURLOPT_POSTFIELDS => json_encode(['sql' => $sql])
        ]);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        
        if ($httpCode !== 200) {
            return ['status' => false, 'error' => 'Database connection failed'];
        }
        
        return json_decode($response, true);
    }
    
    /**
     * Execute SELECT query
     */
    public static function select($sql) {
        $result = self::query($sql);
        
        if (!$result['status']) {
            return [];
        }
        
        return $result['rows'] ?? [];
    }
    
    /**
     * Execute INSERT/UPDATE/DELETE query
     */
    public static function execute($sql) {
        return self::query($sql);
    }
    
    /**
     * Escape string for SQL
     */
    public static function escape($value) {
        return addslashes($value);
    }
    
    /**
     * Get single row
     */
    public static function getOne($sql) {
        $rows = self::select($sql);
        return $rows[0] ?? null;
    }
    
    /**
     * Insert and get ID
     */
    public static function insert($sql) {
        $result = self::execute($sql);
        return $result['insert_id'] ?? null;
    }
}

class Response {
    
    /**
     * Send JSON response
     */
    public static function json($data, $statusCode = 200) {
        http_response_code($statusCode);
        header('Content-Type: application/json');
        echo json_encode($data);
        exit;
    }
    
    /**
     * Send success response
     */
    public static function success($data = [], $message = 'Success') {
        self::json([
            'status' => true,
            'message' => $message,
            'data' => $data
        ]);
    }
    
    /**
     * Send error response
     */
    public static function error($message = 'Error occurred', $statusCode = 400, $data = []) {
        self::json([
            'status' => false,
            'message' => $message,
            'data' => $data
        ], $statusCode);
    }
    
    /**
     * Send unauthorized response
     */
    public static function unauthorized($message = 'Unauthorized') {
        self::error($message, 401);
    }
}

class Auth {
    
    /**
     * Generate random token
     */
    public static function generateToken() {
        return bin2hex(random_bytes(32));
    }
    
    /**
     * Hash password
     */
    public static function hashPassword($password) {
        return password_hash($password, PASSWORD_BCRYPT);
    }
    
    /**
     * Verify password
     */
    public static function verifyPassword($password, $hash) {
        return password_verify($password, $hash);
    }
    
    /**
     * Get current user from token
     */
    public static function getCurrentUser() {
        $headers = getallheaders();
        $auth = $headers['Authorization'] ?? '';
        
        if (!preg_match('/Bearer\s(.+)/', $auth, $matches)) {
            return null;
        }
        
        $token = $matches[1];
        $sql = "SELECT s.*, u.* FROM sessions s 
                JOIN users u ON s.user_id = u.id 
                WHERE s.token = '" . Database::escape($token) . "' 
                AND s.expires_at > NOW()
                LIMIT 1";
        
        return Database::getOne($sql);
    }
    
    /**
     * Require authentication
     */
    public static function requireAuth() {
        $user = self::getCurrentUser();
        if (!$user) {
            Response::unauthorized();
        }
        return $user;
    }
    
    /**
     * Create session
     */
    public static function createSession($userId) {
        $token = self::generateToken();
        $expiresAt = date('Y-m-d H:i:s', strtotime('+' . TOKEN_EXPIRY_HOURS . ' hours'));
        
        $sql = "INSERT INTO sessions (user_id, token, expires_at) 
                VALUES ($userId, '" . Database::escape($token) . "', '$expiresAt')";
        
        Database::execute($sql);
        
        return $token;
    }
    
    /**
     * Delete session
     */
    public static function deleteSession($token) {
        $sql = "DELETE FROM sessions WHERE token = '" . Database::escape($token) . "'";
        Database::execute($sql);
    }
}

class Validator {
    
    /**
     * Validate required fields
     */
    public static function required($data, $fields) {
        $missing = [];
        
        foreach ($fields as $field) {
            if (!isset($data[$field]) || trim($data[$field]) === '') {
                $missing[] = $field;
            }
        }
        
        if (count($missing) > 0) {
            Response::error('Missing required fields: ' . implode(', ', $missing));
        }
        
        return true;
    }
    
    /**
     * Validate email
     */
    public static function email($email) {
        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            Response::error('Invalid email address');
        }
        return true;
    }
    
    /**
     * Validate organization ID format
     */
    public static function orgId($orgId) {
        if (!preg_match('/^[A-Z0-9]{6}$/', $orgId)) {
            Response::error('Invalid organization ID format (must be 6 alphanumeric characters)');
        }
        return true;
    }

    /**
     * Clean phone number: remove spaces, dashes, and plus sign
     */
    public static function phone($phone) {
        if (!$phone) return '';
        return preg_replace('/[^\d]/', '', $phone);
    }
}
