<!DOCTYPE html>
<html lang="en">
<head>
    <?php include 'inc/head-global.php'; ?>
   
    <title><?php echo $subdomaintitle; ?> <?php echo $this_month; ?> | Watch Or Download Latest Movies/Webseries </title>
    <meta name="description" content="(<?php echo $this_month; ?>) Download or Watch Movies and Webseries on <?php echo $subdomain; ?> | Latest Hollywood, English, Bollywood Hindi English Telugu Tamil Malayalam Dubbed Kannada Marathi Punjabi movies" />
    <meta name="robots" content="index, follow" />
   
</head>
<body>
<?php include 'inc/header.php'; ?>
    <main>
        <section class="hero_section">
            <div class="container">
                <div class="hero_container">
                    <div class="hero_logo c-flex ali jut-c">
                        <div class="hero_logo_box r-flex ali-c">
                        <svg width="38" height="38" viewBox="0 0 38 38" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M27.9489 31.0526H31.0526V34.1579H18.6315C10.0564 34.1579 3.10522 27.2067 3.10522 18.6315C3.10522 10.0564 10.0564 3.10522 18.6315 3.10522C27.2067 3.10522 34.1579 10.0564 34.1579 18.6315C34.1598 21.0421 33.5997 23.4199 32.5219 25.5761C31.444 27.7323 29.8782 29.6075 27.9489 31.0526ZM18.6315 15.5263C19.4551 15.5263 20.2449 15.1991 20.8273 14.6168C21.4096 14.0344 21.7368 13.2446 21.7368 12.421C21.7368 11.5974 21.4096 10.8076 20.8273 10.2253C20.2449 9.64291 19.4551 9.31575 18.6315 9.31575C17.808 9.31575 17.0181 9.64291 16.4358 10.2253C15.8534 10.8076 15.5263 11.5974 15.5263 12.421C15.5263 13.2446 15.8534 14.0344 16.4358 14.6168C17.0181 15.1991 17.808 15.5263 18.6315 15.5263ZM12.421 21.7368C13.2446 21.7368 14.0344 21.4096 14.6168 20.8273C15.1991 20.2449 15.5263 19.4551 15.5263 18.6315C15.5263 17.808 15.1991 17.0181 14.6168 16.4358C14.0344 15.8534 13.2446 15.5263 12.421 15.5263C11.5974 15.5263 10.8076 15.8534 10.2253 16.4358C9.64291 17.0181 9.31575 17.808 9.31575 18.6315C9.31575 19.4551 9.64291 20.2449 10.2253 20.8273C10.8076 21.4096 11.5974 21.7368 12.421 21.7368ZM24.8421 21.7368C25.6656 21.7368 26.4555 21.4096 27.0378 20.8273C27.6202 20.2449 27.9473 19.4551 27.9473 18.6315C27.9473 17.808 27.6202 17.0181 27.0378 16.4358C26.4555 15.8534 25.6656 15.5263 24.8421 15.5263C24.0185 15.5263 23.2287 15.8534 22.6463 16.4358C22.064 17.0181 21.7368 17.808 21.7368 18.6315C21.7368 19.4551 22.064 20.2449 22.6463 20.8273C23.2287 21.4096 24.0185 21.7368 24.8421 21.7368ZM18.6315 27.9473C19.4551 27.9473 20.2449 27.6202 20.8273 27.0378C21.4096 26.4555 21.7368 25.6656 21.7368 24.8421C21.7368 24.0185 21.4096 23.2287 20.8273 22.6463C20.2449 22.064 19.4551 21.7368 18.6315 21.7368C17.808 21.7368 17.0181 22.064 16.4358 22.6463C15.8534 23.2287 15.5263 24.0185 15.5263 24.8421C15.5263 25.6656 15.8534 26.4555 16.4358 27.0378C17.0181 27.6202 17.808 27.9473 18.6315 27.9473Z" fill="#3A6B5E"/>
