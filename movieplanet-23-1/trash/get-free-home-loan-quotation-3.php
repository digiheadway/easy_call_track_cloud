<!DOCTYPE html>
<html lang="en">
  <head>

    <?php include 'inc/head-global.php'; ?>
    <?php

    $quality = ucwords($_GET['quality']);
    if ($lang) {
        setcookie('quality', $quality, time() + (86400 * 30), "/"); // 86400 = 1 day
    }
    // Temporary variables
    $lang = "Multi Audio";
    $quality = "Full HD";

    ?>
 
    <title>
      <?php echo $searchquery; ?> - 3 | <?php echo $titlesuffix; ?> 
    </title>
    
    <meta name="robots" content="noindex, nofollow" />
    <script src="/assets/js/script.js?v<?php echo filemtime($_SERVER['DOCUMENT_ROOT'] . '/assets/js/script.js'); ?>"></script>
    <script src="https://unpkg.com/@lottiefiles/lottie-player@latest/dist/lottie-player.js"></script>

    <link rel="stylesheet" href="/assets/css/popup.css?v=<?php echo filemtime($_SERVER['DOCUMENT_ROOT'] . '/assets/css/popup.css'); ?>">

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
              <a href="#?links=gdrive1" class="movies_downlode_btn r-flex ali-c jut-sb" onclick="record('selecting_drive','gdrive');"> 
                <span
                  >Google Drive</span
                >
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
              <a href="#?links=pdrive" class="movies_downlode_btn r-flex ali-c jut-sb" onclick="record('selecting_drive','pdrive');">
                <span
                  >Pdrive</span
                >
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
              <a href="#?links=gdrive2" class="movies_downlode_btn r-flex ali-c jut-sb" onclick="record('selecting_drive','gdrive2');">
                <span
                  >Google Drive 2</span>
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
              <a href="#?links=watch" class="movies_downlode_btn r-flex ali-c jut-sb" onclick="record('selecting_drive','watch');">
                <span
                  >Watch Online</span
                >
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
              <a href="#?links=mega1" class="movies_downlode_btn r-flex ali-c jut-sb" onclick="record('selecting_drive','mega');">
                <span
                  >Mega Drive </span>
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
              <a href="#?links=mega2" class="movies_downlode_btn r-flex ali-c jut-sb" onclick="record('selecting_drive','mega2');">
                <span
                  >Mega Drive 2</span>
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
<?php endif; ?>  
          <?php include 'content/cpc-article.php'; ?>
            <!-- popup material  -->
        <div class="pop_up_container" id="myPopup">
    <div class="pop_up_overlay" onclick="showpop()"></div>
    <div class="pop_up_show_cont">
      <div class="link_locked r-flex ali-c jut-c">
        <img
          src="/assets/img/lock.svg"
          alt="" />
        <span>Link is Locked</span>
      </div>
      <div class="app_downlod_box" id="download_action">
        <div class="downlode_pop_icon r-flex ali-c jut-c">
          <img
            src="/assets/img/app_downlode.svg"
            alt="" />
        </div>
        <div class="filename"><?php echo $searchquery; ?>_full_hd_muliple_audio.mp4</div>
        <div class="pop_heading">Complete Task to Unlock the Link</div>
        <div class="app_downlod_link c-flex ali-c">
          <a
            href="https://bit.ly/42TwMWV"
target="_blank"
            class="app_link"
            id="btn_4_android"
            onclick="switchto('download_process','download_action');"
            onclick="record('download_link_click','btn_4_android');"
            >Download Android App</a
          >
          <a
            href="https://bit.ly/42TwMWV"
target="_blank"
            class="app_link"
            id="btn_4_iphone"
            onclick="switchto('download_process','download_action')"
            onclick="record('download_link_click','iphone');"
            >Download for IPhone</a
          >
          <a
            href="https://bit.ly/3p2IUXm"
target="_blank"
            class="app_link"
            id="btn_4_pc"
            onclick="switchto('download_process','download_action')"
            onclick="record('download_link_click','pc');"
            >Visit and Complete Task</a
          >
        </div>
      </div>
      <div
        class="app_downlod_process"
        style="display: none"
        id="download_process">
        <div class="downlode_pop_icon r-flex ali-c jut-c">
        <lottie-player
            src="https://lottie.host/93b06318-2f6b-4f0d-b752-040b0caa9620/PHUyIFpxGt.json"
            background="transparent"
            speed="1"
            style="width: 300px; height: 300px"
            loop
            autoplay></lottie-player>
        </div>
        <div class="pop_heading"><span id="live_status">Waiting for App Installation</span>
          <span id="wait">.</span></div>
          <div class="pop_heading">

        <button class="link_unlock_btn" onclick="not_downloadeded_app();" onclick="record('download_movie_locked','to_unlock');">Download Movie (Final Link)</button>
          </div>
        <div class="app_back_link r-flex ali-c jut-c" id="try_again">
          <span>Failed?</span>
          <a
            href="javascript:void(0);"
            class="back_link"
            onclick="switchto('download_action','download_process');"
            onclick="record('try_downloading_again','btn_on_popup');"
            >
            Try Downloading Again</a
          >
        </div>
      </div>
    </div>
  </div>
<div class="donlode_privicy_policiy">

            <?php include 'inc/footer.php';  ?>
            <script>
 function switchto(id, id2) {
    var element = document.getElementById(id);
    var element2 = document.getElementById(id2);
    element.style.display = "block";
    element2.style.display = "none";
    
  }
  function showpop() {
    var popup = document.getElementById("myPopup");
    popup.classList.toggle("show");
    document.body.classList.toggle("noscroll");
    record('download_link_popup','na');
  }

  // Get all elements with class "movies_downlode_btn"
  var downloadBtns = document.querySelectorAll(".movies_downlode_btn");

  // Loop through each download button and add a click event listener
  downloadBtns.forEach(function (btn) {
    btn.addEventListener("click", showpop);
  });

  // Animating Dots
  var dots = window.setInterval(function () {
    var wait = document.getElementById("wait");
    if (wait.innerHTML.length > 3) wait.innerHTML = "";
    else wait.innerHTML += ".";
  }, 100);

  // Changing Text of Checking
  let status = document.getElementById("live_status");
  let statuses = [
    "Checking for Download",
    "Checking for Installation",
    "Checking for Registeration",
    "Not Found",
    "Checking Again",
  ];
  let index = 0;

  setInterval(function () {
    index = (index + 1) % statuses.length;
    status.innerText = statuses[index];
  }, 2000);

  // Making the Download button effect  
  const checkingtext = document.getElementById("live_status");
  const try_again = document.getElementById("try_again");

  function not_downloadeded_app(){
    checkingtext.classList.add('link_unlock_btn-effect');
            setTimeout(function() {
              checkingtext.classList.remove('link_unlock_btn-effect');
             }, 500)

             setTimeout(function() {
              try_again.classList.add('link_unlock_btn-effect');
              setTimeout(function() {
              try_again.classList.remove('link_unlock_btn-effect');
             }, 500)

             }, 3000)
             
  }

</script>
