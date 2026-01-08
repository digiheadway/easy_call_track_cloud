<?php
/**
 * PhonePe Payment Gateway API Handler
 * Handles order creation, webhook verification, and payment status checks
 */
require_once '../config.php';
require_once '../utils.php';

// CORS Headers
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-VERIFY");
header("Content-Type: application/json");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

class PhonePe {
    
    /**
     * Make API request to PhonePe
     */
    public static function request($endpoint, $payload = [], $headers = []) {
        $url = PHONEPE_API_BASE . $endpoint;
        
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HTTPHEADER => $headers,
            CURLOPT_POST => true,
            CURLOPT_POSTFIELDS => json_encode($payload),
            CURLOPT_TIMEOUT => 30
        ]);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);
        curl_close($ch);
        
        if ($error) {
            return ['success' => false, 'error' => $error, 'http_code' => $httpCode];
        }
        
        $result = json_decode($response, true);
        return [
            'success' => $result['success'] ?? false,
            'data' => $result['data'] ?? [],
            'code' => $result['code'] ?? '',
            'message' => $result['message'] ?? '',
            'http_code' => $httpCode
        ];
    }

    /**
     * Create a payment order
     */
    public static function createOrder($orderId, $amount, $customerDetails, $returnUrl = null, $callbackUrl = null) {
        $payload = [
            'merchantId' => PHONEPE_MERCHANT_ID,
            'merchantTransactionId' => $orderId,
            'merchantUserId' => $customerDetails['id'],
            'amount' => round($amount * 100), // Amount in paise
            'redirectUrl' => $returnUrl . '?order_id=' . $orderId,
            'redirectMode' => 'REDIRECT',
            'callbackUrl' => $callbackUrl,
            'mobileNumber' => $customerDetails['phone'],
            'paymentInstrument' => [
                'type' => 'PAY_PAGE'
            ]
        ];

        $base64Payload = base64_encode(json_encode($payload));
        $saltKey = PHONEPE_SALT_KEY;
        $saltIndex = PHONEPE_SALT_INDEX;

        $stringToHash = $base64Payload . "/pg/v1/pay" . $saltKey;
        $sha256 = hash('sha256', $stringToHash);
        $xVerify = $sha256 . "###" . $saltIndex;

        $requestPayload = ['request' => $base64Payload];
        $headers = [
            'Content-Type: application/json',
            'X-VERIFY: ' . $xVerify,
            'accept: application/json'
        ];

        return self::request('/pg/v1/pay', $requestPayload, $headers);
    }
    
    /**
     * Get order status
     */
    public static function getStatus($merchantTransactionId) {
        $saltKey = PHONEPE_SALT_KEY;
        $saltIndex = PHONEPE_SALT_INDEX;
        $merchantId = PHONEPE_MERCHANT_ID;

        $endpoint = "/pg/v1/status/$merchantId/$merchantTransactionId";
        
        $stringToHash = $endpoint . $saltKey;
        $sha256 = hash('sha256', $stringToHash);
        $xVerify = $sha256 . "###" . $saltIndex;

        $url = PHONEPE_API_BASE . $endpoint;
        
        $headers = [
            'Content-Type: application/json',
            'X-VERIFY: ' . $xVerify,
            'X-MERCHANT-ID: ' . $merchantId,
            'accept: application/json'
        ];

        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HTTPHEADER => $headers,
            CURLOPT_CUSTOMREQUEST => 'GET',
            CURLOPT_TIMEOUT => 30
        ]);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);
        curl_close($ch);

         if ($error) {
            return ['success' => false, 'error' => $error, 'http_code' => $httpCode];
        }

        $result = json_decode($response, true);
        return [
            'success' => $result['success'] ?? false,
            'code' => $result['code'] ?? '',
            'data' => $result['data'] ?? [],
            'message' => $result['message'] ?? ''
        ];
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
 */