</svg>
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
                        <?php include('inc/featured_images.php') ?>
                    </div>
                </div>

            </div>
            <h3 class="latest_addoons_ending_text">+957 Other Added Recently</h3>
        </section>

        <section class="contant-section">
            <div class="container">
                <div class="contant_container">
                <?php if ($fulldomain === 'harleywives.com'): ?>
                                                        <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-3167708670200886"
                                         crossorigin="anonymous"></script>
                                    <ins class="adsbygoogle"
                                         style="display:block"
                                         data-ad-format="autorelaxed"
                                         data-ad-client="ca-pub-3167708670200886"
                                         data-ad-slot="6921721636"></ins>
                                    <script>
                                         (adsbygoogle = window.adsbygoogle || []).push({});
                                    </script>
<?php endif; ?>        
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
                    include 'inc/other-websites.php';
                    ?>
    <!-- <script type="application/ld+json">{"@context":"https:\/\/schema.org","@type":"Article","name":"<?php echo $subdomaintitle; ?>","url":"https:\/\/<?php echo $subdomaintitle . "." . $fulldomain; ?>\/","sameAs":"<?php echo $subdomaintitle . "." . $fulldomain; ?>","mainEntity":"http:\/\/<?php echo $subdomaintitle . "." . $fulldomain; ?>","author":{"@type":"Organization","name":"Contributors to Wikimedia projects"},"publisher":{"@type":"Organization","name":"Wikimedia Foundation, Inc.","logo":{"@type":"ImageObject","url":"https:\/\/www.wikimedia.org\/static\/images\/wmf-hor-googpub.png"}},"datePublished":"2023-01-19T17:30:06Z","dateModified":"2023-04-28T12:36:57Z","headline":"<?php echo $subdomaintitle; ?>"}</script> -->

<!-- Website Search Schema  -->
    <script type="application/ld+json">
{
  "@context": "https://schema.org/",
  "@type": "WebSite",
  "name": "<?php echo $subdomaintitle; ?>",
  "url": "https://<?php echo $subdomainexactly . "." . $fulldomain; ?>/",
  "potentialAction": {
    "@type": "SearchAction",
    "target": "https://<?php echo $subdomainexactly . "." . $fulldomain; ?>/get-free-home-loan-quotation.php/?q={search_term_string}",
    "query-input": "required name=search_term_string"
  }
}
</script>

<!-- Organisation Schema -->
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "NewsMediaOrganization",
  "name": "<?php echo $subdomaintitle; ?>",
  "alternateName": "<?php echo $subdomainexactly; ?>",
  "url": "http://<?php echo $subdomainexactly . "." . $fulldomain; ?>/",
  "logo": "",
  "sameAs": "https://www.facebook.com/wikipedia/"
}
</script>

<!-- article schema  -->
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "Article",
  "mainEntityOfPage": {
    "@type": "WebPage",
    "@id": "https://<?php echo $subdomainexactly . "." . $fulldomain; ?>/"
  },
  "headline": "<?php echo $subdomaintitle; ?> - Watch Free Movies",
  "description": "The article is about how <?php echo $subdomaintitle; ?> looks and other info about <?php echo $subdomaintitle; ?>",
  "image": "",  
  "author": {
    "@type": "Organization",
    "name": "Contributors to Wikimedia projects"
  },  
  "publisher": {
    "@type": "Organization",
    "name": "<?php echo $subdomaintitle; ?>",
    "logo": {
      "@type": "ImageObject",
      "url": ""
    }
  },
  "datePublished": "2023-05-03"
}
</script>
<script>
    function unique_visitor() {
    // Check if the event has been triggered previously
    if (!localStorage.getItem('unique_visitor')) {
        // Your GA4 event tracking code here
        gtag('event', 'unique_visitor', {
            'event_category': 'view',
            'event_label': 'Page Viewed',
            // Additional event parameters if needed
        });

        // Set a flag in localStorage to indicate that the event has been triggered
        localStorage.setItem('unique_visitor', 'true');
    } else {
        console.log('GA4 event unique_visitor has already been triggered for this user.');
    }
}

// Call the triggerGA4Event function when the search page is viewed
unique_visitor();
</script>
                    <?php
                    include 'inc/footer.php';
                    ?>