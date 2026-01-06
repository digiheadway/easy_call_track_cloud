<?php
/**
 * Global Configuration & State Manager
 * Parses the request and sets up the environment
 */
require_once __DIR__ . '/db.php';

// 1. Domain Parsing
$arr = explode('.', $_SERVER["SERVER_NAME"]);
$canonical = (empty($_SERVER['HTTPS']) ? 'http://' : 'https://') . $_SERVER['HTTP_HOST'] . strtok($_SERVER['REQUEST_URI'], '?');

$subdomainexactly = $arr[0] ?? 'default';
$subdomainexactly_for_domain_checking = $subdomainexactly;
$domain = $arr[1] ?? 'local';
$ext = $arr[2] ?? 'com';
$fulldomain = $domain . "." . $ext;
$domainwithext = $fulldomain . "?ref=" . $subdomainexactly;

// 2. State Management (Cookies/GET)
if (isset($_GET['dm'])) {
    $subdomainexactly = $_GET['dm'];
    setcookie('dm', $_GET['dm'], time() + 3600, "/");
} elseif (isset($_COOKIE['dm'])) {
    $subdomainexactly = $_COOKIE['dm'];
}

// 3. Formatting
$subdomain = ucwords($subdomainexactly);
$subdomaintitle = ucwords(strtolower(str_replace("-", " ", $subdomainexactly)));
$this_month = date("M Y");

// 4. URLs
$query_params = $_SERVER['QUERY_STRING'];
$func_search_url = "/msearch.php?" . $query_params;
$func_search_url_prefix = "/msearch.php?q=";

// 5. Validation
$allowed_subdomains = require __DIR__ . '/subdomains.php';
$titlesuffix = "Download or Watch Online";

if ($domain !== 'local' && !in_array($subdomainexactly_for_domain_checking, $allowed_subdomains)) {
    header('HTTP/1.0 404 Not Found');
    header("Location: https://filmyzilla.moviesda10.com/");
    exit;
}

// 6. Debug Mode
if (isset($_GET['debug'])) {
    ini_set('display_errors', 1);
    error_reporting(E_ALL);
}

// 7. Global Constants
define('APP_VERSION', '1.2.0');
?>
