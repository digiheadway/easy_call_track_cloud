<?php
/**
 * Unified Watch Page Component
 * Renders the movie details, player placeholder, and side info.
 */

// Expecting $movieData to be populated
$title = htmlspecialchars($movieData['title'] ?? 'Movie');
$image = htmlspecialchars($movieData['image'] ?? '/assets/img/not-found.jpg');
$description = htmlspecialchars($movieData['description'] ?? '');
$stats = $movieData['stats'] ?? [];
$fileName = $movieData['file_name'] ?? 'Movie.mp4';
?>

<div class="v3_bradcamp_box r-flex ali-c">
    <a href="/" class="v3_home_brad_camp r-flex ali-c">
        <span><?php echo $subdomaintitle; ?></span>
        <svg width="8" height="15" viewBox="0 0 11 19" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M0.375 18.6L7.7 9.5L0.35 2.15" fill="black" stroke="black"/>
        </svg>
    </a>
    <span class="v3_movie_name_bradcamp"><?php echo $fileName; ?></span>
</div>

<div class="v3_movies_container r-flex ali-c">
    <div class="v3_movies_image_container">
        <div onclick="pop23_myFunction();" class="cur">
            <img id="loader_img" src="<?php echo $image; ?>" alt="<?php echo $title; ?>" class="v3_movies_fither_img">
            <img src="/assets/img/movies_play_icon2.svg?v3" alt="play" class="v3_movies_play_icon" style="display: block;">
        </div>
        <div>
            <p class="not_this_text cur" id="not_this_text" onclick="record('not_this','123movies');">Not This?</p>
            <form action="/msearch.php" class="hero_search_form" id="not_this_searchbox" style="display:none;">
                <input type="search" name="q" placeholder="Search Again" value="<?php echo $title; ?>">
                <button type="submit" class="hero_search_btn">Search</button>
            </form>
        </div>
    </div>

    <div class="v3_side_content_box">
        <h2 class="v3_movie_heading">Watch or Download <?php echo $title; ?> at Full HD</h2>
        <div class="v3_last_update">
            <b>Last Updated:</b> <span>1 Day Ago</span>
        </div>

        <a onclick="shareCurrentUrl()" class="v3_movie_share_btn r-flex ali-c cur">
            <span style="margin-right:8px;">📤</span> <span>Share With Friends</span>
        </a>

        <p class="v3_side_content">
            <?php echo $description ?: "Get ready for an incredible experience with {$title}."; ?>
        </p>

        <?php foreach ($stats as $label => $value): ?>
            <div class="v3_file_size">
                <b><?php echo $label; ?>: </b> <span><?php echo htmlspecialchars($value); ?></span>
            </div>
        <?php endforeach; ?>
        
        <div class="v3_file_size">
            <b>File Name: </b> <span><?php echo $fileName; ?></span>
        </div>

        <div class="v3_paly_and_downlod_btn r-flex ali-c">
            <button class="v3_watch_btn r-flex ali-c" onclick="download_media('watch');">
                <span>▶ Watch Online Full Hd</span>
            </button>
            <button class="v3_watch_btn r-flex ali-c" onclick="download_media('download');">
                <span>📥 Download Now</span>
            </button>
        </div>
    </div>
</div>

<div class="overlay455_overlay" id="overlay455_overlay">
    <div class="overlay455_loader"></div>
</div>
