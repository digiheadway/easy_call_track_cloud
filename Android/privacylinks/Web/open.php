<?php
$token = isset($_GET['token']) ? htmlspecialchars($_GET['token']) : '';
$uniqueId = isset($_GET['uniqueid']) ? htmlspecialchars($_GET['uniqueid']) : '';
$landing = isset($_GET['landing']) ? htmlspecialchars($_GET['landing']) : '';

// Tracking Logic
require_once __DIR__ . '/track.php';
if ($uniqueId) {
    trackClick($uniqueId, $landing);
}

$package = "com.clicktoearn.linkbox";

// Build Referrer Query
$referrerParts = [];
if ($token) $referrerParts[] = "token=" . $token;
if ($uniqueId) $referrerParts[] = "uniqueid=" . $uniqueId;
if ($landing) $referrerParts[] = "landing=" . $landing;

$referrerQuery = implode("&", $referrerParts);
$referrerParam = !empty($referrerQuery) ? "&referrer=" . urlencode($referrerQuery) : "";

$playStoreLink = "https://play.google.com/store/apps/details?id=" . $package . $referrerParam;

// Construct Intent URL
// Note: We only strictly need token in the main intent URI host/path if the app uses that for matching. 
// But checking existing logic: "intent://open?token="...
// We can append other params to the query string part of the intent URI: intent://open?token=...&uniqueid=...
$intentQueryParams = "token=" . urlencode($token);
if ($uniqueId) $intentQueryParams .= "&uniqueid=" . urlencode($uniqueId);
// (Landing usually not needed for direct app open, but can add if app supports it)

$intentUrl = "intent://open?" . $intentQueryParams . "#Intent;scheme=linkbox;package=" . $package . ";S.browser_fallback_url=" . urlencode($playStoreLink) . ";end";
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
