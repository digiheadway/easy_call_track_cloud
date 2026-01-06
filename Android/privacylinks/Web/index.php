<?php
/**
 * Main Index / Token Redirector
 * Optimized for Private/Secure placeholders when landing params are missing.
 */

// Enable error reporting
error_reporting(E_ALL);
ini_set('display_errors', 1);

// Get the token from the URL path
$requestUri = $_SERVER['REQUEST_URI'];
$path = parse_url($requestUri, PHP_URL_PATH);
$token = trim($path, '/');

// List of files/paths that should be served normally
$excludedPaths = ['index.php', 'index.html', 'contact.php', 'privacy-policy.html', 'terms-of-usage.html', 'delete-account.html', 'style.css'];
foreach ($excludedPaths as $excluded) {
    if ($token === $excluded || strpos($token, $excluded) === 0) { exit; }
}
if (strpos($token, '.well-known') === 0) { exit; }

// Serve homepage for root access
if (empty($token)) {
    if (file_exists('index.html')) { readfile('index.html'); } else { echo "Private Files Secure Storage"; }
    exit;
}

// Clean the token
$token = preg_replace('/[^a-zA-Z0-9_-].*$/', '', $token);

// Route to asset.php if we have landing params
if (isset($_GET['landing']) || isset($_GET['name']) || isset($_GET['type'])) {
    $_GET['token'] = $token;
    include __DIR__ . '/asset.php';
    exit;
}

// Platform detection
$userAgent = $_SERVER['HTTP_USER_AGENT'] ?? '';
$isAndroid = stripos($userAgent, 'Android') !== false;
$isIOS = stripos($userAgent, 'iPhone') !== false || stripos($userAgent, 'iPad') !== false;
$packageName = 'com.clicktoearn.linkbox';

// Build Redirect URLs
$appDeepLink = "https://privacy.be6.in/open?token=" . urlencode($token);
$playStoreUrl = "https://play.google.com/store/apps/details?id={$packageName}&referrer=" . urlencode("token={$token}");
$intentUrl = "intent://open?token=" . urlencode($token) . "#Intent;scheme=linkbox;package={$packageName};S.browser_fallback_url=" . urlencode($playStoreUrl) . ";end";

