<!DOCTYPE html>
    <html lang="en">

    <head>
      <?php include 'inc/head-global.php';
      $content_name = $_GET['name'];
      $content_img_url = $_GET['img'];
      ?>
      <title>
      Download or Watch <?php echo $content_name; ?> in Full HD
      </title>

      <meta name="robots" content="index, nofollow" />
       <script>
      function pop23_myFunction() {
        check_if_already_downloaded();
        var pop23_popup = document.getElementById("pop23_myPopup");
        if (pop23_popup.classList.contains("show")) {
          pop23_popup.classList.add("hide");

          // Wait for the animation to complete and then remove the 'show' and 'hide' classes
          setTimeout(() => {
            pop23_popup.classList.remove("show");
            pop23_popup.classList.remove("hide");
          //  document.body.classList.remove("noscroll");
          }, 300); // 300ms is the duration of the fade-out animation
        } else {
          pop23_popup.classList.add("show");
          // document.body.classList.add("noscroll");
          gtag('event', 'watch_play_dialog');
          
          gtag("event", "view_cart", {
  currency: "INR",
  items: [
    {
      item_id: "M_" + query,
      item_name: query,
      item_brand: urlParams.get('utm_source'),
      affiliation: urlParams.get('dm'),
      promotion_name: urlParams.get('utm_medium'),
      price: 1,
      quantity: 1
    }
  ]
});
        }
      }
if (urlparam('q').includes('https') || urlparam('q').includes('teralink') || urlparam('q').includes('surl')) {
  // Google Analytics 4 event tracking
  gtag('event', 'searched invalid query', {
    'event_category': 'Search',
    'event_label': 'Invalid Query',
    'value': urlparam('q') // Logs the invalid search query for analytics
  });

  // Alert for unsupported search query
  alert("Search Query not supported, please try searching for a Movie/Web Series Name");
  
  // Redirect to homepage
  window.location.href = "/";
}

// Function to get URL parameters
function urlparam(name) {
  let urlParams = new URLSearchParams(window.location.search);
  return urlParams.get(name)?.toLowerCase() || '';  // Convert to lowercase or return an empty string if null
}

    </script>
<?php include 'inc/tera_link_gen.php'; ?>
    </head>

    <body>
    <div class="pop23_container show" id="pop23_myPopup">
      <div class="popup-overlay" onclick="pop23_myFunction()"></div>

      <div class="righ-popup pop23_r-flex ali-c jut-c">
        <div class="pop-clos-icon" onclick="pop23_myFunction()">
          <svg
            width="50"
            height="50"
            viewBox="0 0 30 30"
            fill="none"
            xmlns="http://www.w3.org/2000/svg">
            <path
              fill-rule="evenodd"
              clip-rule="evenodd"
              d="M23.564 8.06405C23.6709 7.95728 23.7557 7.83051 23.8136 7.69096C23.8715 7.55142 23.9014 7.40183 23.9014 7.25075C23.9015 7.09967 23.8719 6.95005 23.8141 6.81044C23.7564 6.67082 23.6718 6.54394 23.565 6.43705C23.4582 6.33015 23.3314 6.24533 23.1919 6.18743C23.0524 6.12953 22.9028 6.09968 22.7517 6.09958C22.6006 6.09949 22.451 6.12916 22.3114 6.18689C22.1718 6.24462 22.0449 6.32928 21.938 6.43605L15 13.374L8.06399 6.43605C7.8481 6.22016 7.55529 6.09888 7.24999 6.09888C6.94468 6.09888 6.65187 6.22016 6.43599 6.43605C6.2201 6.65193 6.09882 6.94474 6.09882 7.25005C6.09882 7.55536 6.2201 7.84816 6.43599 8.06405L13.374 15L6.43599 21.936C6.32909 22.0429 6.2443 22.1698 6.18644 22.3095C6.12859 22.4492 6.09882 22.5989 6.09882 22.75C6.09882 22.9012 6.12859 23.0509 6.18644 23.1906C6.2443 23.3302 6.32909 23.4571 6.43599 23.564C6.65187 23.7799 6.94468 23.9012 7.24999 23.9012C7.40116 23.9012 7.55085 23.8714 7.69052 23.8136C7.83019 23.7557 7.95709 23.6709 8.06399 23.564L15 16.626L21.938 23.564C22.1539 23.7797 22.4466 23.9007 22.7517 23.9005C23.0568 23.9003 23.3494 23.7789 23.565 23.563C23.7806 23.3472 23.9016 23.0545 23.9014 22.7493C23.9013 22.4442 23.7799 22.1517 23.564 21.936L16.626 15L23.564 8.06405Z"
              fill="#585352" />
          </svg>
        </div>

        <div action="#" class="pop-form-box c-flex">
          <div class="form-heading">Watch or Download <span class="data_movie_title"><?php echo $content_name; ?></span> Via..</div>
