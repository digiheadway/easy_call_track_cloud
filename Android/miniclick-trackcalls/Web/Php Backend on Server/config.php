<?php
/* =====================================
   CallCloud Admin - Configuration
   ===================================== */

// API Configuration
define('API_SECRET_TOKEN', '567898765678'); // Must match the token in mysql/file managers

// MySQL Manager Endpoint
define('MYSQL_MANAGER_URL', 'https://api.miniclickcrm.com/ai_mysql_manager.php');

// File Manager Endpoint
define('FILE_MANAGER_URL', 'https://api.miniclickcrm.com/ai_file_manager.php');

// Database Configuration (for reference - actual connection via MySQL Manager)
define('DB_HOST', 'localhost');
define('DB_USER', 'u542940820_easycalls');
define('DB_NAME', 'u542940820_easycalls');

// API Settings
define('TOKEN_EXPIRY_HOURS', 24);
define('API_VERSION', 'v1');

// Cashfree Payment Gateway Configuration
// Get your sandbox API keys from: https://merchant.cashfree.com/merchants/pg-dashboard
define('CASHFREE_APP_ID', ''); // Add your Cashfree App ID here
define('CASHFREE_SECRET_KEY', ''); // Add your Cashfree Secret Key here
define('CASHFREE_MODE', 'sandbox'); // 'sandbox' or 'production'
define('CASHFREE_API_BASE', CASHFREE_MODE === 'production' 
    ? 'https://api.cashfree.com/pg' 
    : 'https://sandbox.cashfree.com/pg');
define('CASHFREE_API_VERSION', '2023-08-01');

// CORS Settings
define('CORS_ALLOWED_ORIGINS', '*');

// Base URL for the API and resources
define('BASE_URL', 'https://api.miniclickcrm.com');

// File Upload Settings
define('MAX_FILE_SIZE', 50 * 1024 * 1024); // 50 MB
define('ALLOWED_FILE_TYPES', ['audio/mpeg', 'audio/wav', 'audio/mp3', 'audio/ogg']);

// Timezone
date_default_timezone_set('Asia/Kolkata');

// Error Reporting (disable in production)
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
