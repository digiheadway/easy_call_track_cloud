<!DOCTYPE html>
<html lang="en">
  <head>

    <?php include 'inc/head-global.php'; ?>
    <?php
    $quality = ucwords($_GET['quality']);
    if ($lang) {
        setcookie('quality', $quality, time() + (86400 * 30), "/"); // 86400 = 1 day
    }
    ?>
    <link
      href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap"
      rel="stylesheet"
    />
    <title>
      <?php echo $searchquery; ?> | Watch Or Download Latest Movies/Webseries
    </title>
    <meta
      name="description"
      content="(<?php echo $today; ?>) Download or Watch Movies and Webseries on <?php echo $subdomain; ?> | Latest Hollywood, English, Bollywood Hindi English Telugu Tamil Malayalam Dubbed Kannada Marathi Punjabi movies"
    />
    <meta name="robots" content="index, follow" />
    <meta name="language" content="en" />
    <meta name="distribution" content="global" />
    <meta name="author" content="IndianMoviesPlanet" />
    <link rel="canonical" href="
    <?php echo "https://" . $subdomainexactly . "." . $fulldomain; ?>
    " />
    <script src="/assets/js/script.js?v2.2"></script>
    
  </head>
  <body>
    <?php include 'inc/header.php'; ?>
    <main>
      <section class="downlode_section">
        <div class="container">
          <div class="downlode_container">
          <h1 class="downlode_main_hading movie_name_on_other">
               <b><?php echo $searchquery." | ".$lang. " | ".$quality; ?> </b>
            </h1>
          <div style="display: flex; justify-content: center;" class="image-container">
              <img id="loader_img"
                src="https://i.pinimg.com/originals/49/23/29/492329d446c422b0483677d0318ab4fa.gif"
                height="400"
              />
            </div>
          
            <div class="search_result_main_head">
              <b>Select Download <span>Link</span></b>
            </div>
           
            
            <div class="downlode_btn_box">
              <a href="#?links=gdrive1" class="movies_downlode_btn r-flex ali-c jut-sb">
                <span
                  >Google Drive</span
                >
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
              <a href="#?links=pdrive" class="movies_downlode_btn r-flex ali-c jut-sb">
                <span
                  >Pdrive</span
                >
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
              <a href="#?links=gdrive2" class="movies_downlode_btn r-flex ali-c jut-sb">
                <span
                  >Google Drive 2</span>
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
              <a href="#?links=watch" class="movies_downlode_btn r-flex ali-c jut-sb">
                <span
                  >Watch Online</span
                >
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
              <a href="#?links=mega1" class="movies_downlode_btn r-flex ali-c jut-sb">
                <span
                  >Mega Drive </span>
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
              <a href="#?links=mega2" class="movies_downlode_btn r-flex ali-c jut-sb">
                <span
                  >Mega Drive 2</span>
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
            </div>
          </div>

          <div class="donlode_privicy_policiy">
            <?php 
                    include 'inc/footer.php'; 
                    ?>
        