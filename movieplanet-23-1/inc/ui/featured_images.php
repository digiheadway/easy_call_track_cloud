<?php
/**
 * Featured Images Component
 * Displays featured movie images from database
 */

require_once dirname(__DIR__) . '/core/db.php';

// Fetch featured images
$images = dbFetchAll(
    "SELECT query, imageUrl, anchor FROM images ORDER BY priority DESC LIMIT 16"
);

// Render images
$counter = 0;
foreach ($images as $row) {
    $counter++;
    $query = htmlspecialchars($row['query']);
    $imageUrl = htmlspecialchars($row['imageUrl']);
    $anchor = htmlspecialchars($row['anchor']);
    
    echo '<a href="' . $func_search_url_prefix . $query . '" onclick="record(\'featured_movie_click\', \'image' . $counter . '\');" class="movies_item_box">';
    echo '<div class="movies_image_box">';
    echo '<img src="' . $imageUrl . '" loading="lazy" alt="' . $anchor . '">';
    echo '<div class="movies_quality_type">' . $anchor . '</div>';
    echo '</div>';
    echo '</a>';
}

if ($counter === 0) {
    echo "No featured images found.";
}
?>
