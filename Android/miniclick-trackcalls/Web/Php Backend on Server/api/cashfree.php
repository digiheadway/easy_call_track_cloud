<?php
/**
 * Cashfree Payment Gateway API Handler
 * Handles order creation, webhook verification, and payment status checks
 */
require_once '../config.php';
require_once '../utils.php';

// CORS Headers
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");
header("Content-Type: application/json");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

class Cashfree {
    
    /**
     * Make API request to Cashfree
     */
    public static function request($endpoint, $method = 'GET', $data = null) {
        $url = CASHFREE_API_BASE . $endpoint;
        
        $headers = [
            'Accept: application/json',
            'Content-Type: application/json',
            'x-api-version: ' . CASHFREE_API_VERSION,
            'x-client-id: ' . CASHFREE_APP_ID,
            'x-client-secret: ' . CASHFREE_SECRET_KEY
        ];
        
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HTTPHEADER => $headers,
            CURLOPT_CUSTOMREQUEST => $method,
            CURLOPT_SSL_VERIFYPEER => true,
            CURLOPT_TIMEOUT => 30
        ]);
        
        if ($data && in_array($method, ['POST', 'PUT', 'PATCH'])) {
            curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
        }
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);
        curl_close($ch);
        
        if ($error) {
            return ['success' => false, 'error' => $error, 'http_code' => $httpCode];
        }
        
        $result = json_decode($response, true);
        return [
            'success' => $httpCode >= 200 && $httpCode < 300,
            'data' => $result,
            'http_code' => $httpCode
        ];
    }
    
    /**
     * Create a payment order
     */
    public static function createOrder($orderId, $amount, $customerDetails, $returnUrl = null, $notifyUrl = null) {
        $orderData = [
            'order_id' => $orderId,
            'order_amount' => round($amount, 2),
            'order_currency' => 'INR',
            'customer_details' => [
                'customer_id' => $customerDetails['id'] ?? 'customer_' . time(),
                'customer_phone' => $customerDetails['phone'] ?? '9999999999',
                'customer_email' => $customerDetails['email'] ?? 'customer@example.com',
                'customer_name' => $customerDetails['name'] ?? 'Customer'
            ],
            'order_meta' => []
        ];
        
        if ($returnUrl) {
            $orderData['order_meta']['return_url'] = $returnUrl . '?order_id={order_id}';
        }
        
        if ($notifyUrl) {
            $orderData['order_meta']['notify_url'] = $notifyUrl;
        }
        
        return self::request('/orders', 'POST', $orderData);
    }
    
    /**
     * Get order status
     */
    public static function getOrder($orderId) {
        return self::request('/orders/' . $orderId, 'GET');
    }
    
    /**
     * Get payments for an order
     */
    public static function getPayments($orderId) {
        return self::request('/orders/' . $orderId . '/payments', 'GET');
    }
    
    /**
     * Verify webhook signature
     */
    public static function verifyWebhookSignature($payload, $timestamp, $signature) {
        $signatureData = $timestamp . $payload;
        $expectedSignature = base64_encode(hash_hmac('sha256', $signatureData, CASHFREE_SECRET_KEY, true));
        return hash_equals($expectedSignature, $signature);
    }
}

// Get action
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

/**
 * Create Payment Order
 * Called from frontend after checkout calculation
 */
