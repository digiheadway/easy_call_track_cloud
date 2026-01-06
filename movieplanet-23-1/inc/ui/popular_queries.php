<?php
/**
 * Popular Queries Component
 * Displays popular movie queries as links
 */

require_once dirname(__DIR__) . '/core/db.php';

// Fetch popular queries
$queries = dbFetchAll(
    "SELECT query, anchor FROM images ORDER BY priority DESC LIMIT 16"
);

// Render links
$counter = 0;
foreach ($queries as $row) {
    $counter++;
    $query = htmlspecialchars($row['query']);
    $anchor = htmlspecialchars($row['anchor']);
    
    echo '<a class="category_list" href="' . $func_search_url_prefix . $query . '" onclick="record(\'popular_movie_click\', \'image' . $counter . '\');">';
    echo '<span class="catogry_list_label">' . $anchor . '</span>';
    echo '<span><img src="/assets/img/arrow-right.svg" alt=""></span></a>';
}

if ($counter === 0) {
    echo "No popular queries found.";
}
?>