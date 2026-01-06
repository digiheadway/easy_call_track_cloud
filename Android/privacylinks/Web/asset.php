<?php
/**
 * Asset/Landing Page Router
 * Routes to different landing page designs based on URL parameters.
 */

// Enable error reporting
error_reporting(E_ALL);
ini_set('display_errors', 1); 
 * Usage: https://privacy.be6.in/{token}?name=FileName&type=folder&landing=2&preview=https://...
 * 
 * Parameters (all optional):
 * - token: The secure share token (from URL path)
 * - name: Display name of the shared content (default: "Shared Content")
 * - type: Content type - file, folder, link, video, image, audio, document (default: "file")
 * - landing: Landing page design variant 1, 2, 3 (default: 1)
 * - preview: URL to preview image (optional, shows icon if not provided)
 */

// Configuration
$defaultLanding = 1;
$availableLandings = [1, 2, 3, 4, 5, 6];

// Get parameters with defaults
$size = isset($_GET['size']) && !empty($_GET['size']) 
    ? htmlspecialchars($_GET['size'], ENT_QUOTES, 'UTF-8') 
    : '---';
$name = isset($_GET['name']) && !empty($_GET['name']) 
    ? htmlspecialchars($_GET['name'], ENT_QUOTES, 'UTF-8') 
    : 'Shared Content';

$by = isset($_GET['by']) && !empty($_GET['by']) 
    ? htmlspecialchars($_GET['by'], ENT_QUOTES, 'UTF-8') 
    : 'Private User';

$type = isset($_GET['type']) && !empty($_GET['type']) 
    ? strtolower(htmlspecialchars($_GET['type'], ENT_QUOTES, 'UTF-8')) 
    : 'file';

$landing = isset($_GET['landing']) ? intval($_GET['landing']) : $defaultLanding;

$token = isset($_GET['token']) && !empty($_GET['token']) 
    ? htmlspecialchars($_GET['token'], ENT_QUOTES, 'UTF-8') 
    : 'shared';

// Preview image URL (optional)
$previewUrl = isset($_GET['preview']) && !empty($_GET['preview']) 
    ? filter_var($_GET['preview'], FILTER_VALIDATE_URL) 
    : null;

// Social proof & display options (all optional with defaults)
$downloads = isset($_GET['downloads']) && !empty($_GET['downloads']) 
    ? htmlspecialchars($_GET['downloads'], ENT_QUOTES, 'UTF-8') 
    : '1M+';

$rating = isset($_GET['rating']) && !empty($_GET['rating']) 
    ? htmlspecialchars($_GET['rating'], ENT_QUOTES, 'UTF-8') 
    : '4.5★';

$views = isset($_GET['views']) ? intval($_GET['views']) : rand(50, 500);
$timeAgo = isset($_GET['time']) ? intval($_GET['time']) : rand(1, 59);
$opens = isset($_GET['opens']) ? intval($_GET['opens']) : rand(10, 99);

// Badge text (optional)
$badgeText = isset($_GET['badge']) && !empty($_GET['badge']) 
    ? htmlspecialchars($_GET['badge'], ENT_QUOTES, 'UTF-8') 
    : null; // null = use default per landing page

// Validate landing page number
if (!in_array($landing, $availableLandings)) {
    $landing = $defaultLanding;
}

// Get content type icon & label
function getTypeInfo($type) {
    $types = [
        'file'     => ['icon' => '📄', 'label' => 'File'],
        'folder'   => ['icon' => '📁', 'label' => 'Folder'],
        'link'     => ['icon' => '🔗', 'label' => 'Link'],
        'video'    => ['icon' => '🎬', 'label' => 'Video'],
        'image'    => ['icon' => '🖼️', 'label' => 'Image'],
        'photo'    => ['icon' => '📷', 'label' => 'Photo'],
        'audio'    => ['icon' => '🎵', 'label' => 'Audio'],
        'music'    => ['icon' => '🎶', 'label' => 'Music'],
        'document' => ['icon' => '📑', 'label' => 'Document'],
        'pdf'      => ['icon' => '📕', 'label' => 'PDF'],
        'zip'      => ['icon' => '📦', 'label' => 'Archive'],
        'app'      => ['icon' => '📲', 'label' => 'App'],
        'apk'      => ['icon' => '📲', 'label' => 'APK'],
    ];
    return $types[$type] ?? $types['file'];
}

$typeInfo = getTypeInfo($type);

// Detect user platform
$userAgent = $_SERVER['HTTP_USER_AGENT'] ?? '';
$isAndroid = stripos($userAgent, 'Android') !== false;
$isIOS = stripos($userAgent, 'iPhone') !== false || stripos($userAgent, 'iPad') !== false;

// App package name
$packageName = 'com.clicktoearn.linkbox';

// Build URLs
$currentUrl = "https://" . $_SERVER['HTTP_HOST'] . $_SERVER['REQUEST_URI'];
$appDeepLink = "https://privacy.be6.in/open?token=" . urlencode($token);
$playStoreUrl = "https://play.google.com/store/apps/details?id={$packageName}&referrer=" . urlencode("token={$token}");
$appStoreUrl = "https://apps.apple.com/app/private-files/id123456789";

// Intent URL for Android (Forcing custom scheme linkbox://open)
$intentUrl = "intent://open?token=" . urlencode($token) . "#Intent;"
    . "scheme=linkbox;"
    . "package={$packageName};"
    . "S.browser_fallback_url=" . urlencode($playStoreUrl) . ";"
    . "end";

// Report/DMCA URLs
$reportUrl = "mailto:support@be6.in?subject=Report%20Content&body=URL:%20" . urlencode($currentUrl);
$dmcaUrl = "mailto:dmca@be6.in?subject=DMCA%20Takedown%20Request&body=URL:%20" . urlencode($currentUrl);

// Include the selected landing page
$landingFile = __DIR__ . "/landingpages/landing{$landing}.php";

if (file_exists($landingFile)) {
    include $landingFile;
} else {
    include __DIR__ . "/landingpages/landing1.php";
}