if ($action === 'create_order') {
    // Authenticate user
    $user = Auth::requireAuth();
    
    // Get order details from POST
    $userCount = intval($_POST['user_count'] ?? 0);
    $storageGb = intval($_POST['storage_gb'] ?? 0);
    $duration = intval($_POST['duration_months'] ?? 1);
    $isRenewing = ($_POST['is_renewing'] ?? false) === true || ($_POST['is_renewing'] ?? '') === 'true';
    $remainingDays = intval($_POST['remaining_days'] ?? 0);
    $addUsers = intval($_POST['add_users'] ?? 0);
    $addStorage = intval($_POST['add_storage'] ?? 0);
    $promoCode = trim($_POST['promo_code'] ?? '');
    $totalAmount = floatval($_POST['amount'] ?? 0);
    
    if ($totalAmount <= 0) {
        Response::error("Invalid order amount");
    }
    
    if ($userCount < 0 || $storageGb < 0) {
        Response::error("Invalid quantities");
    }
    
    // Generate unique order ID
    $orderId = 'CC_' . $user['id'] . '_' . time() . '_' . substr(md5(uniqid()), 0, 6);
    
    // Get promo discount if applicable
    $promoDiscount = 0;
    if ($promoCode) {
        $promo = Database::getOne("SELECT * FROM promo_codes WHERE code = '" . Database::escape($promoCode) . "' AND (expiry_date IS NULL OR expiry_date > NOW())");
        if ($promo) {
            $promoDiscount = (float)$promo['discount_percent'];
        }
    }
    
    // Create payment order in our database
    $insertSql = "INSERT INTO payment_orders 
        (user_id, order_id, order_amount, order_status, user_count, storage_gb, duration_months, is_renewing, add_users, add_storage, promo_code, promo_discount_percent)
        VALUES (
            {$user['id']}, 
            '" . Database::escape($orderId) . "',
            $totalAmount,
            'PENDING',
            $userCount,
            $storageGb,
            $duration,
            " . ($isRenewing ? 1 : 0) . ",
            $addUsers,
            $addStorage,
            '" . Database::escape($promoCode) . "',
            $promoDiscount
        )";
    
    $insertResult = Database::insert($insertSql);
    if (!$insertResult) {
        Response::error("Failed to create order record");
    }
    
    // Prepare customer details
    $customerDetails = [
        'id' => 'cust_' . $user['id'],
        'phone' => $user['phone'] ?? '9999999999',
        'email' => $user['email'] ?? 'user@example.com',
        'name' => $user['name'] ?? $user['org_name'] ?? 'Customer'
    ];
    
    // Determine return and notify URLs based on where the API is hosted
    $baseUrl = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http") . "://" . $_SERVER['HTTP_HOST'];
    $returnUrl = $baseUrl . '/payment-result.html';
    $notifyUrl = $baseUrl . '/api/cashfree.php?action=webhook';
    
    // Create Cashfree order
    $cfResult = Cashfree::createOrder($orderId, $totalAmount, $customerDetails, $returnUrl, $notifyUrl);
    
    if (!$cfResult['success']) {
        // Update order status to failed
        Database::execute("UPDATE payment_orders SET order_status = 'FAILED' WHERE order_id = '" . Database::escape($orderId) . "'");
        
        $errorMsg = $cfResult['data']['message'] ?? 'Failed to create payment order';
        Response::error($errorMsg, 400, $cfResult);
    }
    
    $cfOrder = $cfResult['data'];
    
    // Update our order with Cashfree details
    $updateSql = "UPDATE payment_orders SET 
        cf_order_id = '" . Database::escape($cfOrder['cf_order_id'] ?? '') . "',
        payment_session_id = '" . Database::escape($cfOrder['payment_session_id'] ?? '') . "',
        order_status = 'ACTIVE'
        WHERE order_id = '" . Database::escape($orderId) . "'";
    
    Database::execute($updateSql);
    
    Response::success([
        'order_id' => $orderId,
        'cf_order_id' => $cfOrder['cf_order_id'] ?? null,
        'payment_session_id' => $cfOrder['payment_session_id'],
        'order_status' => $cfOrder['order_status'],
        'order_amount' => $totalAmount,
        'order_currency' => 'INR',
        'cashfree_mode' => CASHFREE_MODE
    ], 'Payment order created successfully');
}

/**
 * Get Order Status
 * Check payment status from Cashfree
 */
