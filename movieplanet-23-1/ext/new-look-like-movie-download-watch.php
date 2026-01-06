<!DOCTYPE html>
    <html lang="en">

    <head>

      <?php include 'inc/head-global.php'; ?>
      <?php
      $searchquery = ucwords($_GET['q']);
      if ($searchquery) {
        setcookie('q', $searchquery, time() + (86400 * 30), "/"); // 86400 = 1 day
      }
      ?>

      <title>
        <?php echo $searchquery; ?> - 2 | <?php echo $titlesuffix; ?>
      </title>

      <meta name="robots" content="noindex, nofollow" />
      <script src="/assets/js/script.js?v<?php echo filemtime($_SERVER['DOCUMENT_ROOT'] . '/assets/js/script.js'); ?>"></script>
      <style>
        .new_btn_container {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
        }

        .new_btn_button {
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 20px;
          margin-bottom: 20px;
          border-radius: 5px;
          background-color: #3a6b5e;
          color: #ffffff;
          font-size: 16px;
          font-weight: bold;
          text-transform: uppercase;
          text-decoration: none;
          cursor: pointer;
          transition: background-color 0.3s ease;
        }

        .new_btn_button:hover {
          background-color: #204c42;
        }

        .new_btn_button svg {
          fill: #ffffff;
          margin-right: 10px;
        }
      </style>
          <style>
        .v3_movies_watch_section{
            margin-top: 20px;
            margin-bottom: 50px;
        }
        .v3_bradcamp_box{
            font-size: 14px;
            gap: 10px;
            font-weight: 600;
        }
        .v3_home_brad_camp{
            gap: 10px;
            color: #373737;
        }
        .v3_movie_name_bradcamp{
            color: #466A5F;
        }
        .v3_movies_container{
            margin-top: 20px;
            gap: 70px;
        }
        .v3_movies_image_container{
            position: relative;
            width: 100%;
            max-width: 500px;
        }
        .v3_movies_fither_img{
            /* position: absolute; */
            width: 100%;
            z-index: -1;
            border-radius: 10px;
        }
        .v3_movies_play_icon{
            display: flex;
            flex-direction: row;
            align-items: center;
            justify-content: center;
            margin: auto;
            transform: translate(50%, 100%);
            position: absolute;
            top: 0;
            left: 0;
        }
        .v3_side_content_box{
            width: 100%;
            max-width: 800px;
        }
        .v3_movie_heading{
            font-size: 40px;
            font-weight: 700;
        }
        .v3_movie_share_btn{
            gap: 9px;
            font-size: 16px;
            color: #ffffff;
            padding: 7px 19px;
            background-color: #3A6B5E;
            width: fit-content;
            border-radius: 35px;
            font-weight: 600;
            margin-top: 20px;
        }
        .v3_movie_share_btn:hover{
            background-color: #275348;
        }
        .v3_side_content{
            margin: 25px 0px;
            font-size: 14px;
            line-height: 21px;
             color: #595959;
        }
        .v3_last_update,
        .v3_file_size{
            font-size: 16px;
        }
        .v3_file_size{
            margin-top: 10px;
        }
        .v3_paly_and_downlod_btn{
            gap: 20px;
            margin-top: 30px;
            cursor: pointer;
        }
        .v3_watch_btn{
            padding: 12px 20px;
            border: none;
            outline: none;
            background-color: #3A6B5E;
            border-radius: 4px;
            gap: 8px;
            font-size: 20px;
            color: #ffffff;
            font-weight: 600;
            cursor: pointer;
        }
        .v3_watch_btn:hover{
            background-color: #29584c;
        }

        @media (max-width: 1000px){
            .v3_movies_container{
                gap: 30px;
            }
            .v3_movie_share_btn{
                margin-top: 15px;
                font-size: 15px;
            }
            .v3_movie_share_btn svg{
                width: 18px;
                height: fit-content;
            }
            .v3_movie_heading{
                font-size: 35px;
            }
            .v3_side_content{
                font-size: 14px;
                line-height: 21px;
                margin: 25px 0px 15px 0px;
            }
            .v3_last_update, .v3_file_size{
                font-size: 16px;
            }
            .v3_movies_image_container{
                max-width: 400px;
            }
            .v3_movies_play_icon{
                max-width: 180px;
            }
        }
        @media (max-width: 800px){
            .v3_movies_container{
                flex-direction: column;
            }
            .v3_movies_play_icon{
                max-width: 150px;
                transform: translate(75%, 120%);
            }
            .v3_movie_heading{
                font-size: 24.81px;
            }
            .v3_side_content{
                margin: 20px 0px 12px 0px;
                font-size: 14px;
            }
            .v3_movie_share_btn{
                font-size: 12px;
                padding: 4px 8px;
            }
            .v3_movie_share_btn svg{
                width: 14px;
            }
            .v3_paly_and_downlod_btn{
                flex-direction: column-reverse;
                gap: 10px;
                margin-top: 40px;
            }
            .v3_watch_btn{
                width: 100%;
                justify-content: center;
                font-size: 16px;
            }
            .v3_watch_btn svg {
                width: 16px;
            }
        }
        @media (max-width: 380px){
            .v3_movies_play_icon{
                transform: translate(100%, 150%);
                max-width: 110px;
            }
        }
    </style>
    </head>

    <body>
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
                <span class="v3_movie_name_bradcamp"><?php echo $searchquery; ?></span>
            </div>

            <div class="v3_movies_container r-flex ali-c" class="image-container">
                <div class="v3_movies_image_container">
                    <img id="loader_img" src="https://upload.wikimedia.org/wikipedia/commons/b/b1/Loading_icon.gif?20151024034921" alt="Movie" class="v3_movies_fither_img">
                    <!-- <img src="https://tester.ygsbhardwaj.com/devender/exteraMovies2/images/movies_play_icon.svg" alt="movies play icon" class="v3_movies_play_icon"> -->
                </div>
                <div class="v3_side_content_box">
                    <h2 class="v3_movie_heading"><?php echo $searchquery; ?></h2>
                    <a  onclick="shareCurrentUrl()" class="v3_movie_share_btn r-flex ali-c">
                        <svg width="14" height="20" viewBox="0 0 25 27" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M20.8971 19.0437C19.888 19.0437 18.9852 19.4303 18.2947 20.0359L8.82787 14.688C8.89426 14.3917 8.94737 14.0953 8.94737 13.786C8.94737 13.4767 8.89426 13.1803 8.82787 12.8839L18.1885 7.5876C18.9055 8.23192 19.8482 8.63141 20.8971 8.63141C23.1012 8.63141 24.8804 6.90462 24.8804 4.76546C24.8804 2.62631 23.1012 0.899521 20.8971 0.899521C18.6931 0.899521 16.9139 2.62631 16.9139 4.76546C16.9139 5.07474 16.967 5.37113 17.0334 5.66752L7.67273 10.9639C6.95575 10.3195 6.01304 9.92005 4.96412 9.92005C2.76005 9.92005 0.980865 11.6468 0.980865 13.786C0.980865 15.9251 2.76005 17.6519 4.96412 17.6519C6.01304 17.6519 6.95575 17.2525 7.67273 16.6081L17.1263 21.9689C17.0599 22.2395 17.0201 22.523 17.0201 22.8065C17.0201 24.8812 18.7595 26.5694 20.8971 26.5694C23.0348 26.5694 24.7742 24.8812 24.7742 22.8065C24.7742 20.7318 23.0348 19.0437 20.8971 19.0437Z" fill="white"/>
                        </svg>                            
                        <span>Share With Friends</span>
                    </a>
                    <p class="v3_side_content">
                    Get ready for an incredible experience with <?php echo $searchquery; ?>, a captivating masterpiece that is perfect for everyone. This amazing creation combines thrilling action, intriguing suspense, and heartfelt emotions to deliver a truly memorable viewing experience. With talented actors, stunning visuals, and a compelling story, <?php echo $searchquery; ?> has something for everyone to enjoy. Don't miss the chance to download <?php echo $searchquery; ?> and immerse yourself in a world of excitement and entertainment that will stay with you long after the credits roll. Get ready to be captivated by the magic of <?php echo $searchquery; ?>!
                    </p>
                    <div class="v3_last_update">
                        <b>Last Updated:</b>
                        <span>1 Day Ago</span>
                    </div>
                    <div class="v3_file_size">
                        <b>File Size:</b>
                        <span>500 MB - 2.5GB</span>
                    </div>

                    <div class="v3_paly_and_downlod_btn r-flex ali-c">
                        <button class="v3_watch_btn r-flex ali-c" onclick="download_media('watch');"> 
                            <svg width="20" height="25" viewBox="0 0 39 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M5.87407 47.3317C4.59012 48.1707 3.29076 48.2197 1.976 47.4788C0.661233 46.7379 0.0025679 45.5917 0 44.0402V3.96172C0 2.41279 0.658665 1.26659 1.976 0.523103C3.29333 -0.220382 4.59269 -0.171332 5.87407 0.670252L37.2667 20.7095C38.4222 21.484 39 22.5811 39 24.001C39 25.4208 38.4222 26.518 37.2667 27.2924L5.87407 47.3317Z" fill="white"/>
                            </svg>                                
                            <span>Watch Now</span>
                        </button>
                        <button class="v3_watch_btn r-flex ali-c" onclick="download_media('download');">
                            <svg width="30" height="25" viewBox="0 0 63 58" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M48.4615 50.75C48.4615 50.0955 48.2218 49.5291 47.7422 49.0508C47.2626 48.5725 46.6947 48.3333 46.0385 48.3333C45.3822 48.3333 44.8143 48.5725 44.3347 49.0508C43.8552 49.5291 43.6154 50.0955 43.6154 50.75C43.6154 51.4045 43.8552 51.9709 44.3347 52.4492C44.8143 52.9275 45.3822 53.1667 46.0385 53.1667C46.6947 53.1667 47.2626 52.9275 47.7422 52.4492C48.2218 51.9709 48.4615 51.4045 48.4615 50.75ZM58.1538 50.75C58.1538 50.0955 57.9141 49.5291 57.4345 49.0508C56.9549 48.5725 56.387 48.3333 55.7308 48.3333C55.0745 48.3333 54.5066 48.5725 54.027 49.0508C53.5475 49.5291 53.3077 50.0955 53.3077 50.75C53.3077 51.4045 53.5475 51.9709 54.027 52.4492C54.5066 52.9275 55.0745 53.1667 55.7308 53.1667C56.387 53.1667 56.9549 52.9275 57.4345 52.4492C57.9141 51.9709 58.1538 51.4045 58.1538 50.75ZM63 42.2917V54.375C63 55.3819 62.6466 56.2379 61.9399 56.9427C61.2332 57.6476 60.375 58 59.3654 58H3.63462C2.625 58 1.76683 57.6476 1.0601 56.9427C0.353365 56.2379 0 55.3819 0 54.375V42.2917C0 41.2847 0.353365 40.4288 1.0601 39.724C1.76683 39.0191 2.625 38.6667 3.63462 38.6667H21.2398L26.351 43.8021C27.8149 45.2118 29.5312 45.9167 31.5 45.9167C33.4688 45.9167 35.1851 45.2118 36.649 43.8021L41.7981 38.6667H59.3654C60.375 38.6667 61.2332 39.0191 61.9399 39.724C62.6466 40.4288 63 41.2847 63 42.2917ZM50.6953 20.806C51.1244 21.8381 50.9477 22.7192 50.1653 23.4492L33.2037 40.3659C32.7494 40.8442 32.1815 41.0833 31.5 41.0833C30.8185 41.0833 30.2506 40.8442 29.7963 40.3659L12.8347 23.4492C12.0523 22.7192 11.8756 21.8381 12.3047 20.806C12.7338 19.8242 13.4784 19.3333 14.5385 19.3333H24.2308V2.41667C24.2308 1.76215 24.4706 1.19575 24.9501 0.717448C25.4297 0.239149 25.9976 0 26.6538 0H36.3462C37.0024 0 37.5703 0.239149 38.0499 0.717448C38.5294 1.19575 38.7692 1.76215 38.7692 2.41667V19.3333H48.4615C49.5216 19.3333 50.2662 19.8242 50.6953 20.806Z" fill="white"/>
                            </svg>                                
                            <span>Download</span>
                        </button>
                    </div>
                </div>
       </div>
            <?php if ($fulldomain === 'harleywives.com') : ?>

              <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-3167708670200886" crossorigin="anonymous"></script>
              <ins class="adsbygoogle" style="display:block" data-ad-format="autorelaxed" data-ad-client="ca-pub-3167708670200886" data-ad-slot="6921721636"></ins>
              <script>
                (adsbygoogle = window.adsbygoogle || []).push({});
              </script>
            <?php
              include 'content/cpc-article.php';
            endif; ?>
            <script>
              function redirectToAppOrFallback() {
                const appLink = 'dubox://share_detail?shareid=3856125';
                const link_if_not_installed = '/get-free-home-loan-quotation-2.php?lang=watch';
                const link_if_installed = 'https://bit.ly/3pWzYTP';

                const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);

                if (isMobile) {
                  const timeout = setTimeout(() => {
                    window.location = link_if_not_installed;
                    record('isMobile', 'run_function');
                  }, 2000);

                  window.addEventListener('blur', () => {
                    clearTimeout(timeout);
                    window.location = link_if_installed;
                    record('already_installed_tera', 'run_function');
                  });

                  window.location = appLink;
                  record('tried_app_link', 'run_function');

                } else {
                  window.location = link_if_not_installed;
                  record('already_installed_tera', 'run_function');
                }
              }

              function download_media(source) {
                record('new_btns_tera', source);
                redirectToAppOrFallback();
              }
             
    function shareCurrentUrl() {
        if (navigator.share) {
            // Use the Web Share API if it's supported by the browser
            navigator.share({
                title: 'Share Title',
                text: 'Check out this amazing content!',
                url: window.location.href
            })
            .then(() => {
                console.log('Successfully shared.');
            })
            .catch((error) => {
                console.log('Sharing failed:', error);
            });
        } else {
            // Fallback behavior if the Web Share API is not supported
            console.log('Web Share API not supported.');
            // Perform any custom sharing action or display a message to the user
        }
    }

            </script>
            <div class="donlode_privicy_policiy">
              <?php
              include 'inc/footer.php';
              ?>
