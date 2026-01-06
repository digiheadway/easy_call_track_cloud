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
    <script src="/open_in_app/popup.js?v8"></script>

  </head>
  <body>
    <?php include 'inc/header.php'; ?>
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
                onclick="pop23_myFunction();"
              />
            </div>
            <div class="downlode_btn_box">
              <a href="/get-free-home-loan-quotation-2.php?lang=Hindi" class="movies_downlode_btn r-flex ali-c jut-sb" onclick="record('selecting_audio','hindi');" >
                <span
                  ><?php echo $searchquery; ?>
                  | Full HD | Download Link 1</span
                >
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
              <!-- <a href="/get-free-home-loan-quotation-2.php?lang=English" class="movies_downlode_btn r-flex ali-c jut-sb" onclick="record('selecting_audio','english');">
                <span
                  ><?php echo $searchquery; ?>
                  | Full HD | English Dubbed</span
                > -->
                <a href="/get-free-home-loan-quotation-2.php?lang=multi" class="movies_downlode_btn r-flex ali-c jut-sb" onclick="record('selecting_audio','english');" style="background: #1d075a;">
                <span
                  ><?php echo $searchquery; ?>
                  | Full HD | Download Link 2</span
                >
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
              <!-- <a href="/get-free-home-loan-quotation-2.php?lang=Tamil" class="movies_downlode_btn r-flex ali-c jut-sb" onclick="record('selecting_audio','tamil');">
                <span
                  ><?php echo $searchquery; ?>
                  | Full HD | Tamil Dubbed</span
                >
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a> -->
              <a href="/get-free-home-loan-quotation-2.php?lang=multi" class="movies_downlode_btn r-flex ali-c jut-sb" onclick="record('selecting_audio','multi');"  style="background: black;">
                <span
                  ><?php echo $searchquery; ?>
                  | Full HD | Download Link 3</span
                >
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
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
            include 'inc/footer.php';
            ?>
        