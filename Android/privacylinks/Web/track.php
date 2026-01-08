<?php
// track.php - Handles tracking logic for clicks and installs
// This file should be included in asset.php (for clicks) and log_flow.php (for installs)

// Database Configuration
$host = 'localhost';
$dbname = 'u240376517_private_files';
$username = 'u240376517_private_files';
$password = '2YsX@@l2T';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
} catch(PDOException $e) {
    // Silent fail in production or log to file
    error_log("Tracking DB Conn Error: " . $e->getMessage());
    file_put_contents(__DIR__ . '/debug_log.txt', "Connection Failed: " . $e->getMessage() . "\n", FILE_APPEND);
    $pdo = null;
}

// Function to get current IST time
function getISTValues() {
    $date = new DateTime('now', new DateTimeZone('UTC'));
    $date->setTimezone(new DateTimeZone('Asia/Kolkata'));
    return [
        'datetime' => $date->format('Y-m-d H:i:s'),
        'date' => $date->format('Y-m-d')
    ];
}

/**
 * Tracks a page view event
 * @param string $uniqueId The campaign/ref ID
 * @param string $landing The landing page ID/name
 */
function trackView($uniqueId, $landing) {
    global $pdo;
    
    // Check if duplicate view (24h)
    $cookieName = 'v_' . md5($uniqueId);
    if (isset($_COOKIE[$cookieName])) return;

    if (!$pdo || empty($uniqueId)) return;

    $ist = getISTValues();
    $now = $ist['datetime'];
    $today = $ist['date'];

    $sql = "INSERT INTO tracking_stats 
            (uniqueId, views, landingpage, date, created_at, updated_at) 
            VALUES 
            (:uid, 1, :landing, :today, :now, :now)
            ON DUPLICATE KEY UPDATE 
            views = views + 1,
            updated_at = :now";

    try {
        $stmt = $pdo->prepare($sql);
        $stmt->execute([
            ':uid' => $uniqueId,
            ':landing' => $landing,
            ':now' => $now,
            ':today' => $today
        ]);
        // Set cookie for 24 hours
        setcookie($cookieName, '1', time() + 86400, "/");
    } catch (PDOException $e) {
        error_log("Track View Error: " . $e->getMessage());
    }
}

/**
 * Tracks a click event
 * @param string $uniqueId The campaign/ref ID
 * @param string $landing The landing page ID/name
 */
function trackClick($uniqueId, $landing) {
    global $pdo;
    
    // Check if duplicate click (24h)
    $cookieName = 'c_' . md5($uniqueId);
    if (isset($_COOKIE[$cookieName])) return;
    
    // DEBUG: Write to log file
    $logMsg = date('Y-m-d H:i:s') . " - Tracking Click: UID=$uniqueId, Landing=$landing\n";
    file_put_contents(__DIR__ . '/debug_log.txt', $logMsg, FILE_APPEND);

    if (!$pdo) {
        file_put_contents(__DIR__ . '/debug_log.txt', "DB Connection Missing!\n", FILE_APPEND);
        return;
    }
    
    if (empty($uniqueId)) return;

    $ist = getISTValues();
    $now = $ist['datetime'];
    $today = $ist['date'];

    // Upsert logic: Insert if new (unlikely for click without view but possible), update if exists
    $sql = "INSERT INTO tracking_stats 
            (uniqueId, clicks, landingpage, last_click, date, created_at, updated_at) 
            VALUES 
            (:uid, 1, :landing, :now, :today, :now, :now)
            ON DUPLICATE KEY UPDATE 
            clicks = clicks + 1,
            last_click = :now,
            updated_at = :now";

    try {
        $stmt = $pdo->prepare($sql);
        $stmt->execute([
            ':uid' => $uniqueId,
            ':landing' => $landing,
            ':now' => $now,
            ':today' => $today
        ]);
        
        // Set cookie for 24 hours
        setcookie($cookieName, '1', time() + 86400, "/");

        file_put_contents(__DIR__ . '/debug_log.txt', "Click Tracked Successfully\n", FILE_APPEND);
    } catch (PDOException $e) {
        error_log("Track Click Error: " . $e->getMessage());
        file_put_contents(__DIR__ . '/debug_log.txt', "PDO Error: " . $e->getMessage() . "\n", FILE_APPEND);
    }
}

/**
 * Tracks an installation event
 * @param string $uniqueId The campaign/ref ID
 * @param string $landing The landing page ID/name
 * @param bool $isNewInstall True if first open, False if already installed (re-open)
 */
function trackInstall($uniqueId, $landing, $isNewInstall) {
    global $pdo;
    if (!$pdo || empty($uniqueId)) return;

    $ist = getISTValues();
    $now = $ist['datetime'];
    $today = $ist['date'];

    // Determine which counter to increment
    $counterField = $isNewInstall ? 'new_installs' : 'already_installed';

    // Upsert logic: Insert (with 0 clicks if created here) or Update
    // Note: Usually a record exists from the Click event, but we handle the edge case where it doesn't
    $sql = "INSERT INTO tracking_stats 
            (uniqueId, landingpage, $counterField, last_install, date, created_at, updated_at) 
            VALUES 
            (:uid, :landing, 1, :now, :today, :now, :now)
            ON DUPLICATE KEY UPDATE 
            $counterField = $counterField + 1,
            last_install = :now,
            updated_at = :now";

    try {
        $stmt = $pdo->prepare($sql);
        $stmt->execute([
            ':uid' => $uniqueId,
            ':landing' => $landing ?: 'unknown',
            ':now' => $now,
            ':today' => $today
        ]);
    } catch (PDOException $e) {
        error_log("Track Install Error: " . $e->getMessage());
    }
}
?>
