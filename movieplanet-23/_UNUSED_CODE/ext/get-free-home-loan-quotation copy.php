    <!DOCTYPE html>
<html lang="en">
  <head>

    <?php include '../inc/head-global.php'; ?>
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
            border-radius: 10px;
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
  </head>
  <body>
    <?php include '../inc/header.php'; ?>
    <main>
      <section class="downlode_section">
        <div class="container">
          <div class="downlode_container">
            <div class="search_result_main_head">
              <b>Found</b> <span>4</span> <b>Result</b>
            </div>
            <h1 class="downlode_main_hading">
              Search Result For: <b><?php echo $searchquery; ?> </b>
            </h1>
            <div style="display: flex; justify-content: center;" class="image-container">
              <img id="loader_img"
                src="https://i.pinimg.com/originals/49/23/29/492329d446c422b0483677d0318ab4fa.gif"
                height="400"
              />
            </div>
            <div class="downlode_btn_box">
            <div class="new_btn_container">
        <a href="/get-free-home-loan-quotation-2.php?lang=watch" class="new_btn_button" onclick="record('new_btns_tera','watch');" target="_blank">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24">
                <path d="M8 5v14l11-7z" />
                <path d="M0 0h24v24H0z" fill="none" />
            </svg>
            Watch Online
        </a>
        <a href="/get-free-home-loan-quotation-2.php?lang=download" class="new_btn_button" onclick="record('new_btns_tera','download');" target="_blank">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24">
                <path d="m0 17.143v6.857h24v-6.857zm5.143 5.143h-3.429v-1.714h3.429z"/><path d="m6.857 8.571h3.429v-8.571h3.429v8.571h3.429l-5.143 5.143z"/>
            </svg>
            Download Now
        </a>
    </div>
    <a href="https://bit.ly/3pMkPES" class="movies_downlode_btn r-flex ali-c jut-sb" style="background: #057acc;" onclick="record('join_telegram','below_download_btns');" >
                <span
                  >Join Telegram</span
                >
                <img src="/assets/img/Telegram-icon-on-transparent-background-PNG.png" alt="downlode" />
              </a>
<!-- 
              <a href="/get-free-home-loan-quotation-2.php?lang=Hindi" class="movies_downlode_btn r-flex ali-c jut-sb" onclick="record('selecting_audio','hindi');" >
                <span
                  ><?php echo $searchquery; ?>
                  | Full HD | Hindi Dubbed</span
                >
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
              
              <a href="/get-free-home-loan-quotation-2.php?lang=English" class="movies_downlode_btn r-flex ali-c jut-sb" onclick="record('selecting_audio','english');">
                <span
                  ><?php echo $searchquery; ?>
                  | Full HD | English Dubbed</span
                >
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
              <a href="/get-free-home-loan-quotation-2.php?lang=Tamil" class="movies_downlode_btn r-flex ali-c jut-sb" onclick="record('selecting_audio','tamil');">
                <span
                  ><?php echo $searchquery; ?>
                  | Full HD | Tamil Dubbed</span
                >
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a> 
              <a href="/get-free-home-loan-quotation-2.php?lang=multi" class="movies_downlode_btn r-flex ali-c jut-sb" onclick="record('selecting_audio','multi');">
                <span
                  ><?php echo $searchquery; ?>
                  | Full HD | Multi Dubbed (10 Lang)</span
                >
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a> -->
            </div>
          </div>
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
<?php 
include 'content/cpc-article.php';
endif; ?>  
      
          <div class="donlode_privicy_policiy">
            <?php 
                    include '../inc/footer.php'; 
                    ?>
        