// Generate Fake but Professional Private Metadata
$filetype = ["Shared Private Link", "Secure Content Hub", "Private Data Session", "Encrypted Access Port", "Protected Content"][(int)(hexdec(hash('crc32', $token . 'type')) % 5)];
$views = ((int)hexdec(hash('crc32', $token . 'v')) % 5000) + 1200;
$opens = ((int)hexdec(hash('crc32', $token . 'o')) % 800) + 300;
$timeAgo = ((int)hexdec(hash('crc32', $token . 't')) % 50) + 5;
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Private Content | Private Files</title>
    <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🔒</text></svg>">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root{--primary:#3b82f6;--primary-dark:#2563eb;--bg:#0d1117;--surface:#161b22;--border:#30363d;--text:#f0f6fc;--text-muted:#8b949e;--warning:#f59e0b;--success:#10b981}
        *{margin:0;padding:0;box-sizing:border-box}
        body{font-family:'Inter',sans-serif;background: radial-gradient(circle at 0% 0%,#161b22 0%,#0d1117 100%);min-height:100vh;display:flex;flex-direction:column;color:var(--text);overflow-x:hidden}
        
        .header{background:rgba(22,27,34,0.8);backdrop-filter:blur(10px);border-bottom:1px solid var(--border);padding:1rem 1.5rem;display:flex;align-items:center;justify-content:space-between;width:100%;position:sticky;top:0;z-index:100}
        .header .logo{font-weight:700;font-size:1.1rem;color:var(--primary);display:flex;align-items:center;gap:0.5rem}
        .header .user-info{font-size:0.8rem;color:var(--text-muted)}
        
        .main{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;width:100%}
        
        .trust-bar{display:flex;gap:1.5rem;margin-bottom:1.5rem;font-size:.7rem;color:var(--text-muted)}
        .trust-bar span{display:flex;align-items:center;gap:.3rem}
        
        .card{width:100%;max-width:420px;background:var(--surface);border:1px solid var(--border);border-radius:24px;overflow:hidden;box-shadow:0 20px 50px rgba(0,0,0,0.5);animation:fadeIn 0.5s ease}
        @keyframes fadeIn{from{opacity:0;transform:translateY(20px)}to{opacity:1;transform:translateY(0)}}
        
        .card-header .app-icon-featured{z-index:2;filter:drop-shadow(0 0 15px rgba(59,130,246,0.2));animation:iconGlow 3s infinite alternate}
        @keyframes iconGlow{from{filter:drop-shadow(0 0 10px rgba(59,130,246,0.3))}to{filter:drop-shadow(0 0 30px rgba(59,130,246,0.6))}}
        .card-header .app-icon-featured img{display:block;object-fit:contain}
        .card-header{background:linear-gradient(180deg, #161b22 0%, #0d1117 100%);padding:2.5rem 1.5rem;display:flex;flex-direction:column;align-items:center;gap:1.5rem;border-bottom:1px solid var(--border)}
        .card-header .text-content{text-align:center}
        
        .card-header h1{font-size:1.3rem;font-weight:700;margin-bottom:0.4rem;letter-spacing:-0.5px}
        .card-header .type{font-size:0.85rem;color:var(--text-muted);font-weight:500}
        
        .stats-row{display:flex;gap:1rem;padding-bottom:1.25rem;border-bottom:1px solid var(--border)}
        .stat{flex:1;text-align:center}
        .stat .num{font-size:1.1rem;font-weight:700;color:var(--text)}
        .stat .lbl{font-size:.65rem;color:var(--text-muted);text-transform:uppercase;letter-spacing:1px}
        
        .info-row{display:flex;align-items:center;gap:.75rem;padding:.85rem 0;border-bottom:1px solid rgba(255,255,255,0.03);font-size:.9rem}
        .info-row:last-child{border-bottom:none; padding: 1rem;}
        .info-row .label{color:var(--text-muted);flex:1}
        .info-row .value{font-weight:500;color:var(--text)}
        .info-row .status-pill{background:rgba(16,185,129,0.1);color:var(--success);padding:0.2rem 0.6rem;border-radius:50px;font-size:0.75rem;border:1px solid rgba(16,185,129,0.2)}
        
        .btn{display:flex;align-items:center;justify-content:center;gap:.6rem;width:100%;padding:1.1rem;border-radius:14px;font-weight:700;font-size:1rem;cursor:pointer;border:none;transition:all 0.2s;text-decoration:none}
        .btn-primary{background:var(--primary);color:#fff;box-shadow:0 10px 25px rgba(59,130,246,0.3)}
        .btn-primary:hover{background:var(--primary-dark);transform:translateY(-2px);box-shadow:0 15px 35px rgba(59,130,246,0.4)}
        
        .footer{padding:3rem 1.5rem;text-align:center;color:var(--text-muted);font-size:0.75rem;background:rgba(0,0,0,0.1)}
        .footer-links{margin-top:1rem;display:flex;justify-content:center;gap:1.5rem}
        .footer-links a{color:var(--text-muted);text-decoration:none}
        
        .modal-overlay{display:none;position:fixed;inset:0;background:rgba(0,0,0,0.9);backdrop-filter:blur(10px);z-index:1000;align-items:center;justify-content:center;padding:1.5rem}
        .modal-overlay.show{display:flex}
        .modal{background:var(--surface);border:1px solid var(--border);border-radius:28px;width:100%;max-width:380px;padding:2.5rem 1.5rem;text-align:center;position:relative}
        .modal h2{font-size:1.5rem;margin-bottom:0.75rem}
        .modal p{color:var(--text-muted);font-size:0.9rem;margin-bottom:1.5rem;line-height:1.5}
        .qr-box{background:#fff;padding:1rem;border-radius:20px;display:inline-block;margin-bottom:1.5rem;box-shadow:0 10px 30px rgba(0,0,0,0.3)}
        .qr-box img{display:block;width:180px;height:180px}
        
        .btn-outline{background:transparent;border:1px solid var(--border);color:var(--text);margin-top:0.5rem}
    </style>
</head>
<body>
    <div class="header">
        <div class="logo">
            <img src="app_icon_clear.png" width="28" height="28">
            Private Files
        </div>
    </div>

    <div class="main">
        <div class="card">
            <div class="card-header">
                <div class="app-icon-featured">
                    <img src="app_icon_clear.png" width="90" height="90">
                </div>
                <div class="text-content">
                    <h1>Private File Id #<?php echo $token; ?></h1>
                    <div class="type">Shared by Private Files User</div>
                </div>
                <div class="actions" style="width: 100%; max-width: 320px; margin-top: 0.5rem;">
                    <button onclick="handleOpen()" class="btn btn-primary" id="mainBtn">
                        Open Now
                    </button>
                    <?php if ($isAndroid): ?>
                    <p id="redirectMsg" style="margin-top:0.75rem; font-size:0.8rem; color:var(--primary); font-weight:500; display:flex; align-items:center; justify-content:center; gap:0.5rem">
                        <span class="spinner" style="width:14px; height:14px; border-width:2px"></span>
                        Opening app...
                    </p>
                    <?php endif; ?>
                </div>
            </div>

            <div class="card-body">
               
                <div class="info-list">
                    <div class="info-row"><span class="label">Access Using</span><span class="value">Private Files App only</span></div>
                </div>
            </div>
        </div>
    </div>

    <footer class="footer">
        <p>This content is securely shared via Private Files protocol.</p>
        <div class="footer-links">
            <a href="/privacy-policy.html">Privacy</a>
            <a href="/terms-of-usage.html">Terms</a>
            <a href="mailto:support@be6.in">Report</a>
        </div>
    </footer>

    <div class="modal-overlay" id="modal">
        <div class="modal">
            <div style="font-size:3rem; margin-bottom:1rem">🔒</div>
            <h2>Unlock Content</h2>
            <p>This private content is encrypted for your security. To view or download, please scan this code with the <strong>Private Files Android App</strong>.</p>
            <div class="qr-box">
                <img src="https://api.qrserver.com/v1/create-qr-code/?size=300x300&margin=10&data=<?php echo urlencode($appDeepLink); ?>" alt="QR">
            </div>
            <div class="modal-btns">
                <button onclick="copyLink()" class="btn btn-primary">📋 Copy Access Link</button>
                <button onclick="closeModal()" class="btn btn-outline">Dismiss</button>
            </div>
        </div>
    </div>

    <script>
        const intentUrl = '<?php echo addslashes($intentUrl); ?>';
        const shareLink = '<?php echo addslashes($appDeepLink); ?>';
        const isAndroid = <?php echo $isAndroid ? 'true' : 'false'; ?>;

        function handleOpen() {
            if (isAndroid) {
                window.location.href = intentUrl;
            } else {
                document.getElementById('modal').classList.add('show');
            }
        }

        function closeModal() {
            document.getElementById('modal').classList.remove('show');
        }

        function copyLink() {
            navigator.clipboard.writeText(shareLink).then(() => {
                alert('Access link copied!');
            }).catch(() => {
                prompt('Copy access link:', shareLink);
            });
        }

        // Close modal on click outside
        document.getElementById('modal').addEventListener('click', function(e) {
            if (e.target.id === 'modal') closeModal();
        });

        // Auto-redirect for Android users after a short delay
        if (isAndroid) {
            setTimeout(() => {
                const btn = document.getElementById('mainBtn');
                if(btn) {
                    btn.innerText = 'Redirecting...';
                    btn.style.opacity = '0.7';
                    btn.disabled = true;
                }
                window.location.href = intentUrl;
            }, 1500);
        }
    </script>
</body>
</html>
