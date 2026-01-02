<?php
// Handle CORS preflight
if (isset($_SERVER['HTTP_ORIGIN'])) {
    header("Access-Control-Allow-Origin: {$_SERVER['HTTP_ORIGIN']}");
    header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
    header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");
    header("Access-Control-Allow-Credentials: true");
} else {
    header("Access-Control-Allow-Origin: *");
}

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once '../config.php';
require_once '../utils.php';

header("Content-Type: application/json");

// Authenticate
$user = Auth::requireAuth(); 

$action = $_GET['action'] ?? $_POST['action'] ?? '';

// Handle JSON input
if (empty($_POST) && strpos($_SERVER['CONTENT_TYPE'] ?? '', 'application/json') !== false) {
    $json = file_get_contents('php://input');
    $data = json_decode($json, true);
    if ($data) {
        $_POST = $data;
        if (!isset($action) || !$action) {
            $action = $data['action'] ?? '';
        }
    }
}

if ($action === 'get_usage') {
// ... (rest of the actions)
    // Return limits and usage directly from the authenticated user row
    // Note: $user contains the state at the time of auth (token check). 
    // If we want fresh data, we should re-query, but for now this is likely fine or we can re-select.
    
    // Let's re-query to be safe/fresh
    $freshUser = Database::getOne("SELECT * FROM users WHERE id = " . $user['id']);
    
    if (!$freshUser) {
        Response::error("User record not found");
    }
    
    Response::success([
        'plan_expiry' => $freshUser['plan_expiry_date'] ?? null,
        'allowed_users' => (int)($freshUser['allowed_users_count'] ?? 0),
        'allowed_storage_gb' => (float)($freshUser['allowed_storage_gb'] ?? 0),
        'storage_used_bytes' => (int)($freshUser['storage_used_bytes'] ?? 0),
        'last_storage_check' => $freshUser['last_storage_check'] ?? null
    ]);
}

if ($action === 'refresh_storage') {
    $orgId = $user['org_id'];
    if (!$orgId) Response::error("Invalid Organization");

    // Try multiple cases for the directory
    $possibleDirs = [
        dirname(__DIR__) . "/public/" . $orgId,
        dirname(__DIR__) . "/public/" . strtoupper($orgId),
        dirname(__DIR__) . "/public/" . strtolower($orgId)
    ];
    
    $publicDir = null;
    foreach ($possibleDirs as $dir) {
        if (is_dir($dir)) {
            $publicDir = $dir;
            break;
        }
    }
    
    $totalBytes = 0;
    if ($publicDir) {
        try {
            $iterator = new RecursiveIteratorIterator(
                new RecursiveDirectoryIterator($publicDir, RecursiveDirectoryIterator::SKIP_DOTS)
            );
            foreach ($iterator as $file) {
                if ($file->isFile()) {
                    $totalBytes += $file->getSize();
                }
            }
        } catch (Exception $e) {
            // Log error if needed: $e->getMessage()
        }
    }

    // Update DB
    $sql = "UPDATE users SET storage_used_bytes = $totalBytes, last_storage_check = NOW() WHERE id = " . $user['id'];
    Database::execute($sql);

    Response::success([
        'storage_used_bytes' => $totalBytes,
        'last_storage_check' => date('Y-m-d H:i:s'),
        'message' => 'Storage usage recalculated successfully'
    ]);
}

if ($action === 'cleanup_storage') {
    $days = (int)($_POST['days'] ?? ($_GET['days'] ?? 0));
    if ($days <= 0) Response::error("Invalid days specified");

    $orgId = $user['org_id'];
    if (!$orgId) Response::error("Invalid Organization");

    $possibleDirs = [
        dirname(__DIR__) . "/public/" . $orgId,
        dirname(__DIR__) . "/public/" . strtoupper($orgId),
        dirname(__DIR__) . "/public/" . strtolower($orgId)
    ];
    
    $publicDir = null;
    foreach ($possibleDirs as $dir) {
        if (is_dir($dir)) {
            $publicDir = $dir;
            break;
        }
    }

    $deleteCount = 0;
    $threshold = time() - ($days * 24 * 60 * 60);

    if ($publicDir) {
        $iterator = new RecursiveIteratorIterator(
            new RecursiveDirectoryIterator($publicDir, RecursiveDirectoryIterator::SKIP_DOTS),
            RecursiveIteratorIterator::CHILD_FIRST
        );

        foreach ($iterator as $file) {
            if ($file->isFile()) {
                $ext = strtolower($file->getExtension());
                if (in_array($ext, ['m4a', 'mp3', 'wav', 'aac'])) {
                    if ($file->getMTime() < $threshold) {
                        if (unlink($file->getRealPath())) {
                            $deleteCount++;
                        }
                    }
                }
            }
        }
    }

    Response::success([
        'deleted_count' => $deleteCount,
        'message' => "Successfully deleted $deleteCount recordings older than $days days."
    ]);
}