<span id="ctr_booster_1" style="text-align: center;font-weight:400;padding: 10px;border: 1px black dotted;margin: 10px auto;font-size:12px;display:none;"></span>
          <div class="tab pop23_r-flex ali-c jut-sb" id="popup_tab_app">
            <div class="left-side-icon pop23_r-flex ali-c">
              <img
                src="https://play-lh.googleusercontent.com/Tv3h9IHUliBayyGRxcmzOICwPGfbB8M-rnHDpzMlGM5YPS_-LytZO6GccsVPszse2Zqr"
                alt=""
                srcset="" id='icon_for_first_link' />
              <span>TeraBox Drive App</span>
            </div>
            <a target="_blank" class="reddit_btn cur" id="tera_drive_link" rel=”noopener noreferrer”  onclick="open_tera_on_play();gtag('event', 'play_on_tera', {'event_category': 'converts', 'event_label': 'visited_tera', 'value': 0.8, 'currency': 'INR'});down_tried('tera_drive');">Open</a>
          </div>
          <div class="tab pop23_r-flex ali-c jut-sb" id="popup_tab_shortener">
            <div class="left-side-icon pop23_r-flex ali-c">
              <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/e/e1/Google_Chrome_icon_%28February_2022%29.svg/1024px-Google_Chrome_icon_%28February_2022%29.svg.png" alt="" srcset="" id='icon_for_second_link'>
              <span>View in Browser</span>
            </div>
                 <a href="<?php echo $browserTeraLink; ?> " target="_blank"  id="url_shorten_link"   rel=”noopener noreferrer”  class="chrom_btn"  onclick="gtag('event', 'play_via_url', {'event_category': 'converts', 'event_label': 'visited_tera', 'value': 0.4, 'currency': 'INR'});down_tried('link_shortener');">View</a>

            <!-- <a href="/url_shortner.php?q=<?php echo $content_name; ?>" target="_blank"  id="url_shorten_link"   rel=”noopener noreferrer”  class="chrom_btn"  onclick="gtag('event', 'play_via_url', {'event_category': 'converts', 'event_label': 'visited_tera', 'value': 0.4, 'currency': 'INR'});down_tried('link_shortener');">Open</a> -->
          </div>
          <div style="
    font-size: 12px;
    text-align: center;
    display: flex;
    gap: 5px;
    background: #148005;
    align-items: center;
    margin: 10px 0;
    font-weight: bold;
    color: #ffffff;
    padding: 10px;
    border-radius: 5px;
    justify-content: center;
"><svg width="20px" height="20px" viewBox="0 0 24 24" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" fill="#ffffff" stroke="#ffffff">

<g id="SVGRepo_bgCarrier" stroke-width="0"/>

<g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>