if ($action === 'create_order') {
    // Authenticate user
    $user = Auth::requireAuth();
    
    // Get order details from POST
    $userCount = intval($_POST['user_count'] ?? 0);
    $storageGb = intval($_POST['storage_gb'] ?? 0);
    $duration = intval($_POST['duration_months'] ?? 1);
    $isRenewing = ($_POST['is_renewing'] ?? false) === true || ($_POST['is_renewing'] ?? '') === 'true';
    $addUsers = intval($_POST['add_users'] ?? 0);
    $addStorage = intval($_POST['add_storage'] ?? 0);
    $promoCode = trim($_POST['promo_code'] ?? '');
    $totalAmount = floatval($_POST['amount'] ?? 0);
    
    if ($totalAmount <= 0) {
        Response::error("Invalid order amount");
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
    
    // Determine return and notify URLs
    $baseUrl = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http") . "://" . $_SERVER['HTTP_HOST'];
    $returnUrl = $baseUrl . '/payment-result.html';
    $callbackUrl = $baseUrl . '/api/phonepe.php?action=callback';
    
    // Create PhonePe order
    $ppResult = PhonePe::createOrder($orderId, $totalAmount, $customerDetails, $returnUrl, $callbackUrl);
    
    if (!$ppResult['success']) {
        Database::execute("UPDATE payment_orders SET order_status = 'FAILED' WHERE order_id = '" . Database::escape($orderId) . "'");
        Response::error($ppResult['message'] ?? 'Failed to initiate payment', 400, $ppResult);
    }
    
    $redirectUrl = $ppResult['data']['instrumentResponse']['redirectInfo']['url'] ?? '';
    
    if (empty($redirectUrl)) {
        Response::error("No redirect URL received from Payment Gateway", 500);
    }

    Response::success([
        'order_id' => $orderId,
        'payment_url' => $redirectUrl, // Frontend should redirect here
        'order_amount' => $totalAmount,
        'order_currency' => 'INR'
    ], 'Payment initiated successfully');
}

/**
 * Verify Payment (called from payment-result.html)
 */
if ($action === 'verify_payment') {
    $orderId = $_GET['order_id'] ?? $_POST['order_id'] ?? '';
    
    if (!$orderId) {
        Response::error("Order ID is required");
    }
    
    // Get order from DB
    $order = Database::getOne("SELECT po.*, u.email, u.org_name FROM payment_orders po 
        JOIN users u ON po.user_id = u.id 
        WHERE po.order_id = '" . Database::escape($orderId) . "'");
    
    if (!$order) {
        Response::error("Order not found", 404);
    }
    
    // Check status with PhonePe
    $ppResult = PhonePe::getStatus($orderId);
    
    if ($ppResult['success']) {
        $status = $ppResult['code']; // "PAYMENT_SUCCESS", "PAYMENT_ERROR", "PAYMENT_PENDING"
        $newStatus = $order['order_status'];

        if ($status === 'PAYMENT_SUCCESS') {
            $newStatus = 'PAID';
        } else if ($status === 'PAYMENT_ERROR') {
            $newStatus = 'FAILED';
        } else {
             // Keep as PENDING or whatever it is, unless specifically PENDING
            if ($status === 'PAYMENT_PENDING') {
                $newStatus = 'PENDING';
            }
        }
        
        if ($newStatus !== $order['order_status']) {
            $transactionId = $ppResult['data']['transactionId'] ?? ''; // PhonePe Transaction Id
            
            // Map PhonePe fields to existing columns (reusing cf_ columns)
            Database::execute("UPDATE payment_orders SET 
                order_status = '$newStatus',
                cf_payment_id = '" . Database::escape($transactionId) . "',
                payment_method = '" . Database::escape($ppResult['data']['paymentInstrument']['type'] ?? 'PHONEPE') . "',
                payment_time = NOW()
                WHERE order_id = '" . Database::escape($orderId) . "'");
                
            $order['order_status'] = $newStatus;
            
            if ($newStatus === 'PAID') {
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
 * Handle S2S Callback
 */
if ($action === 'callback') {
    // Read the callback
    $rawPayload = file_get_contents('php://input');
    $data = json_decode($rawPayload, true);
    
    // PhonePe sends base64 encoded response in 'response' key
    $base64Response = $data['response'] ?? '';
    
    // Verify X-VERIFY header if needed (Recommended)
    $xVerify = $_SERVER['HTTP_X_VERIFY'] ?? '';
    
    if ($base64Response && $xVerify) {
        $saltKey = PHONEPE_SALT_KEY;
        $saltIndex = PHONEPE_SALT_INDEX;
        
        $calculatedHash = hash('sha256', $base64Response . $saltKey) . "###" . $saltIndex;
        
        if ($calculatedHash === $xVerify) {
            $responseJson = base64_decode($base64Response);
            $responseObj = json_decode($responseJson, true);
            
            if ($responseObj && isset($responseObj['data']['merchantTransactionId'])) {
                $orderId = $responseObj['data']['merchantTransactionId'];
                $code = $responseObj['code'] ?? '';
                $transactionId = $responseObj['data']['transactionId'] ?? '';
                
                $order = Database::getOne("SELECT * FROM payment_orders WHERE order_id = '" . Database::escape($orderId) . "'");
                
                if ($order && $order['order_status'] !== 'PAID') {
                    if ($code === 'PAYMENT_SUCCESS') {
                        Database::execute("UPDATE payment_orders SET 
                            order_status = 'PAID',
                            cf_payment_id = '" . Database::escape($transactionId) . "',
                            webhook_received_at = NOW()
                            WHERE order_id = '" . Database::escape($orderId) . "'");
                            
                        // Re-fetch to process
                        $order['user_count'] = (int)$order['user_count'];
                        $order['storage_gb'] = (int)$order['storage_gb'];
                        $order['duration_months'] = (int)$order['duration_months'];
                        $order['is_renewing'] = (bool)$order['is_renewing'];
                        processSuccessfulPayment($order);
                        
                    } else if ($code === 'PAYMENT_ERROR') {
                         Database::execute("UPDATE payment_orders SET 
                            order_status = 'FAILED',
                            webhook_received_at = NOW()
                            WHERE order_id = '" . Database::escape($orderId) . "'");
                    }
                }
            }
        }
    }
    
    // Always return success to acknowledge receipt
    http_response_code(200);
    echo json_encode(['status' => 'OK']);
    exit;
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
    error_log("Processed PhonePe payment for user $userId: Users=$userCount, Storage=$storageGb, Duration=$duration months");
}

Response::error("Invalid Action");
?>
