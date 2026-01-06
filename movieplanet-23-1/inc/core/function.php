<?php
/**
 * Global Utility Functions
 */

require_once __DIR__ . '/config.php';

/**
 * Returns asset URL with cache-busting version
 */
function asset($path) {
    if (file_exists($_SERVER['DOCUMENT_ROOT'] . $path)) {
        $v = filemtime($_SERVER['DOCUMENT_ROOT'] . $path);
        return $path . "?v=" . $v;
    }
    return $path;
}

/**
 * Safe redirect
 */
function redirect($url) {
    header("Location: " . $url);
    exit();
}

/**
 * Get current search query
 */
function getSearchQuery() {
    return trim($_GET['q'] ?? $_COOKIE['q'] ?? '');
}

/**
 * Generate Search URL
 */
function getSearchUrl($q = '') {
    return "/msearch.php?q=" . urlencode($q);
}
?>
