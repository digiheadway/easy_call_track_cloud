<?php
/**
 * TMDB Helper
 * Provdes server-side access to TMDB API with caching
 */

require_once __DIR__ . '/db.php';

define('TMDB_API_KEY', '576092042f883b27b40fdb9b2b5247b9'); // Extracted from original proxy

/**
 * Fetch movie/tv data from TMDB
 */
function fetchTMDBData($type, $id) {
    if (!in_array($type, ['movie', 'tv'])) return null;
    
    $cacheKey = "tmdb_{$type}_{$id}";
    // Optional: Implement DB caching here
    
    $url = "https://api.themoviedb.org/3/{$type}/{$id}?api_key=" . TMDB_API_KEY . "&language=en-US";
    $response = @file_get_contents($url);
    
    if (!$response) return null;
    
    return json_decode($response, true);
}

/**
 * Clean up TMDB data for our UI
 */
function normalizeTMDBData($data, $type) {
    if (!$data) return null;
    
    return [
        'title' => ($type === 'tv') ? ($data['name'] ?? '') : ($data['title'] ?? ''),
        'description' => $data['overview'] ?? '',
        'image' => isset($data['poster_path']) ? "https://image.tmdb.org/t/p/w500" . $data['poster_path'] : '/assets/img/not-found.jpg',
        'stats' => [
            'Release' => ($type === 'tv') ? ($data['first_air_date'] ?? '') : ($data['release_date'] ?? ''),
            'Rating' => ($data['vote_average'] ?? '0') . ' / 10',
            'Duration' => ($type === 'tv') ? ($data['number_of_seasons'] ?? '0') . ' Seasons' : ($data['runtime'] ?? '0') . ' Min',
            'Genres' => implode(', ', array_column($data['genres'] ?? [], 'name'))
        ]
    ];
}
?>
