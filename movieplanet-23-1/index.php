<!DOCTYPE html>
<html lang="en">
<head>
    <?php include 'inc/ui/head-global.php'; ?>
   
    <title><?php echo $subdomaintitle; ?> <?php echo $this_month; ?> | Watch Or Download Latest Movies/Webseries </title>
    <meta name="description" content="(<?php echo $this_month; ?>) Download or Watch Movies and Webseries on <?php echo $subdomain; ?> | Latest Hollywood, English, Bollywood Hindi English Telugu Tamil Malayalam Dubbed Kannada Marathi Punjabi movies" />
    <meta name="robots" content="index, follow" />
   
</head>
<body>
<?php include 'inc/ui/header.php'; ?>
    <main>
        <section class="hero_section">
            <div class="container">
                <div class="hero_container">
                    <div class="hero_logo c-flex ali jut-c">
                        <div class="hero_logo_box r-flex ali-c">
                            <?php include 'inc/ui/logo.php'; ?>
                            <span class="logo_name"><?php echo $subdomaintitle; ?></span>
                        </div> 
                        <span class="logo_help_text hero_logo_HT">Download Movies & WebSeries</span>                         
                    </div>
                    <form action="<?php echo $func_search_url; ?>" class="hero_search_form" onsubmit="record('searched_from_home','<?php echo $subdomaintitle; ?>');" >
                        <input type="search" name="q" class="i_am_search_btn" id="hero_search" placeholder="Search Any Movie/Series/Show ">
                        <button type="submit" class="hero_search_btn r-flex ali-c jut-c">
                            <svg width="24" height="24" view9Box="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M15.5 14H14.71L14.43 13.73C15.4439 12.554 16.0011 11.0527 16 9.5C16 8.21442 15.6188 6.95772 14.9046 5.8888C14.1903 4.81988 13.1752 3.98676 11.9874 3.49479C10.7997 3.00282 9.49279 2.87409 8.23192 3.1249C6.97104 3.3757 5.81285 3.99477 4.90381 4.90381C3.99477 5.81285 3.3757 6.97104 3.1249 8.23192C2.87409 9.49279 3.00282 10.7997 3.49479 11.9874C3.98676 13.1752 4.81988 14.1903 5.8888 14.9046C6.95772 15.6188 8.21442 16 9.5 16C11.11 16 12.59 15.41 13.73 14.43L14 14.71V15.5L19 20.49L20.49 19L15.5 14ZM9.5 14C7.01 14 5 11.99 5 9.5C5 7.01 7.01 5 9.5 5C11.99 5 14 7.01 14 9.5C14 11.99 11.99 14 9.5 14Z" fill="white"/>
                            </svg>                                
                            <span>Search Now</span>
                        </button>
                    </form>

