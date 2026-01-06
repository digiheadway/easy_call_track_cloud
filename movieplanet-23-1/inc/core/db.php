<?php
/**
 * Database Connection Singleton
 * Provides a centralized database connection with failover support
 * 
 * Usage: 
 *   require_once __DIR__ . '/db.php';
 *   $conn = getDbConnection();
 */

// Database connection parameters - Primary server
define('DB_HOST', 'localhost');
define('DB_USER', 'fmyfzvvwud');
define('DB_PASS', 'MG5xCnA8Pt');
define('DB_NAME', 'fmyfzvvwud');

// Secondary server (failover)
define('DB_HOST_2', 'localhost');
define('DB_USER_2', 'nwbfpgkywc');
define('DB_PASS_2', 'MZckmJkD5W');
define('DB_NAME_2', 'nwbfpgkywc');

// Singleton connection instance
$_dbConnection = null;

/**
 * Get database connection (creates if not exists)
 * @return mysqli|null Database connection or null on failure
 */
function getDbConnection() {
    global $_dbConnection;
    
    if ($_dbConnection !== null && $_dbConnection->ping()) {
        return $_dbConnection;
    }
    
    // Try primary server
    $_dbConnection = @new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);
    
    if ($_dbConnection->connect_error) {
        // Try secondary server
        $_dbConnection = @new mysqli(DB_HOST_2, DB_USER_2, DB_PASS_2, DB_NAME_2);
        
        if ($_dbConnection->connect_error) {
            error_log("Database connection failed: " . $_dbConnection->connect_error);
            return null;
        }
    }
    
    $_dbConnection->set_charset('utf8mb4');
    return $_dbConnection;
}

/**
 * Execute a prepared statement with parameters
 * @param string $sql SQL query with placeholders
 * @param string $types Parameter types (s=string, i=int, d=double, b=blob)
 * @param array $params Array of parameters
 * @return mysqli_result|bool Query result or false on failure
 */
function dbQuery($sql, $types = '', $params = []) {
    $conn = getDbConnection();
    if (!$conn) return false;
    
    $stmt = $conn->prepare($sql);
    if (!$stmt) {
        error_log("Prepare failed: " . $conn->error);
        return false;
    }
    
    if (!empty($params)) {
        $stmt->bind_param($types, ...$params);
    }
    
    if (!$stmt->execute()) {
        error_log("Execute failed: " . $stmt->error);
        $stmt->close();
        return false;
    }
    
    $result = $stmt->get_result();
    if ($result === false && $stmt->affected_rows >= 0) {
        // INSERT/UPDATE/DELETE query
        $affected = $stmt->affected_rows;
        $stmt->close();
        return $affected;
    }
    
    $stmt->close();
    return $result;
}

/**
 * Fetch single row from database
 * @param string $sql SQL query
 * @param string $types Parameter types
 * @param array $params Parameters
 * @return array|null Single row or null
 */
function dbFetchOne($sql, $types = '', $params = []) {
    $result = dbQuery($sql, $types, $params);
    if ($result && $result instanceof mysqli_result) {
        $row = $result->fetch_assoc();
        $result->free();
        return $row;
    }
    return null;
}

/**
 * Fetch all rows from database
 * @param string $sql SQL query
 * @param string $types Parameter types
 * @param array $params Parameters
 * @return array Array of rows
 */
function dbFetchAll($sql, $types = '', $params = []) {
    $result = dbQuery($sql, $types, $params);
    if ($result && $result instanceof mysqli_result) {
        $rows = $result->fetch_all(MYSQLI_ASSOC);
        $result->free();
        return $rows;
    }
    return [];
}

/**
 * Increment a counter column in queries table
 * @param string $column Column name to increment
 * @param string $query Query value
 * @return bool Success status
 */
function incrementQueryCounter($column, $query) {
    // Whitelist allowed columns to prevent SQL injection
    $allowedColumns = ['hits', 'not_this', 'down_tried'];
    if (!in_array($column, $allowedColumns)) {
        return false;
    }
    
    $sql = "UPDATE queries SET {$column} = {$column} + 1 WHERE query = ?";
    $result = dbQuery($sql, 's', [$query]);
    return $result !== false && $result > 0;
}

/**
 * Record hits for a query in a specific table
 */
function recordHit($tableName, $query) {
    $allowedTables = ['images', 'queries'];
    if (in_array($tableName, $allowedTables)) {
        return dbQuery("UPDATE {$tableName} SET hits = hits + 1 WHERE query = ?", "s", [$query]);
    }
    return false;
}

/**
 * Log hostname access
 */
function logHostname() {
    if (isset($_SERVER['HTTP_REFERER'])) {
        $referer_url = parse_url($_SERVER['HTTP_REFERER']);
        $domain = $referer_url['host'] ?? $_SERVER['HTTP_HOST'];
    } else {
        $domain = $_SERVER['HTTP_HOST'] ?? "unknown";
    }

    if ($domain) {
        return dbQuery(
            "INSERT INTO unique_domains (domain_name) VALUES (?) ON DUPLICATE KEY UPDATE request_count = request_count + 1, last_request = CURRENT_TIMESTAMP",
            "s",
            [$domain]
        );
    }
    return false;
}

/**
 * Get count of records in a table
 */
function dbGetCount($table) {
    $allowed = ['images', 'queries', 'hits_logger'];
    if (!in_array($table, $allowed)) return 0;
    
    $res = dbFetchAll("SELECT COUNT(*) as count FROM $table");
    return (int)($res[0]['count'] ?? 0);
}

// Legacy support - expose variables for old code
$servername = DB_HOST;
$username = DB_USER;
$password = DB_PASS;
$database = DB_NAME;
?>
