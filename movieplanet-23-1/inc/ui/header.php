<?php
/**
 * Header Component
 * Contains the logo, navigation menu, and search bar
 */
?>
<header class="iamontop">
    <div class="container">
        <nav class="navbar r-flex ali-c jut-sb iamontop">
            <a href="/" class="header_logo r-flex ali-c">
                <div class="logo_text r-flex ali-c ">
                    <?php include __DIR__ . '/logo.php'; ?>
                    <span class="logo_name"><?php echo $subdomaintitle; ?></span>
                </div>        
                <span class="logo_help_text">New Look</span>
            </a>
            
            <input type="checkbox" name="hambergar" id="hambergar" class="hambergar">
            <label for="hambergar" class="hambergar_label">
                <svg width="28" height="28" viewBox="0 0 21 21" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M4.5 6.5H16.5M4.498 10.5H16.495M4.5 14.5H16.495" stroke="#000000" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>                        
            </label>
            
            <ul class="header_menu_bar r-flex ali-c jut-sb iamontop">
                <li class="nav_menu_list"><a href="/">Home</a></li>
                <li class="nav_menu_list"><a href="/pages/contact-us.php" target="_blank">Remove Your Content</a></li>
                <li class="nav_menu_list"><a href="https://t.me/pokipros" target="_blank">Join Telegram</a></li>
            </ul>
            
            <div class="header_search iamontop r-flex ali-c">
                <form action="/msearch.php" class="r-flex ali-c">
                    <label for="header_search_box">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M15.5 14H14.71L14.43 13.73C15.4439 12.554 16.0011 11.0527 16 9.5C16 8.21442 15.6188 6.95772 14.9046 5.8888C14.1903 4.81988 13.1752 3.98676 11.9874 3.49479C10.7997 3.00282 9.49279 2.87409 8.23192 3.1249C6.97104 3.3757 5.81285 3.99477 4.90381 4.90381C3.99477 5.81285 3.3757 6.97104 3.1249 8.23192C2.87409 9.49279 3.00282 10.7997 3.49479 11.9874C3.98676 13.1752 4.81988 14.1903 5.8888 14.9046C6.95772 15.6188 8.21442 16 9.5 16C11.11 16 12.59 15.41 13.73 14.43L14 14.71V15.5L19 20.49L20.49 19L15.5 14ZM9.5 14C7.01 14 5 11.99 5 9.5C5 7.01 7.01 5 9.5 5C11.99 5 14 7.01 14 9.5C14 11.99 11.99 14 9.5 14Z" fill="#5E5E5E"/>
                        </svg>                            
                    </label>
                    <input type="search" id="header_search_box" placeholder="Search here" name="q" class="i_am_search_btn">
                    <button type="submit" class="header_search_btn">Search Now</button>
                </form>
                <?php 
                require_once __DIR__ . '/ads.php';
                renderAd('header'); 
                ?>
            </div>
        </nav>
    </div>
</header>

<div id="dark_search_modal">
    <div class="dark_search_modal_header">
        <input type="text" id="dark_search_input" placeholder="Type to search..." autocomplete="off">
        <span id="dark_modal_close_btn" style="cursor:pointer; font-size:24px;">✕</span>
    </div>
    <div id="dark_search_modal_status">Search Anything</div>
    <div id="dark_search_results"></div>
</div>