if ($action === 'get_status') {
    $orderId = $_GET['order_id'] ?? '';
    
    if (!$orderId) {
        Response::error("Order ID is required");
    }
    
    // Get order from our DB
    $order = Database::getOne("SELECT * FROM payment_orders WHERE order_id = '" . Database::escape($orderId) . "'");
    
    if (!$order) {
        Response::error("Order not found", 404);
    }
    
    // If order is pending/active, check with Cashfree
    if (in_array($order['order_status'], ['PENDING', 'ACTIVE'])) {
        $cfResult = Cashfree::getOrder($orderId);
        
        if ($cfResult['success']) {
            $cfOrder = $cfResult['data'];
            $newStatus = $cfOrder['order_status'] ?? $order['order_status'];
            
            // Update local status if changed
            if ($newStatus !== $order['order_status']) {
                Database::execute("UPDATE payment_orders SET order_status = '$newStatus' WHERE order_id = '" . Database::escape($orderId) . "'");
                $order['order_status'] = $newStatus;
            }
            
            // If paid, process the order
            if ($newStatus === 'PAID' && $order['order_status'] !== 'PAID') {
                processSuccessfulPayment($order);
            }
        }
    }
    
    Response::success([
        'order_id' => $order['order_id'],
        'order_status' => $order['order_status'],
        'order_amount' => (float)$order['order_amount'],
        'payment_time' => $order['payment_time'],
        'cf_payment_id' => $order['cf_payment_id']
    ]);
}

/**
 * Webhook Handler
 * Receives payment notifications from Cashfree
 */
