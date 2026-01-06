<?php
$token = isset($_GET['token']) ? htmlspecialchars($_GET['token']) : '';
$package = "com.clicktoearn.linkbox";
// Add token to referrer parameter for deferred deep linking
$referrerParam = $token ? "&referrer=" . urlencode("token=" . $token) : "";
$playStoreLink = "https://play.google.com/store/apps/details?id=" . $package . $referrerParam;
$intentUrl = "intent://open?token=" . $token . "#Intent;scheme=linkbox;package=" . $package . ";S.browser_fallback_url=" . urlencode($playStoreLink) . ";end";
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Opening Private Files...</title>
    <style>
        body {
            background-color: #121212;
            color: #E0E0E0;
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            font-family: sans-serif;
            margin: 0;
        }
    </style>
    <script>
        window.onload = function() {
            var token = "<?php echo $token; ?>";
            if (token) {
                // Attempt to open the app via Intent
                window.location.href = "<?php echo $intentUrl; ?>";
                
                // Fallback timeout not strictly needed with Intent scheme browser_fallback_url, 
                // but strictly speaking safe to have UI just in case.
            }
        };
    </script>
</head>
<body>
    <?php if ($token): ?>
        <div>
            <p>Opening Private Files...</p>
            <p><a href="<?php echo $intentUrl; ?>" style="color: #6C63FF;">Click here manually if not redirected</a></p>
        </div>
    <?php else: ?>
        <p>No token provided.</p>
    <?php endif; ?>
</body>
</html>