if ($action === 'get_usage_breakdown') {
    $orgId = $user['org_id'];
    $empIdFilter = $_GET['employee_id'] ?? null;
    $type = $_GET['type'] ?? 'month'; // year, month, date
    
    $possibleDirs = [
        dirname(__DIR__) . "/public/" . $orgId,
        dirname(__DIR__) . "/public/" . strtoupper($orgId),
        dirname(__DIR__) . "/public/" . strtolower($orgId)
    ];
    
    $publicDir = null;
    foreach ($possibleDirs as $dir) {
        if (is_dir($dir)) {
            $publicDir = $dir;
            break;
        }
    }

    $breakdown = [];
    
    if ($publicDir) {
        $iterator = new RecursiveIteratorIterator(
            new RecursiveDirectoryIterator($publicDir, RecursiveDirectoryIterator::SKIP_DOTS)
        );

        foreach ($iterator as $file) {
            if ($file->isFile()) {
                $ext = strtolower($file->getExtension());
                if (!in_array($ext, ['m4a', 'mp3', 'wav', 'aac'])) continue;

                // Employee Filter (Path: public/org/empId/...)
                $normalizedPublicDir = rtrim($publicDir, DIRECTORY_SEPARATOR) . DIRECTORY_SEPARATOR;
                $rel = str_replace($normalizedPublicDir, '', $file->getRealPath());
                $parts = explode(DIRECTORY_SEPARATOR, $rel);
                $empId = $parts[0];
                
                if ($empIdFilter && (string)$empId !== (string)$empIdFilter) continue;

                $size = $file->getSize();
                $mtime = $file->getMTime();
                
                $key = "";
                if ($type === 'year') $key = date('Y', $mtime);
                elseif ($type === 'date') $key = date('Y-m-d', $mtime);
                elseif ($type === 'employee') $key = $empId;
                else $key = date('Y-m', $mtime); // default month
                
                if (!isset($breakdown[$key])) {
                    $breakdown[$key] = [
                        'label' => $key,
                        'size' => 0,
                        'count' => 0
                    ];
                }
                $breakdown[$key]['size'] += $size;
                $breakdown[$key]['count']++;
            }
        }
    }

    // Sort by key descending
    krsort($breakdown);

    Response::success(array_values($breakdown));
}

