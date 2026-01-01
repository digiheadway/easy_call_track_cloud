<?php
/* =====================================
   CallCloud Admin - Database Initializer
   ===================================== */

require_once 'config.php';

/**
 * Initialize database schema via MySQL Manager
 */
function initializeDatabase() {
    echo "CallCloud Admin - Database Initialization\n";
    echo "=========================================\n\n";
    
    // Read schema file
    $schema = file_get_contents(__DIR__ . '/schema.sql');
    
    // Split into individual statements
    $statements = array_filter(
        array_map('trim', explode(';', $schema)),
        function($stmt) { return !empty($stmt); }
    );
    
    echo "Found " . count($statements) . " SQL statements to execute.\n\n";
    
    $success = 0;
    $failed = 0;
    
    foreach ($statements as $index => $sql) {
        echo "Executing statement " . ($index + 1) . "... ";
        
        $ch = curl_init(MYSQL_MANAGER_URL);
        
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_POST => true,
            CURLOPT_HTTPHEADER => [
                'Authorization: Bearer ' . API_SECRET_TOKEN,
                'Content-Type: application/json'
            ],
            CURLOPT_POSTFIELDS => json_encode(['sql' => $sql . ';'])
        ]);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        
        $result = json_decode($response, true);
        
        if ($httpCode === 200 && $result['status']) {
            echo "✓ Success\n";
            $success++;
        } else {
            echo "✗ Failed\n";
            if (isset($result['error'])) {
                echo "   Error: " . $result['error'] . "\n";
            }
            $failed++;
        }
    }
    
    echo "\n=========================================\n";
    echo "Database Initialization Complete\n";
    echo "Success: $success | Failed: $failed\n";
    echo "=========================================\n";
}

// Run initialization
initializeDatabase();
