<?php
/**
 * Firebase FCM Config (Simple Version)
 */

$FCM_SERVICE_ACCOUNT = [
    "project_id" => "deviceadmin-d549b",
    "private_key" => "-----BEGIN PRIVATE KEY-----\nMIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCePr0puNdqBlf6\nz8Gc+zCIkt5JqBCEmAiMLFVPghGpIPRUABPGpk0kw2esSNYSHJvEgX5gmWBtz8SD\nYMA/1ZpS/ct25+RCbzaDfWz7HE5QIfu+p5hWfpCcGRp2hHF+dZLdDg2i+4WMkc9O\nUj1D/OUW+7cQvY9cOvoXDllNZe+O4Z+uhd88Kn91wkLa0YHRGO/UliV5Q1oUVORQ\ndsQNoLaiJsocrDMuH0xivrT7ezgHbkAEyB39y1H//KTB7Y7gvU3dqr4uW0ZuqCsf\nDy2j04Hh66/QCMoAPx6fwhWUemZduRuOv7B45aYxPIrAzK/QQq6ysuYxBtXOl0ms\nxyU7Ex9hAgMBAAECggEABCjpfITxbHYNCrTKud56ajosY8y6Pa6QkbT+A31zivXo\n11MS0gLb0fm9Jki6QCLSc08NQjNA9zUGJScaSalLBafKWRpvoCGT2f82adfWvQ12\nJHb70qkAQpq4NOWulTa4cwU6NXrBuhIojDY+Kh0GHYl1mJgHkmLSYYAXB9FcU63j\nAk51dKAaRuR0WORw3zbgO/o4iCKfq8J2ETuHXdqtgM4CDQH1pRolel+DaVuh4sRH\nPFHH4qeG2xJxIAzG7rJ6nFkjPRzChws6P/CAaLzRzmknLzClo2Cy8x9m/9WwVmWi\nnZzj3Yrcl0l07W1X8uhzs6ASUZZ5K+8KBN0SdAtWAQKBgQDYgCqXG3bi2O/IpSIT\nzJMa0GklISjogmPeQg0cOInS3bieU711Qg75rIXvAB0hUMZv+voJyKEKW47LDS0P\njGdfxb/LhqQ1UfNnPq9iwnMzSUOSqy7UOvDTDAr5la54YbmSbuel1aVaU7rgn8Zh\ndvKC5DEVZeu3sYZ3RvHSp+INYQKBgQC7HbMt/9M+szd1ttVjtqrnxnax3CQc1zp4\nXkKPU54kth8hML1sE+2Tn18jg9L2DRUA0I0Kv7eMLo+6pCf3G2YpCa0I+PhQB8IB\nyxjFkKPhLRoOE0zcO0nCw6oWyEs/OLyRT7p5V9ik489lNOHmT+3c00EMwtWQRxP6\nL2Vo6+hSAQKBgHG7gqQ27VFmHTEObsRv56dibJnwvYjHVqdfk3uLx/taNq4V16VN\nuog2tXVEXgkuYdBZzBhvqQnD51OL7GwKmhOZ0pOce473KiLGr2P2OoZqqnDWNJeR\nDwoPfYR3uVvMGqxuToqznVXCPp3Z0WFKF0PjlFVlYVryi20Fe+vp/bqBAoGAMm4T\nv58GUQy+MsSfCGvP7f2oOdFqDjfXs188MyLHKX/ILgrT0pAgZLv8STcbIWNvOLP8\nf1wiXO6joBrkBo5k30STVSq8ydz9ZbxWJdEQyfx83DRJb8Wu4IYZjmBXH2fsMoG5\ncUDCy9X2LIGvLIJ4Sh7JlmXgZy0w7ind/AQwrgECgYBM1xjicAxLnEKHJ0S9e7ar\nt3RyhWbv+iYX3BMgcxdBXV2W92XNy4FdeElTlyp1jGqhiWyV2rA3vNSbj3DyPHDG\nXlhSPXIrzGo7i+HELEsjAId3XtwddmXvMrzGP538/1fU3QTtZ0dBKlb6r4btgF92\nM6Wk9bVGfExncv0PxS+Xzw==\n-----END PRIVATE KEY-----\n",
    "client_email" => "firebase-adminsdk-fbsvc@deviceadmin-d549b.iam.gserviceaccount.com"
];

function getAccessToken() {
    global $FCM_SERVICE_ACCOUNT;
    static $token = null, $expiry = 0;
    
    if ($token && time() < $expiry) return $token;
    
    $now = time();
    $header = base64_encode(json_encode(['alg' => 'RS256', 'typ' => 'JWT']));
    $payload = base64_encode(json_encode([
        'iss' => $FCM_SERVICE_ACCOUNT['client_email'],
        'scope' => 'https://www.googleapis.com/auth/firebase.messaging',
        'aud' => 'https://oauth2.googleapis.com/token',
        'iat' => $now,
        'exp' => $now + 3600
    ]));
    
    $header = rtrim(strtr($header, '+/', '-_'), '=');
    $payload = rtrim(strtr($payload, '+/', '-_'), '=');
    
    openssl_sign("$header.$payload", $sig, $FCM_SERVICE_ACCOUNT['private_key'], OPENSSL_ALGO_SHA256);
    $sig = rtrim(strtr(base64_encode($sig), '+/', '-_'), '=');
    
    $jwt = "$header.$payload.$sig";
    
    $ch = curl_init('https://oauth2.googleapis.com/token');
    curl_setopt_array($ch, [
        CURLOPT_POST => true,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_POSTFIELDS => http_build_query([
            'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
            'assertion' => $jwt
        ])
    ]);
    $resp = json_decode(curl_exec($ch), true);
    curl_close($ch);
    
    $token = $resp['access_token'] ?? null;
    $expiry = $now + 3500;
    return $token;
}

function sendFCM($deviceToken, $data) {
    global $FCM_SERVICE_ACCOUNT;
    $accessToken = getAccessToken();
    
    // Convert all to strings
    foreach ($data as $k => $v) {
        $data[$k] = is_bool($v) ? ($v ? '1' : '0') : (string)$v;
    }
    
    $payload = [
        'message' => [
            'token' => $deviceToken,
            'data' => $data,
            'android' => ['priority' => 'high']
        ]
    ];
    
    $ch = curl_init("https://fcm.googleapis.com/v1/projects/{$FCM_SERVICE_ACCOUNT['project_id']}/messages:send");
    curl_setopt_array($ch, [
        CURLOPT_POST => true,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_HTTPHEADER => [
            'Authorization: Bearer ' . $accessToken,
            'Content-Type: application/json'
        ],
        CURLOPT_POSTFIELDS => json_encode($payload)
    ]);
    
    $resp = curl_exec($ch);
    $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    return ['success' => $code === 200, 'code' => $code, 'response' => json_decode($resp, true)];
}