<!--                     <div class="hero_main_heading"><a target="_blank" href="https://shortio.helpsarkari.com/times-prime-landing-page?text=I%20Want%20Times%20Prime%20With%20Coupon%20Code=Discountedat249"><img src="/assets/img/times-prime/ad-times-prime-cta-colored.png" width="100%" style="max-width: 600px; margin: 5px auto;"   /></a></div>
                </div> -->
            </div>
        </section>

        <section class="latest_movies_section">
            <div class="container">
                <div class="latest_movies_conatiner">
                    <div class="latest_main_heading r-flex ali">
                        <span>Latest Addons</span>
                        <svg width="34" height="34" viewBox="0 0 34 34" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M31.6094 7.4375V15.9375C31.6094 16.1488 31.5255 16.3515 31.376 16.501C31.2266 16.6504 31.0239 16.7344 30.8125 16.7344C30.6012 16.7344 30.3985 16.6504 30.2491 16.501C30.0996 16.3515 30.0157 16.1488 30.0157 15.9375V9.36328L18.6204 20.7453C18.5484 20.8209 18.4619 20.881 18.366 20.9221C18.2701 20.9632 18.1669 20.9844 18.0625 20.9844C17.9582 20.9844 17.855 20.9632 17.7591 20.9221C17.6632 20.881 17.5767 20.8209 17.5047 20.7453L12.75 16.0039L3.74536 24.9953C3.67211 25.0686 3.58514 25.1267 3.48943 25.1663C3.39372 25.206 3.29114 25.2264 3.18755 25.2264C3.08395 25.2264 2.98137 25.206 2.88566 25.1663C2.78995 25.1267 2.70299 25.0686 2.62974 24.9953C2.55648 24.9221 2.49837 24.8351 2.45873 24.7394C2.41909 24.6437 2.39868 24.5411 2.39868 24.4375C2.39868 24.3339 2.41909 24.2313 2.45873 24.1356C2.49837 24.0399 2.55648 23.9529 2.62974 23.8797L12.1922 14.3172C12.2642 14.2416 12.3507 14.1815 12.4466 14.1404C12.5425 14.0993 12.6457 14.0781 12.75 14.0781C12.8544 14.0781 12.9576 14.0993 13.0535 14.1404C13.1494 14.1815 13.2359 14.2416 13.3079 14.3172L18.0625 19.0586L28.8868 8.23437H22.3125C22.1012 8.23437 21.8985 8.15042 21.7491 8.00098C21.5996 7.85153 21.5157 7.64884 21.5157 7.4375C21.5157 7.22616 21.5996 7.02347 21.7491 6.87402C21.8985 6.72458 22.1012 6.64063 22.3125 6.64062H30.8125C31.0239 6.64062 31.2266 6.72458 31.376 6.87402C31.5255 7.02347 31.6094 7.22616 31.6094 7.4375Z" fill="#3A6B5E"/>
                        </svg>
                    </div>
                    <div class="latest_movies_bx r-flex ali-c jut-sb">
                        <?php include('inc/ui/featured_images.php') ?>
                    </div>
                </div>

            </div>
            <h3 class="latest_addoons_ending_text">+<?php echo dbGetCount('images') + 500; ?> Other Added Recently</h3>
        </section>

        <section class="contant-section">
            <div class="container">
                <div class="contant_container">
                    <?php renderAd('content'); ?>
<?php
$folderPath = "content/" . $domain . "/";  // Check for folder corresponding to domain
$contentfile = $folderPath . $subdomainexactly . ".html";  // The specific subdomain file
$defaultFolder = "content/default/";  // Default folder if the domain folder doesn't exist
$defaultfile = $defaultFolder . "default.php";  // Default fallback PHP file

// Check if the domain folder exists, if not fall back to the default folder
if (!is_dir($folderPath)) {
    $folderPath = $defaultFolder;  // Use default folder
    $contentfile = $folderPath . $subdomainexactly . ".html";  // Set the file to be checked in the default folder
}

// Now check if the content file exists in the selected folder
if (file_exists($contentfile)) {
    include $contentfile;  // Include the subdomain file
} else {
    // If the file doesn't exist, include the default PHP file
    include $defaultfile;
}
?>

                    
                    <?php
                    include 'inc/ui/other-websites.php';
                    include 'inc/core/seo.php';
                    renderSchemas([
                        'subdomain' => $subdomainexactly,
                        'subdomaintitle' => $subdomaintitle,
                        'fulldomain' => $fulldomain
                    ]);
                    ?>

                    <script>
                        function unique_visitor() {
                            if (!localStorage.getItem('unique_visitor')) {
                                gtag('event', 'unique_visitor', {
                                    'event_category': 'view',
                                    'event_label': 'Page Viewed'
                                });
                                localStorage.setItem('unique_visitor', 'true');
                            }
                        }
                        unique_visitor();
                    </script>
                    
                    <?php include 'inc/ui/footer.php'; ?>
                </div>
            </div>
        </section>
    </main>
</body>
</html>
