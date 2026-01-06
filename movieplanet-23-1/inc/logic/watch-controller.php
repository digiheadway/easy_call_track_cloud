<?php
/**
 * Watch Page Content Controller
 * Determines what data to show (TMDB vs Query)
 */

require_once dirname(__DIR__) . '/core/function.php';
require_once dirname(__DIR__) . '/core/tmdb-helper.php';

$movieId = $_GET['id'] ?? null;
$type = $_GET['type'] ?? 'movie';
$searchquery = $_GET['q'] ?? '';

$movieData = [];

if ($movieId) {
    // 1. Case: Direct ID from TMDB (used by msearch2.php)
    $rawData = fetchTMDBData($type, $movieId);
    $movieData = normalizeTMDBData($rawData, $type);
    $movieData['file_name'] = ($type === 'tv' ? 'Series' : 'Movie') . $movieId . '.mp4';
} elseif ($searchquery) {
    // 2. Case: Search Query (used by msearch.php)
    $movieData = [
        'title' => ucwords(urldecode($searchquery)),
        'image' => '/assets/img/not-found.jpg', // Placeholder, JS usually updates this
        'description' => "Get ready for an incredible experience with " . ucwords(urldecode($searchquery)) . ".",
        'stats' => [
            'Print' => 'Full HD 1080p',
            'Size' => '700 MB - 2.5 GB'
        ],
        'file_name' => 'Movie53676.mp4'
    ];
}
?>