<g id="SVGRepo_iconCarrier"><g id="🔍-Product-Icons" stroke="none" stroke-width="1" fill="none" fill-rule="evenodd"> <g id="ic_fluent_checkbox_checked_24_regular" fill="#ffffff" fill-rule="nonzero"> <path d="M18.25,3 C19.7687831,3 21,4.23121694 21,5.75 L21,18.25 C21,19.7687831 19.7687831,21 18.25,21 L5.75,21 C4.23121694,21 3,19.7687831 3,18.25 L3,5.75 C3,4.23121694 4.23121694,3 5.75,3 L18.25,3 Z M18.25,4.5 L5.75,4.5 C5.05964406,4.5 4.5,5.05964406 4.5,5.75 L4.5,18.25 C4.5,18.9403559 5.05964406,19.5 5.75,19.5 L18.25,19.5 C18.9403559,19.5 19.5,18.9403559 19.5,18.25 L19.5,5.75 C19.5,5.05964406 18.9403559,4.5 18.25,4.5 Z M10,14.4393398 L16.4696699,7.96966991 C16.7625631,7.6767767 17.2374369,7.6767767 17.5303301,7.96966991 C17.7965966,8.23593648 17.8208027,8.65260016 17.6029482,8.94621165 L17.5303301,9.03033009 L10.5303301,16.0303301 C10.2640635,16.2965966 9.84739984,16.3208027 9.55378835,16.1029482 L9.46966991,16.0303301 L6.46966991,13.0303301 C6.1767767,12.7374369 6.1767767,12.2625631 6.46966991,11.9696699 C6.73593648,11.7034034 7.15260016,11.6791973 7.44621165,11.8970518 L7.53033009,11.9696699 L10,14.4393398 L16.4696699,7.96966991 L10,14.4393398 Z" id="🎨Color"> </path> </g> </g> </g>

