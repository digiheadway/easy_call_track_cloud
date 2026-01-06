<?php
/**
 * Reusable Download/Watch Popup Modal Component
 * 
 * Expected variables:
 * - $content_title: The title of the content (movie/series name)
 * - $browserTeraLink: Link for browser viewing
 * - $terawatch: Mirror link (optional)
 * - $is_android_device: Boolean for device detection
 */

$content_title = $content_title ?? $searchquery ?? 'Content';
$show_mirror_link = isset($terawatch) && !empty($terawatch);
?>
<div class="pop23_container show" id="pop23_myPopup">
    <div class="popup-overlay" onclick="pop23_myFunction()"></div>
    <div class="righ-popup pop23_r-flex ali-c jut-c">
        <div class="pop-clos-icon" onclick="pop23_myFunction()">
            <svg width="50" height="50" viewBox="0 0 30 30" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path fill-rule="evenodd" clip-rule="evenodd" d="M23.564 8.06405C23.6709 7.95728 23.7557 7.83051 23.8136 7.69096C23.8715 7.55142 23.9014 7.40183 23.9014 7.25075C23.9015 7.09967 23.8719 6.95005 23.8141 6.81044C23.7564 6.67082 23.6718 6.54394 23.565 6.43705C23.4582 6.33015 23.3314 6.24533 23.1919 6.18743C23.0524 6.12953 22.9028 6.09968 22.7517 6.09958C22.6006 6.09949 22.451 6.12916 22.3114 6.18689C22.1718 6.24462 22.0449 6.32928 21.938 6.43605L15 13.374L8.06399 6.43605C7.8481 6.22016 7.55529 6.09888 7.24999 6.09888C6.94468 6.09888 6.65187 6.22016 6.43599 6.43605C6.2201 6.65193 6.09882 6.94474 6.09882 7.25005C6.09882 7.55536 6.2201 7.84816 6.43599 8.06405L13.374 15L6.43599 21.936C6.32909 22.0429 6.2443 22.1698 6.18644 22.3095C6.12859 22.4492 6.09882 22.5989 6.09882 22.75C6.09882 22.9012 6.12859 23.0509 6.18644 23.1906C6.2443 23.3302 6.32909 23.4571 6.43599 23.564C6.65187 23.7799 6.94468 23.9012 7.24999 23.9012C7.40116 23.9012 7.55085 23.8714 7.69052 23.8136C7.83019 23.7557 7.95709 23.6709 8.06399 23.564L15 16.626L21.938 23.564C22.1539 23.7797 22.4466 23.9007 22.7517 23.9005C23.0568 23.9003 23.3494 23.7789 23.565 23.563C23.7806 23.3472 23.9016 23.0545 23.9014 22.7493C23.9013 22.4442 23.7799 22.1517 23.564 21.936L16.626 15L23.564 8.06405Z" fill="#585352"/>
            </svg>
        </div>
        <div class="pop-form-box c-flex">
            <div class="form-heading">Watch or Download <span class="data_movie_title"><?php echo htmlspecialchars($content_title); ?></span> Via..</div>
            <span id="ctr_booster_1" style="text-align: center;font-weight:400;padding: 10px;border: 1px black dotted;margin: 10px auto;font-size:12px;display:none;"></span>
            
            <!-- TeraBox App Link -->
            <div class="tab pop23_r-flex ali-c jut-sb" id="popup_tab_app">
                <div class="left-side-icon pop23_r-flex ali-c">
                    <img src="https://play-lh.googleusercontent.com/Tv3h9IHUliBayyGRxcmzOICwPGfbB8M-rnHDpzMlGM5YPS_-LytZO6GccsVPszse2Zqr" alt="TeraBox" id="icon_for_first_link"/>
                    <span>TeraBox Drive App</span>
                </div>
                <a target="_blank" class="reddit_btn cur" id="tera_drive_link" rel="noopener noreferrer" onclick="open_tera_link();gtag('event', 'play_on_tera', {'event_category': 'converts', 'event_label': 'visited_tera', 'value': 0.8, 'currency': 'INR'});down_tried('tera_drive');">Open</a>
            </div>
            
            <!-- Browser Link -->
            <div class="tab pop23_r-flex ali-c jut-sb" id="popup_tab_shortener">
                <div class="left-side-icon pop23_r-flex ali-c">
                    <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/e/e1/Google_Chrome_icon_%28February_2022%29.svg/1024px-Google_Chrome_icon_%28February_2022%29.svg.png" alt="Browser" id="icon_for_second_link">
                    <span>View in Browser</span>
                </div>
                <?php
                $final_browser_link = $browserTeraLink ?? $browserTeraLink1 ?? '#';
                if (isset($is_android_device) && $is_android_device && isset($browserTeraLink1)) {
                    $tera_params = [
                        'name' => $content_title,
                        'size' => '1.3 GB',
                        'by' => 'Pokipro',
                        'type' => 'folder',
                        'downloads' => '10K+',
                        'rating' => '4.7★',
                        'views' => '234',
                        'time' => '45',
                        'badge' => 'Hot',
                        'landing' => '4'
                    ];
                    $final_browser_link = $browserTeraLink1 . '?' . http_build_query($tera_params, '', '&', PHP_QUERY_RFC3986);
                }
                ?>
                <a href="<?= htmlspecialchars($final_browser_link) ?>" target="_blank" id="url_shorten_link" rel="noopener noreferrer" class="chrom_btn" onclick="gtag('event', 'play_via_url', {'event_category': 'converts', 'event_label': 'visited_tera', 'value': 0.4, 'currency': 'INR'});down_tried('link_shortener');">View</a>
            </div>
            
            <?php if ($show_mirror_link): ?>
            <!-- Mirror Link -->
            <div class="tab pop23_r-flex ali-c jut-sb" id="popup_tab_shortener3">
                <div class="left-side-icon pop23_r-flex ali-c">
                    <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/e/e1/Google_Chrome_icon_%28February_2022%29.svg/1024px-Google_Chrome_icon_%28February_2022%29.svg.png" alt="Mirror">
                    <span>Mirror Link 3</span>
                </div>
                <a href="<?= htmlspecialchars($terawatch) ?>?title=<?= urlencode($content_title) ?>+Full+Hd" target="_blank" rel="noopener noreferrer" class="chrom_btn" onclick="gtag('event', 'play_via_url', {'event_category': 'converts', 'event_label': 'visited_tera', 'value': 0.4, 'currency': 'INR'});down_tried('link_shortener');">View</a>
            </div>
            <?php endif; ?>
            
            <!-- Working Links Badge -->
            <div class="working-links-badge">
                <svg width="20px" height="20px" viewBox="0 0 24 24" fill="#ffffff" xmlns="http://www.w3.org/2000/svg">
                    <path d="M18.25,3 C19.7687831,3 21,4.23121694 21,5.75 L21,18.25 C21,19.7687831 19.7687831,21 18.25,21 L5.75,21 C4.23121694,21 3,19.7687831 3,18.25 L3,5.75 C3,4.23121694 4.23121694,3 5.75,3 L18.25,3 Z M18.25,4.5 L5.75,4.5 C5.05964406,4.5 4.5,5.05964406 4.5,5.75 L4.5,18.25 C4.5,18.9403559 5.05964406,19.5 5.75,19.5 L18.25,19.5 C18.9403559,19.5 19.5,18.9403559 19.5,18.25 L19.5,5.75 C19.5,5.05964406 18.9403559,4.5 18.25,4.5 Z M10,14.4393398 L16.4696699,7.96966991 C16.7625631,7.6767767 17.2374369,7.6767767 17.5303301,7.96966991 C17.7965966,8.23593648 17.8208027,8.65260016 17.6029482,8.94621165 L17.5303301,9.03033009 L10.5303301,16.0303301 C10.2640635,16.2965966 9.84739984,16.3208027 9.55378835,16.1029482 L9.46966991,16.0303301 L6.46966991,13.0303301 C6.1767767,12.7374369 6.1767767,12.2625631 6.46966991,11.9696699 C6.73593648,11.7034034 7.15260016,11.6791973 7.44621165,11.8970518 L7.53033009,11.9696699 L10,14.4393398 Z"/>
                </svg>
                100% Working Links
            </div>
        </div>
    </div>
</div>