if ($action === 'download_storage') {
    $orgId = $user['org_id'];
    $empId = $_GET['employee_id'] ?? $_GET['target_employee_id'] ?? null;
    $year = $_GET['year'] ?? null;
    $month = $_GET['month'] ?? null;
    $date = $_GET['date'] ?? null;
    
    $possibleDirs = [
        dirname(__DIR__) . "/public/" . $orgId,
        dirname(__DIR__) . "/public/" . strtoupper($orgId),
        dirname(__DIR__) . "/public/" . strtolower($orgId)
    ];
    
    $publicDir = null;
    foreach ($possibleDirs as $dir) {
        if (is_dir($dir)) {
            $publicDir = $dir;
            break;
        }
    }

    if (!$publicDir) Response::error("No recordings found");

    $zip = new ZipArchive();
    $zipName = "recordings_" . ($empId ?? 'all') . "_" . ($date ?? $month ?? $year ?? 'all') . ".zip";
    $zipFile = tempnam(sys_get_temp_dir(), 'rec');

    if ($zip->open($zipFile, ZipArchive::CREATE) !== TRUE) {
        Response::error("Could not create ZIP");
    }

    $iterator = new RecursiveIteratorIterator(
        new RecursiveDirectoryIterator($publicDir, RecursiveDirectoryIterator::SKIP_DOTS)
    );

    $added = 0;
    foreach ($iterator as $file) {
        if ($file->isFile()) {
            $ext = strtolower($file->getExtension());
            if (!in_array($ext, ['m4a', 'mp3', 'wav', 'aac'])) continue;

            $mtime = $file->getMTime();
            
            // Filter Employee
            $normalizedPublicDir = rtrim($publicDir, DIRECTORY_SEPARATOR) . DIRECTORY_SEPARATOR;
            $rel = str_replace($normalizedPublicDir, '', $file->getRealPath());
            $parts = explode(DIRECTORY_SEPARATOR, $rel);
            $fileEmpId = $parts[0];

            if ($empId && (string)$fileEmpId !== (string)$empId) continue;

            // Time Filters
            if ($year && date('Y', $mtime) !== $year) continue;
            if ($month && date('Y-m', $mtime) !== $month) continue;
            if ($date && date('Y-m-d', $mtime) !== $date) continue;

            $zip->addFile($file->getRealPath(), $file->getFilename());
            $added++;
        }
    }

    $zip->close();

    if ($added === 0) {
        @unlink($zipFile);
        Response::error("No files found matching criteria");
    }

    header('Content-Type: application/zip');
    header('Content-Disposition: attachment; filename="' . $zipName . '"');
    header('Content-Length: ' . filesize($zipFile));
    readfile($zipFile);
    @unlink($zipFile);
    exit;
}

if ($action === 'cleanup_filtered') {
    $orgId = $user['org_id'];
    $empId = $_POST['employee_id'] ?? $_POST['target_employee_id'] ?? null;
    $year = $_POST['year'] ?? null;
    $month = $_POST['month'] ?? null;
    $date = $_POST['date'] ?? null;
    
    $possibleDirs = [
        dirname(__DIR__) . "/public/" . $orgId,
        dirname(__DIR__) . "/public/" . strtoupper($orgId),
        dirname(__DIR__) . "/public/" . strtolower($orgId)
    ];
    
    $publicDir = null;
    foreach ($possibleDirs as $dir) {
        if (is_dir($dir)) {
            $publicDir = $dir;
            break;
        }
    }

    $deleteCount = 0;
    if ($publicDir) {
        $iterator = new RecursiveIteratorIterator(
            new RecursiveDirectoryIterator($publicDir, RecursiveDirectoryIterator::SKIP_DOTS),
            RecursiveIteratorIterator::CHILD_FIRST
        );

        foreach ($iterator as $file) {
            if ($file->isFile()) {
                $ext = strtolower($file->getExtension());
                if (!in_array($ext, ['m4a', 'mp3', 'wav', 'aac'])) continue;

                $mtime = $file->getMTime();

                // Employee Filter
                $normalizedPublicDir = rtrim($publicDir, DIRECTORY_SEPARATOR) . DIRECTORY_SEPARATOR;
                $rel = str_replace($normalizedPublicDir, '', $file->getRealPath());
                $parts = explode(DIRECTORY_SEPARATOR, $rel);
                $fileEmpId = $parts[0];

                if ($empId && (string)$fileEmpId !== (string)$empId) continue;

                // Time Filters
                if ($year && date('Y', $mtime) !== $year) continue;
                if ($month && date('Y-m', $mtime) !== $month) continue;
                if ($date && date('Y-m-d', $mtime) !== $date) continue;

                if (unlink($file->getRealPath())) {
                    $deleteCount++;
                }
            }
        }
    }

    Response::success(['deleted' => $deleteCount], "Deleted $deleteCount files.");
}

if ($action === 'validate_promo') {
    $code = trim($_GET['code'] ?? '');
    if (!$code) Response::error("Promo code is required");

    $promo = Database::getOne("SELECT * FROM promo_codes WHERE code = '" . Database::escape($code) . "' AND (expiry_date IS NULL OR expiry_date > NOW())");
    
    if (!$promo) {
        Response::error("Invalid or expired promo code");
    }

    Response::success([
        'code' => $promo['code'],
        'discount_percent' => (float)$promo['discount_percent']
    ]);
}