</svg>100% Working Links</div>
        </div>
      </div>
    </div>
      <?php include 'inc/header.php'; ?>
      <main>
      <section class="v3_movies_watch_section">
        <div class="container">
            <div class="v3_bradcamp_box r-flex ali-c">
                <a href="#" class="v3_home_brad_camp  r-flex ali-c">
                    <span><?php echo $subdomaintitle; ?> </span>
                    <svg width="8" height="15" viewBox="0 0 11 19" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M0.375 18.6C0.125 18.35 0 18.054 0 17.712C0 17.37 0.125 17.0743 0.375 16.825L7.7 9.5L0.35 2.15C0.116667 1.91667 0 1.625 0 1.275C0 0.925 0.125 0.625 0.375 0.375C0.625 0.125 0.921 0 1.263 0C1.605 0 1.90067 0.125 2.15 0.375L10.55 8.8C10.65 8.9 10.721 9.00833 10.763 9.125C10.805 9.24167 10.8257 9.36667 10.825 9.5C10.825 9.63333 10.804 9.75833 10.762 9.875C10.72 9.99167 10.6493 10.1 10.55 10.2L2.125 18.625C1.89167 18.8583 1.604 18.975 1.262 18.975C0.92 18.975 0.624333 18.85 0.375 18.6Z" fill="black"/>
                    </svg>                        
                </a>
                <span class="v3_movie_name_bradcamp" id="data_movie_breadcamp_file_name">Movie57676.mp4 <?php // echo $content_name; ?></span>
            </div>

            <div class="v3_movies_container r-flex ali-c" class="image-container">
                <div class="v3_movies_image_container">
                  <div onclick="pop23_myFunction();" class="cur">  
                    <!-- <img id="loader_img" src="https://images.squarespace-cdn.com/content/v1/569c85bdab281050fe336597/1509422855421-UIGBB87SV4C8QVGPXEG7/ajax-loader.gif?format=500w" alt="Movie" class="v3_movies_fither_img"> -->
                   <img id="loader_img" src="<?php echo $content_img_url; ?>" alt="Movie" class="v3_movies_fither_img">
                    <img src="/assets/img/movies_play_icon2.svg?v3" alt="movies play icon" class="v3_movies_play_icon" style="display: none;">
                  </div>
                    <div>
                      <p class="not_this_text cur" id="not_this_text"  onclick="record('not_this','123movies');">Not This?</p>
                      <form action="<?php echo $func_search_url; ?>" class="hero_search_form" onsubmit="record('searched_from_not_this','123movies');" id="not_this_searchbox" style="display:none;">
                        <input type="search" name="q" id="hero_search" placeholder="Search Any Movie/Series/Show" class="i_am_search_btn" value="<?php echo $content_name; ?>">
                        <button type="submit" class="hero_search_btn r-flex ali-c jut-c">
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M15.5 14H14.71L14.43 13.73C15.4439 12.554 16.0011 11.0527 16 9.5C16 8.21442 15.6188 6.95772 14.9046 5.8888C14.1903 4.81988 13.1752 3.98676 11.9874 3.49479C10.7997 3.00282 9.49279 2.87409 8.23192 3.1249C6.97104 3.3757 5.81285 3.99477 4.90381 4.90381C3.99477 5.81285 3.3757 6.97104 3.1249 8.23192C2.87409 9.49279 3.00282 10.7997 3.49479 11.9874C3.98676 13.1752 4.81988 14.1903 5.8888 14.9046C6.95772 15.6188 8.21442 16 9.5 16C11.11 16 12.59 15.41 13.73 14.43L14 14.71V15.5L19 20.49L20.49 19L15.5 14ZM9.5 14C7.01 14 5 11.99 5 9.5C5 7.01 7.01 5 9.5 5C11.99 5 14 7.01 14 9.5C14 11.99 11.99 14 9.5 14Z" fill="white"></path>
                            </svg>                                
                            <span>Search Again</span>
                        </button>
                    </form>
                    </div>                
                </div>
                
                <div class="v3_side_content_box">
                    <h2 class="v3_movie_heading">Watch or Download <span class="data_movie_title"><?php echo $content_name; ?></span> at Full HD 1080p | 720p | 480p</h2>
                    <div class="v3_last_update">
                        <b>Last Updated:</b>
                        <span>1 Day Ago</span>
                    </div>

                    <a  onclick="shareCurrentUrl()" class="v3_movie_share_btn r-flex ali-c cur">
                        <svg width="14" height="20" viewBox="0 0 25 27" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M20.8971 19.0437C19.888 19.0437 18.9852 19.4303 18.2947 20.0359L8.82787 14.688C8.89426 14.3917 8.94737 14.0953 8.94737 13.786C8.94737 13.4767 8.89426 13.1803 8.82787 12.8839L18.1885 7.5876C18.9055 8.23192 19.8482 8.63141 20.8971 8.63141C23.1012 8.63141 24.8804 6.90462 24.8804 4.76546C24.8804 2.62631 23.1012 0.899521 20.8971 0.899521C18.6931 0.899521 16.9139 2.62631 16.9139 4.76546C16.9139 5.07474 16.967 5.37113 17.0334 5.66752L7.67273 10.9639C6.95575 10.3195 6.01304 9.92005 4.96412 9.92005C2.76005 9.92005 0.980865 11.6468 0.980865 13.786C0.980865 15.9251 2.76005 17.6519 4.96412 17.6519C6.01304 17.6519 6.95575 17.2525 7.67273 16.6081L17.1263 21.9689C17.0599 22.2395 17.0201 22.523 17.0201 22.8065C17.0201 24.8812 18.7595 26.5694 20.8971 26.5694C23.0348 26.5694 24.7742 24.8812 24.7742 22.8065C24.7742 20.7318 23.0348 19.0437 20.8971 19.0437Z" fill="white"/>
                        </svg>                            
                        <span>Share With Friends</span>
                    </a>
                    <p class="v3_side_content" id="data_movie_description">
                    Get ready for an incredible experience with <?php echo $content_name; ?>, a captivating masterpiece that is perfect for everyone. 
                    <!-- This amazing creation combines thrilling action, intriguing suspense, and heartfelt emotions to deliver a truly memorable viewing experience. With talented actors, stunning visuals, and a compelling story, <?php echo $content_name; ?> has something for everyone to enjoy.  -->
                    Don't miss the chance to download <?php echo $content_name; ?> and immerse yourself in a world of excitement and entertainment that will stay with you long after the credits roll. Get ready to be captivated by the magic of <?php echo $content_name; ?>!
                    </p>
                    
                    <div class="v3_file_size">
                        <b>Print Type: </b>
                        <span> Full HD 1080p </span>
                    </div>
                    <div class="v3_file_size" id="file_name_div">
                        <b id="file_name_label">File Name: </b>
                        <span id="data_file_name">Movie57676.mp4</span>
                    </div>
                    <div class="v3_file_size"  id="file_size_div">
                        <b  id="file_size_label">File Size:</b>
                        <span id="file_size_value">1.4 GB</span>
                    </div>
                     <div class="v3_file_size"  id="file_duration_div">
                        <b  id="file_duration_label">Duration :</b>
                        <span id="file_duration_value">....</span>
                    </div>
                    <div class="v3_file_size">
                        <b>Screenshots:</b>
                        <span>5 Available</span>
                    </div>

                    <div class="v3_paly_and_downlod_btn r-flex ali-c">
                        <button class="v3_watch_btn r-flex ali-c" onclick="download_media('watch');"> 
                            <svg width="20" height="25" viewBox="0 0 39 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M5.87407 47.3317C4.59012 48.1707 3.29076 48.2197 1.976 47.4788C0.661233 46.7379 0.0025679 45.5917 0 44.0402V3.96172C0 2.41279 0.658665 1.26659 1.976 0.523103C3.29333 -0.220382 4.59269 -0.171332 5.87407 0.670252L37.2667 20.7095C38.4222 21.484 39 22.5811 39 24.001C39 25.4208 38.4222 26.518 37.2667 27.2924L5.87407 47.3317Z" fill="white"/>
                            </svg>                                
                            <span>Watch Online Full Hd</span>
                        </button>
                        <button class="v3_watch_btn r-flex ali-c" onclick="download_media('download');">
                            <svg width="30" height="25" viewBox="0 0 63 58" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M48.4615 50.75C48.4615 50.0955 48.2218 49.5291 47.7422 49.0508C47.2626 48.5725 46.6947 48.3333 46.0385 48.3333C45.3822 48.3333 44.8143 48.5725 44.3347 49.0508C43.8552 49.5291 43.6154 50.0955 43.6154 50.75C43.6154 51.4045 43.8552 51.9709 44.3347 52.4492C44.8143 52.9275 45.3822 53.1667 46.0385 53.1667C46.6947 53.1667 47.2626 52.9275 47.7422 52.4492C48.2218 51.9709 48.4615 51.4045 48.4615 50.75ZM58.1538 50.75C58.1538 50.0955 57.9141 49.5291 57.4345 49.0508C56.9549 48.5725 56.387 48.3333 55.7308 48.3333C55.0745 48.3333 54.5066 48.5725 54.027 49.0508C53.5475 49.5291 53.3077 50.0955 53.3077 50.75C53.3077 51.4045 53.5475 51.9709 54.027 52.4492C54.5066 52.9275 55.0745 53.1667 55.7308 53.1667C56.387 53.1667 56.9549 52.9275 57.4345 52.4492C57.9141 51.9709 58.1538 51.4045 58.1538 50.75ZM63 42.2917V54.375C63 55.3819 62.6466 56.2379 61.9399 56.9427C61.2332 57.6476 60.375 58 59.3654 58H3.63462C2.625 58 1.76683 57.6476 1.0601 56.9427C0.353365 56.2379 0 55.3819 0 54.375V42.2917C0 41.2847 0.353365 40.4288 1.0601 39.724C1.76683 39.0191 2.625 38.6667 3.63462 38.6667H21.2398L26.351 43.8021C27.8149 45.2118 29.5312 45.9167 31.5 45.9167C33.4688 45.9167 35.1851 45.2118 36.649 43.8021L41.7981 38.6667H59.3654C60.375 38.6667 61.2332 39.0191 61.9399 39.724C62.6466 40.4288 63 41.2847 63 42.2917ZM50.6953 20.806C51.1244 21.8381 50.9477 22.7192 50.1653 23.4492L33.2037 40.3659C32.7494 40.8442 32.1815 41.0833 31.5 41.0833C30.8185 41.0833 30.2506 40.8442 29.7963 40.3659L12.8347 23.4492C12.0523 22.7192 11.8756 21.8381 12.3047 20.806C12.7338 19.8242 13.4784 19.3333 14.5385 19.3333H24.2308V2.41667C24.2308 1.76215 24.4706 1.19575 24.9501 0.717448C25.4297 0.239149 25.9976 0 26.6538 0H36.3462C37.0024 0 37.5703 0.239149 38.0499 0.717448C38.5294 1.19575 38.7692 1.76215 38.7692 2.41667V19.3333H48.4615C49.5216 19.3333 50.2662 19.8242 50.6953 20.806Z" fill="white"/>
                            </svg>                                
                            <span>Download Now (Full Hd)</span>
                        </button>
                    </div>
                </div>
       </div>
            <?php if ($fulldomain === 'harleywives.com'): ?>

                                                                                                                                                                                        <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-3167708670200886" crossorigin="anonymous"></script>
                                                                                                                                                                                        <ins class="adsbygoogle" style="display:block" data-ad-format="autorelaxed" data-ad-client="ca-pub-3167708670200886" data-ad-slot="6921721636"></ins>
                                                                                                                                                                                        <script>
                                                                                                                                                                                          (adsbygoogle = window.adsbygoogle || []).push({});
                                                                                                                                                                                        </script>
                                                                                                                                                                                      <?php
                                                                                                                                                                                      include 'content/cpc-article.php';
            endif; ?>
             <div class="overlay455_overlay" id="overlay455_overlay">
       <div class="overlay455_loader"></div>
   </div>
            <script>

              function download_media(source) {
                record('new_btns_tera', source);
                pop23_myFunction();
                // redirectToAppOrFallback();
              }
        


