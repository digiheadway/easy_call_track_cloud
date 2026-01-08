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

// PhonePe Payment Gateway Configuration
// Get your API keys from PhonePe Merchant Dashboard
define('PHONEPE_MERCHANT_ID', 'M22Z4XHRFV3KZ'); // Add your Merchant ID here
define('PHONEPE_SALT_KEY', '894044d6-795b-4dfd-8023-004d853520bc'); // Add your Salt Key here
define('PHONEPE_SALT_INDEX', '1'); // Salt Index
define('PHONEPE_ENV', 'PRODUCTION'); // 'UAT' or 'PRODUCTION'
define('PHONEPE_API_BASE', PHONEPE_ENV === 'PRODUCTION' 
    ? 'https://api.phonepe.com/apis/hermes' 
    : 'https://api-preprod.phonepe.com/apis/pg-sandbox');

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