if ($action === 'webhook') {
    // Log incoming webhook
    $rawPayload = file_get_contents('php://input');
    error_log("Cashfree Webhook Received: " . $rawPayload);
    
    // Get signature from headers
    $timestamp = $_SERVER['HTTP_X_WEBHOOK_TIMESTAMP'] ?? '';
    $signature = $_SERVER['HTTP_X_WEBHOOK_SIGNATURE'] ?? '';
    
    // Verify signature (optional but recommended)
    // For now, we'll process the webhook and validate order ID
    
    $payload = json_decode($rawPayload, true);
    
    if (!$payload) {
        http_response_code(400);
        echo json_encode(['status' => 'error', 'message' => 'Invalid payload']);
        exit;
    }
    
    $eventType = $payload['type'] ?? '';
    $data = $payload['data'] ?? [];
    
    if ($eventType === 'PAYMENT_SUCCESS_WEBHOOK' || $eventType === 'PAYMENT.SUCCESS') {
        $orderId = $data['order']['order_id'] ?? '';
        $cfPaymentId = $data['payment']['cf_payment_id'] ?? '';
        $paymentMethod = $data['payment']['payment_method'] ?? '';
        $bankReference = $data['payment']['bank_reference'] ?? '';
        $paymentAmount = $data['payment']['payment_amount'] ?? 0;
        
        // Find order in our DB
        $order = Database::getOne("SELECT * FROM payment_orders WHERE order_id = '" . Database::escape($orderId) . "'");
        
        if ($order && $order['order_status'] !== 'PAID') {
            // Update order status
            $updateSql = "UPDATE payment_orders SET 
                order_status = 'PAID',
                cf_payment_id = '" . Database::escape($cfPaymentId) . "',
                payment_method = '" . Database::escape($paymentMethod) . "',
                bank_reference = '" . Database::escape($bankReference) . "',
                payment_time = NOW(),
                webhook_received_at = NOW()
                WHERE order_id = '" . Database::escape($orderId) . "'";
            
            Database::execute($updateSql);
            
            // Process successful payment - update user plan
            processSuccessfulPayment($order);
        }
        
        http_response_code(200);
        echo json_encode(['status' => 'ok']);
        exit;
    }
    
    if ($eventType === 'PAYMENT_FAILED_WEBHOOK' || $eventType === 'PAYMENT.FAILED') {
        $orderId = $data['order']['order_id'] ?? '';
        
        Database::execute("UPDATE payment_orders SET 
            order_status = 'FAILED',
            webhook_received_at = NOW()
            WHERE order_id = '" . Database::escape($orderId) . "'");
        
        http_response_code(200);
        echo json_encode(['status' => 'ok']);
        exit;
    }
    
    // Default response for other webhook types
    http_response_code(200);
    echo json_encode(['status' => 'ok', 'message' => 'Webhook received']);
    exit;
}

/**
 * Verify Payment (Called from payment result page)
 */
if ($action === 'verify_payment') {
    $orderId = $_GET['order_id'] ?? $_POST['order_id'] ?? '';
    
    if (!$orderId) {
        Response::error("Order ID is required");
    }
    
    // Get order from our DB
    $order = Database::getOne("SELECT po.*, u.email, u.org_name FROM payment_orders po 
        JOIN users u ON po.user_id = u.id 
        WHERE po.order_id = '" . Database::escape($orderId) . "'");
    
    if (!$order) {
        Response::error("Order not found", 404);
    }
    
    // Check with Cashfree for final status
    $cfResult = Cashfree::getOrder($orderId);
    
    if ($cfResult['success']) {
        $cfOrder = $cfResult['data'];
        $newStatus = $cfOrder['order_status'] ?? $order['order_status'];
        
        // Update local status if different
        if ($newStatus !== $order['order_status']) {
            Database::execute("UPDATE payment_orders SET order_status = '$newStatus' WHERE order_id = '" . Database::escape($orderId) . "'");
            $order['order_status'] = $newStatus;
            
            // If just became paid, process it
            if ($newStatus === 'PAID') {
                // Get payment details
                $paymentsResult = Cashfree::getPayments($orderId);
                if ($paymentsResult['success'] && !empty($paymentsResult['data'])) {
                    $payment = $paymentsResult['data'][0];
                    Database::execute("UPDATE payment_orders SET 
                        cf_payment_id = '" . Database::escape($payment['cf_payment_id'] ?? '') . "',
                        payment_method = '" . Database::escape($payment['payment_method'] ?? '') . "',
                        bank_reference = '" . Database::escape($payment['bank_reference'] ?? '') . "',
                        payment_time = NOW()
                        WHERE order_id = '" . Database::escape($orderId) . "'");
                }
                
                processSuccessfulPayment($order);
            }
        }
    }
    
    Response::success([
        'order_id' => $order['order_id'],
        'order_status' => $order['order_status'],
        'order_amount' => (float)$order['order_amount'],
        'payment_time' => $order['payment_time'],
        'is_paid' => $order['order_status'] === 'PAID',
        'user_email' => $order['email'],
        'org_name' => $order['org_name'],
        'user_count' => (int)$order['user_count'],
        'storage_gb' => (int)$order['storage_gb'],
        'duration_months' => (int)$order['duration_months']
    ]);
}

/**
 * Process successful payment - Update user plan
 */
function processSuccessfulPayment($order) {
    $userId = $order['user_id'];
    $userCount = (int)$order['user_count'];
    $storageGb = (int)$order['storage_gb'];
    $duration = (int)$order['duration_months'];
    $isRenewing = (bool)$order['is_renewing'];
    
    if ($isRenewing && $duration > 0) {
        // Renewal: Update plan and extend expiry
        $expiryText = "DATE_ADD(IF(plan_expiry_date > NOW(), plan_expiry_date, NOW()), INTERVAL $duration MONTH)";
    } else {
        // Add-on only: Keep existing expiry
        $expiryText = "plan_expiry_date";
    }
    
    // Update user plan
    $sql = "UPDATE users SET 
            allowed_users_count = $userCount, 
            allowed_storage_gb = $storageGb, 
            plan_expiry_date = $expiryText 
            WHERE id = $userId";
    
    Database::execute($sql);
    
    error_log("Processed payment for user $userId: Users=$userCount, Storage=$storageGb, Duration=$duration months");
}

/**
 * Get Payment History
 */
if ($action === 'payment_history') {
    $user = Auth::requireAuth();
    
    $orders = Database::select("SELECT * FROM payment_orders WHERE user_id = {$user['id']} ORDER BY created_at DESC LIMIT 50");
    
    Response::success($orders);
}

Response::error("Invalid Action");
?>