if ($action === 'get_plans') {
    // Return unit prices
    Response::success([
        'price_per_user' => 149,
        'price_per_gb' => 99,
        'currency' => 'Rs'
    ]);
}

if ($action === 'checkout') {
    $userCount = intval($_POST['user_count'] ?? 0);
    $storageGb = intval($_POST['storage_gb'] ?? 0);
    $duration = intval($_POST['duration_months'] ?? 1);
    $isRenewing = ($_POST['is_renewing'] ?? false) === true || ($_POST['is_renewing'] ?? '') === 'true';
    $remainingDays = intval($_POST['remaining_days'] ?? 0);
    $addUsers = intval($_POST['add_users'] ?? 0);
    $addStorage = intval($_POST['add_storage'] ?? 0);
    $promoCode = trim($_POST['promo_code'] ?? '');

    if ($userCount < 0 || $storageGb < 0) Response::error("Invalid quantities");
    if ($userCount === 0 && $storageGb === 0) Response::error("A plan must include at least 1 employee slot or some storage capacity.");

    $total = 0;

    if ($isRenewing && $duration > 0) {
        // RENEWAL MODE
        
        // 1. Charge for ALL resources for the NEW term
        $renewalCost = ($userCount * 149) + ($storageGb * 99);
        $renewalCost = $renewalCost * $duration;

        // Apply Duration Discount to renewal portion
        if ($duration === 3) $renewalCost *= 0.80;
        else if ($duration === 6) $renewalCost *= 0.75;
        else if ($duration === 12) $renewalCost *= 0.70;

        $total += $renewalCost;
        
        // 2. If adding resources AND plan is still active, also charge prorated for additions from today → current expiry
        if ($remainingDays > 0 && ($addUsers > 0 || $addStorage > 0)) {
            $proratedFactor = $remainingDays / 30;
            $additionsProratedCost = (max(0, $addUsers) * 149 + max(0, $addStorage) * 99) * $proratedFactor;
            $total += $additionsProratedCost;
        }

        // Expiry: Add to existing if active, else start from now
        $expiryText = "DATE_ADD(IF(plan_expiry_date > NOW(), plan_expiry_date, NOW()), INTERVAL $duration MONTH)";
    } else {
        // ADD-ON MODE: Mid-cycle addition only
        if ($remainingDays > 0) {
            $proratedFactor = $remainingDays / 30;
            $additionCost = (($addUsers * 149) + ($addStorage * 99)) * $proratedFactor;
            $total = $additionCost;
        } else {
            // Plan expired, charge full month for additions
            $total = (($addUsers * 149) + ($addStorage * 99));
        }
        // Keep existing expiry date for upgrades
        $expiryText = "plan_expiry_date";
    }

    // Apply Promo
    $promoDiscount = 0;
    if ($promoCode) {
        $promo = Database::getOne("SELECT * FROM promo_codes WHERE code = '$promoCode' AND (expiry_date IS NULL OR expiry_date > NOW())");
        if ($promo) {
            $promoDiscount = (float)$promo['discount_percent'];
            $total = $total - ($total * ($promoDiscount / 100));
        }
    }

    // Round to 2 decimal places
    $total = round($total, 2);

    // If total is 0 or negative, apply directly without payment
    if ($total <= 0) {
        // Direct update without payment
        $sql = "UPDATE users SET 
                allowed_users_count = $userCount, 
                allowed_storage_gb = $storageGb, 
                plan_expiry_date = $expiryText 
                WHERE id = " . $user['id'];
        
        Database::execute($sql);
        
        Response::success([
            'amount' => $total,
            'payment_required' => false,
            'message' => 'Plan updated successfully (no payment required)'
        ], "Plan updated successfully");
    }

    // Return order details for Cashfree payment
    Response::success([
        'amount' => $total,
        'payment_required' => true,
        'order_details' => [
            'user_count' => $userCount,
            'storage_gb' => $storageGb,
            'duration_months' => $duration,
            'is_renewing' => $isRenewing,
            'remaining_days' => $remainingDays,
            'add_users' => $addUsers,
            'add_storage' => $addStorage,
            'promo_code' => $promoCode,
            'promo_discount' => $promoDiscount
        ]
    ], "Proceed to payment");
}

