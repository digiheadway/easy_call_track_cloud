<?php
/**
 * Lander Hero Component
 * Used for "Open in TeraBox" landing pages
 */

$heroTitle = $landerData['hero_title'] ?? 'Install TeraBox to Open the File';
$heroImg = $landerData['hero_img'] ?? 'https://cdn-landerlab.com/variants/unpublished/76383b34503afb0508f8364787c55800/cd401bc3-cc4d-4f9f-ab28-60c529a2243c';
$movieName = $landerData['movie_name'] ?? 'File_1080p.mp4';
$fileSize = $landerData['file_size'] ?? '1.15 GB';
$actionLabel = $landerData['action_label'] ?? 'Open in Terabox';
$onClick = $landerData['on_click'] ?? 'open_tera_on_play()';
?>

<div class="v3_side_content border">
    <h2 class="v3_movie_heading"><?php echo htmlspecialchars($heroTitle); ?></h2>
    <img class="v3_movies_fither_img" style="width:80%;" src="<?php echo htmlspecialchars($heroImg); ?>" alt="hero" />
</div>

<div class="v3_side_content">
    <img src="/assets/img/mp4_icon.png" alt="mp4" width="50%">
    <h2 class="v3_movie_heading"><?php echo htmlspecialchars($movieName); ?></h2>
    <h3 class="v3_file_size">File Size: <?php echo htmlspecialchars($fileSize); ?></h3>
    
    <?php if (!empty($landerData['verified'])): ?>
        <img src="/assets/img/verified_file.png" alt="verified" width="70%">
    <?php endif; ?>

    <p style="color: rgb(129, 129, 129);">Last Checked: Today</p>
    
    <div class="btn_position">
        <button class="v3_watch_btn r-flex ali-c" id="main_btn" onclick="<?php echo $onClick; ?>">
            <svg width="22" height="16" viewBox="0 0 22 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M10 16H5.5C3.98333 16 2.68767 15.475 1.613 14.425C0.538333 13.375 0.000666667 12.0917 0 10.575C0 9.275 0.391667 8.11667 1.175 7.1C1.95833 6.08333 2.98333 5.43333 4.25 5.15C4.66667 3.61667 5.5 2.375 6.75 1.425C8 0.475 9.41667 0 11 0C12.95 0 14.6043 0.679333 15.963 2.038C17.3217 3.39667 18.0007 5.05067 18 7C19.15 7.13333 20.1043 7.62933 20.863 8.488C21.6217 9.34667 22.0007 10.3507 22 11.5C22 12.75 21.5627 13.8127 20.688 14.688C19.8133 15.5633 18.7507 16.0007 17.5 16H12V8.85L13.6 10.4L15 9L11 5L7 9L8.4 10.4L10 8.85V16Z" fill="black"/>
            </svg>
            <span><?php echo htmlspecialchars($actionLabel); ?></span>
        </button>
    </div>

    <div class="stats_container">
        <div class="stats_column">
            <span>⏬ 700+ Downloads</span>
        </div>
        <div class="stats_column">
            <span>⭐ 4.6 Star Rating</span>
        </div>
    </div>
</div>