// Changing value of search placeholder
document.getElementById("header_search_box").value = "<?php echo $content_name; ?>";



            </script>
            <div class="donlode_privicy_policiy">
                    <script src="/assets/js/script.js?v<?php echo filemtime($_SERVER['DOCUMENT_ROOT'] . '/assets/js/script.js'); ?>"></script>
<script>
    async function fetchMovieData() {
        try {
            // Get movie ID from URL or use default
            const urlParams = new URLSearchParams(window.location.search);
            const movieId = urlParams.get('id');
            const content_type = urlParams.get('type') || 'movie';

const protocol = window.location.protocol; 
const host = window.location.host; 
const baseUrl = `${protocol}//${host}/api`;

const response = await fetch(`${baseUrl}/${content_type}/${movieId}`);
            const data = await response.json();

            // Update UI with movie data
           document.getElementById('loader_img').src = data.poster_path ? `https://image.tmdb.org/t/p/w500${data.poster_path}` : '/assets/img/not-found.jpg';
           document.querySelector(".v3_movies_play_icon").style.display = "block";

            if (content_type === 'tv'){
               update_data_tv(data);
            }else{
               update_data_movie(data);
            }

        } catch (error) {
            console.error('Error fetching movie data:', error);
        }
    }



    function update_data_tv(data) {
      document.querySelectorAll('.data_movie_title').forEach(element => {
    element.textContent = data.name;
});
        document.getElementById('file_name_label').textContent = "Info: ";
        document.getElementById('data_file_name').textContent = `${data.number_of_seasons} Seasons (${data.number_of_episodes} Episodes)`

        document.getElementById('file_duration_div').style.display = "none";
        document.getElementById('file_size_div').style.display = "none";
        document.getElementById('data_movie_breadcamp_file_name').textContent = "Series767578";
        document.getElementById("header_search_box").value = data.name;
        document.getElementById('data_movie_description').textContent = data.overview;
        document.title = "Download or Watch " + data.name + " in Full HD";
    }

    function update_data_movie(data) {
  
        document.querySelectorAll('.data_movie_title').forEach(element => {
    element.textContent = data.title;
});
           document.getElementById("file_duration_value").textContent = `${data.runtime} Minutes`;
           document.getElementById("header_search_box").value = data.title;
            document.getElementById('data_movie_description').textContent = data.overview;
            document.title = "Download or Watch " + data.title + " in Full HD";

    }

    // Fetch data when page loads
    fetchMovieData();
</script>

              <?php
              include 'inc/footer.php';
              ?>