if ($action === 'submit_upi_payment') {
    $utr = trim($_POST['utr'] ?? '');
    $amount = floatval($_POST['amount'] ?? 0);
    $userCount = intval($_POST['user_count'] ?? 0);
    $storageGb = intval($_POST['storage_gb'] ?? 0);
    $duration = intval($_POST['duration_months'] ?? 1);
    $isRenewing = ($_POST['is_renewing'] ?? false) === true || ($_POST['is_renewing'] ?? '') === 'true' || ($_POST['is_renewing'] ?? 0) == 1;
    
    if (!$utr) Response::error("Transaction ID (UTR) is required");
    
    // Check if UTR already exists
    $existing = Database::getOne("SELECT id FROM upi_payments WHERE utr = '" . Database::escape($utr) . "'");
    if ($existing) {
        Response::error("This Transaction ID has already been submitted.");
    }

    $orgId = $user['org_id'];
    $userId = $user['id'];
    $renewVal = $isRenewing ? 1 : 0;

    $sql = "INSERT INTO upi_payments (org_id, user_id, amount, utr, user_count, storage_gb, duration_months, is_renewing, status) 
            VALUES ('$orgId', $userId, $amount, '" . Database::escape($utr) . "', $userCount, $storageGb, $duration, $renewVal, 'completed')";
    
    $result = Database::execute($sql);
    
    if ($result['status']) {
        // Ensure notifications table exists
        Database::execute("CREATE TABLE IF NOT EXISTS notifications (id INT AUTO_INCREMENT PRIMARY KEY, org_id VARCHAR(50) NOT NULL, type VARCHAR(20) DEFAULT 'info', title VARCHAR(255) NOT NULL, message TEXT, is_read TINYINT(1) DEFAULT 0, link VARCHAR(255), created_at DATETIME DEFAULT CURRENT_TIMESTAMP, INDEX (org_id), INDEX (is_read), INDEX (created_at))");
        
        // Add notification
        $notifTitle = "UPI Payment Submitted";
        $notifMsg = "Your transaction (UTR: $utr) for Rs $amount has been recorded. Your plan will be updated automatically.";
        Database::execute("INSERT INTO notifications (org_id, type, title, message, link) VALUES ('$orgId', 'success', '" . Database::escape($notifTitle) . "', '" . Database::escape($notifMsg) . "', '/plans')");

        Response::success([
            'message' => 'Payment details submitted successfully. Your plan will be updated momentarily.',
            'utr' => $utr
        ], "Payment submitted successfully");
    } else {
        Response::error("Failed to record payment: " . ($result['error'] ?? 'Unknown error'));
    }
}

if ($action === 'confirm_manual_payment') {
    $userCount = intval($_POST['user_count'] ?? 0);
    $storageGb = intval($_POST['storage_gb'] ?? 0);
    $duration = intval($_POST['duration_months'] ?? 1);
    $isRenewing = ($_POST['is_renewing'] ?? false) === true || ($_POST['is_renewing'] ?? '') === 'true';
    $remainingDays = intval($_POST['remaining_days'] ?? 0);
    
    // We don't recalculate price here for validation as it's a manual trust-based flow
    // We just trust the intent to upgrade/renew
    
    // Determine Expiry Date Logic
    if ($isRenewing && $duration > 0) {
        $expiryText = "DATE_ADD(IF(plan_expiry_date > NOW(), plan_expiry_date, NOW()), INTERVAL $duration MONTH)";
    } else {
        $expiryText = "plan_expiry_date";
    }

    // Update User Plan
    $sql = "UPDATE users SET 
            allowed_users_count = $userCount, 
            allowed_storage_gb = $storageGb, 
            plan_expiry_date = $expiryText 
            WHERE id = " . $user['id'];
    
    Database::execute($sql);

    // TODO: Log this manual payment in payment_orders for records?
    // For now, simpler is better as requested.

    Response::success([
        'message' => 'Plan updated successfully via UPI manual payment'
    ], "Plan updated successfully");
}

Response::error("Invalid Action");
?>
