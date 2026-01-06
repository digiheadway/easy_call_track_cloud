<!DOCTYPE html>
<html lang="en">

<head>
          <?php
          if (isset($_GET['test'])) {
              $adurls = [
                  "https://shorturl.at/kitki",
                  "https://shorturl.at/kitki"
              ];

              // Select a random URL from the list
              $randomUrl = $adurls[array_rand($adurls)];

              // Redirect to the randomly selected URL
              header('Location: ' . $randomUrl);
              exit();
          }
          // Check if 'ir' cookie is not set or user agent contains 'Android'
          if (!isset($_COOKIE['ir']) || strpos($_SERVER['HTTP_USER_AGENT'], 'Android') == false) {
              // Redirect to the homepage
              header('Location: /?utm_source=from_tera_inst'); // Change '/homepage.php' to the actual path of your homepage
              exit(); // Exit to prevent further execution
          }


          include 'inc/head-global.php';
          $searchquery = isset($_GET['q']) ? trim(strtolower($_GET['q'])) : '';

          if (!empty($searchquery)) {
              $query = urldecode($searchquery);
          } else {
              $query = isset($_COOKIE['q']) ? urldecode($_COOKIE['q']) : 'default_value';
          }
          ?>
     <meta name="robots" content="noindex, nofollow" />
    <meta charset="UTF-8" />
    <meta name="referrer" content="no-referrer" /> 
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>View File in TeraBox</title>

    <link rel="stylesheet" href="/assets/css/style.css" />
    <style>
        .border {
  border: 1px dotted #2b2a2a;
}
.border2 {
  border: 1px dotted #63d1db;
  background-color: black;
  border-radius: 5px;
  color: #00a173;
}
.border2 > h2 {
  color: #2bc79b;
}
.v3_side_content {
  text-align: center;
  padding: 10px 10px;
  text-align: center;
}
.v3_side_content > * {
  margin: 10px 0px;
  /* line-height: 50px; */
}

.v3_movie_heading {
  color: white;
  font-size: 20px;
}
.v3_file_size {
  color: #00a173;
}

.stats_container {
  display: flex;
  flex-direction: row;
  justify-content: center;
  margin: 20px;
}

.stats_row {
  display: flex;
}

.stats_column {
  margin: 20px;
}

.main {
  max-width: 700px;
  line-height: 35px;
}

.btn_position {
  display: flex;
  justify-content: center;
}
#status_report_dialog{
    margin-top:10px;
}
@keyframes swipeup {
             0% {
                opacity: 1;
                transform: translateX(0);
            }
            100% {
                opacity: 0;
                transform: translateY(-100%);
            }
        }
    </style>
    <script>

var link_if_uninstalled = "https://bit.ly/SGyBkHH6xN3k4gXDD7kxhA";

function check_if_already_downloaded() {

    const timeout = setTimeout(() => {
        console.log("Not Installed");
        document.getElementById("step_1").style.animation = "fadeOut 1s forwards";
        playAudio("step_2");
        gtag('event', 'step_2_on_uninstall_page');

        setTimeout(function() {
            document.getElementById("step_1").style.display = "none";
        }, 800);
        document.getElementById('main_btn').removeAttribute('onclick');
        document.getElementById('step_2').classList.add('border2');
        document.getElementById('main_btn').addEventListener('click', function() {
            window.open(link_if_uninstalled, '_blank');

        });

    }, 1000);

    const blurEvent = () => {
        clearTimeout(timeout);
        console.log("Installed");
        already_installed_terabox();
    };
    window.addEventListener("blur", blurEvent);
    const timeoutRemove = setTimeout(() => {
        window.removeEventListener("blur", blurEvent);
    }, 1000);

    console.log("Checking Installation..");
    window.location = "dubox://check";
}

function already_installed_terabox() {
    // Code to run on page load if the application is installed
    document.getElementById('status_report_dialog').innerText = 'Still Installed';
    playAudio("still_installed");
            gtag('event', 'still_installed_uninstall_page');

    document.getElementById('check_install_btn').innerText = 'Check Again';
}
var isAudioPlaying = false;
function playAudio(file) {
    if (isAudioPlaying) {
        return; // Do nothing if the audio is already playing
    }

    var allMediaElements = document.querySelectorAll('audio, video');

    allMediaElements.forEach(function(mediaElement) {
        mediaElement.pause();
        mediaElement.currentTime = 0; // Reset the playback to the beginning
    });

    var audio = new Audio('/assets/'+file+'.mp3');

    // Set the flag to indicate that the audio is now playing
    isAudioPlaying = true;

    audio.addEventListener('ended', function() {
        // Reset the flag when the audio finishes playing
        isAudioPlaying = false;
    });

    audio.play();
}

    </script>
</head>

<body>
     <?php include 'inc/header.php'; ?>
    <div class="container main">
        <div class="v3_side_content border">
            <h2 class="v3_movie_heading">Reinstall TeraBox to Open the File</h2>
            <img class="v3_movies_fither_img" style="width:80%;"
                src="https://cdn-landerlab.com/variants/unpublished/76383b34503afb0508f8364787c55800/cd401bc3-cc4d-4f9f-ab28-60c529a2243c"
                alt="" />
        </div>
        <div class="v3_side_content">
            <img src="/assets/img/mp4_icon.png" alt="" width="50%">
            <!-- <svg id="iyz4dk" data-gjs-type="svg" draggable="true" width="128" height="128" viewBox="0 0 128 128"
                fill="none" xmlns="http://www.w3.org/2000/svg">
                <path id="i5susj" data-gjs-type="svg-in" draggable="true"
                    d="M46.1902 27.93V26.19C46.1902 22.72 44.1102 22 41.8302 22.41L22.7302 25.86C20.4402 26.27 18.7602 28.33 18.7602 30.73V35.17C18.7602 36.79 17.6302 38.18 16.0802 38.46L15.7402 38.52C14.4002 38.76 13.4102 39.96 13.4102 41.37L14.4902 115.4C14.4902 115.4 14.4102 116.58 15.5202 116.66C16.6702 116.74 17.5402 115.86 18.6902 115.59L108.33 94.23C109.67 93.9901 110.66 92.79 110.66 91.38V25.35C110.66 23.56 109.1 22.2 107.39 22.5L51.0802 32.19C48.5302 32.64 46.1902 31.68 46.1902 27.93Z"
                    fill="#F3AB47"></path>
                <path id="ih79sf" data-gjs-type="svg-in" draggable="true"
                    d="M112.56 91.651L114.58 32.001C114.58 30.211 113.02 28.851 111.31 29.151L55.0395 38.771C52.4195 39.221 50.4495 41.411 50.2895 44.061L50.0595 47.831C49.8795 50.851 47.6295 53.351 44.6495 53.851L21.9995 57.661C19.7595 58.041 18.1095 59.981 18.1095 62.261L15.5195 116.641C15.5195 116.641 15.8495 116.781 19.5595 115.841L108.76 94.731C110.11 94.511 111.65 93.251 112.56 91.651Z"
                    fill="#FFE36C"></path>
                <path id="ibfttl" data-gjs-type="svg-in" draggable="true"
                    d="M21.6328 58.7293L44.3928 54.8393C50.0928 53.5693 51.0028 51.4093 50.9928 46.8693L51.0028 45.6993C51.1828 42.2793 52.8428 40.2593 56.2128 39.6793L111.883 30.0293"
                    stroke="#FFF9C4" stroke-width="2" stroke-miterlimit="10" stroke-linecap="round"></path>
            </svg> -->
            <h2 class="v3_movie_heading"><?php echo $query; ?>_1080p.mp4</h2>
            <h3 class="v3_file_size">File Size: 1.15 GB</h3>
            <p class="" style="color: rgb(129, 129, 129);">Last Checked: Today</p>
            <div class="btn_position">
            <button class="v3_watch_btn r-flex ali-c" id="main_btn" onclick="document.getElementById('unistall_heading').scrollIntoView({ behavior: 'smooth' });playAudio('uninstall_tera');gtag('event', 'open_file_btn_uninstall_page');">

<svg width="22" height="16" viewBox="0 0 22 16" fill="none" xmlns="http://www.w3.org/2000/svg">
<path d="M10 16H5.5C3.98333 16 2.68767 15.475 1.613 14.425C0.538333 13.375 0.000666667 12.0917 0 10.575C0 9.275 0.391667 8.11667 1.175 7.1C1.95833 6.08333 2.98333 5.43333 4.25 5.15C4.66667 3.61667 5.5 2.375 6.75 1.425C8 0.475 9.41667 0 11 0C12.95 0 14.6043 0.679333 15.963 2.038C17.3217 3.39667 18.0007 5.05067 18 7C19.15 7.13333 20.1043 7.62933 20.863 8.488C21.6217 9.34667 22.0007 10.3507 22 11.5C22 12.75 21.5627 13.8127 20.688 14.688C19.8133 15.5633 18.7507 16.0007 17.5 16H12V8.85L13.6 10.4L15 9L11 5L7 9L8.4 10.4L10 8.85V16Z" fill="black"/>
</svg>



                <span>Open in Terabox</span>
            </button>
</div>
            <div class="stats_container">
                <div class="stats_column">
                    <svg id="ir4hc" data-gjs-type="svg" draggable="true" width="24" height="24" viewBox="0 0 24 24"
                        fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path id="ibldp" data-gjs-type="svg-in" draggable="true"
                            d="M21 15V16.2C21 17.8802 21 18.7202 20.673 19.362C20.3854 19.9265 19.9265 20.3854 19.362 20.673C18.7202 21 17.8802 21 16.2 21H7.8C6.11984 21 5.27976 21 4.63803 20.673C4.07354 20.3854 3.6146 19.9265 3.32698 19.362C3 18.7202 3 17.8802 3 16.2V15M17 10L12 15M12 15L7 10M12 15V3"
                            stroke="#FFFFFFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        </path>
                    </svg>
                    <span class="stats_row">
                        700+ Downloads
                    </span>
                </div>
                <div class="stats_column">
                    <svg id="i2ucl" data-gjs-type="svg" draggable="true" width="24" height="24" viewBox="0 0 24 24"
                        fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path id="imp6j" data-gjs-type="svg-in" draggable="true"
                            d="M11.2827 3.45332C11.5131 2.98638 11.6284 2.75291 11.7848 2.67831C11.9209 2.61341 12.0791 2.61341 12.2152 2.67831C12.3717 2.75291 12.4869 2.98638 12.7174 3.45332L14.9041 7.88328C14.9721 8.02113 15.0061 8.09006 15.0558 8.14358C15.0999 8.19096 15.1527 8.22935 15.2113 8.25662C15.2776 8.28742 15.3536 8.29854 15.5057 8.32077L20.397 9.03571C20.9121 9.11099 21.1696 9.14863 21.2888 9.27444C21.3925 9.38389 21.4412 9.5343 21.4215 9.68377C21.3988 9.85558 21.2124 10.0372 20.8395 10.4004L17.3014 13.8464C17.1912 13.9538 17.136 14.0076 17.1004 14.0715C17.0689 14.128 17.0487 14.1902 17.0409 14.2545C17.0321 14.3271 17.0451 14.403 17.0711 14.5547L17.906 19.4221C17.994 19.9355 18.038 20.1922 17.9553 20.3445C17.8833 20.477 17.7554 20.57 17.6071 20.5975C17.4366 20.6291 17.2061 20.5078 16.7451 20.2654L12.3724 17.9658C12.2361 17.8942 12.168 17.8584 12.0962 17.8443C12.0327 17.8318 11.9673 17.8318 11.9038 17.8443C11.832 17.8584 11.7639 17.8942 11.6277 17.9658L7.25492 20.2654C6.79392 20.5078 6.56341 20.6291 6.39297 20.5975C6.24468 20.57 6.11672 20.477 6.04474 20.3445C5.962 20.1922 6.00603 19.9355 6.09407 19.4221L6.92889 14.5547C6.95491 14.403 6.96793 14.3271 6.95912 14.2545C6.95132 14.1902 6.93111 14.128 6.89961 14.0715C6.86402 14.0076 6.80888 13.9538 6.69859 13.8464L3.16056 10.4004C2.78766 10.0372 2.60121 9.85558 2.57853 9.68377C2.55879 9.5343 2.60755 9.38389 2.71125 9.27444C2.83044 9.14863 3.08797 9.11099 3.60304 9.03571L8.49431 8.32077C8.64642 8.29854 8.72248 8.28742 8.78872 8.25662C8.84736 8.22935 8.90016 8.19096 8.94419 8.14358C8.99391 8.09006 9.02793 8.02113 9.09597 7.88328L11.2827 3.45332Z"
                            stroke="#FFFFFFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        </path>
                    </svg>
                    <span class="stats_row">
                        4.6 Star Rating
                    </span>
                </div>

            </div>
        </div>
        <div>
            <div class="v3_side_content">
                <h2 class="v3_movie_heading">How to Watch it in TeraBox?</h2>
                <p class="v3_side_content">It’s Very Simple Process But Below is the Step wise tutorial</p>
            </div>
            <div class="v3_side_content border2" id="step_1">
<h2 class="v3_movie_heading" id="unistall_heading">Step 1: Uninstall the TeraBox App</h2>
<p class="v3_side_content">We have detected that you have already installed terabox app in your device To View the file you need to unisntall the terabox application from your device. Use the below button to update your installation status in the page. if terabox is still installed on your device. the file will not open if you didn’t reinstall the terabox app.</p>
 <div class="btn_position">
<button class="v3_watch_btn r-flex ali-c" onclick="check_if_already_downloaded();">

<svg width="23" height="23" viewBox="0 0 235 235" fill="none" xmlns="http://www.w3.org/2000/svg">
<g clip-path="url(#clip0_672_72)">
<path d="M117.5 0C128.286 0 138.69 1.37695 148.711 4.13086C158.732 6.88477 168.065 10.8626 176.709 16.0645C185.353 21.2663 193.271 27.3861 200.461 34.4238C207.652 41.4616 213.81 49.3791 218.936 58.1763C224.061 66.9735 228 76.3444 230.754 86.2891C233.508 96.2337 234.924 106.637 235 117.5C235 128.286 233.623 138.69 230.869 148.711C228.115 158.732 224.137 168.065 218.936 176.709C213.734 185.353 207.614 193.271 200.576 200.461C193.538 207.652 185.621 213.81 176.824 218.936C168.027 224.061 158.656 228 148.711 230.754C138.766 233.508 128.363 234.924 117.5 235C106.714 235 96.3102 233.623 86.2891 230.869C76.2679 228.115 66.9352 224.137 58.291 218.936C49.6468 213.734 41.7293 207.614 34.5386 200.576C27.3478 193.538 21.1898 185.621 16.0645 176.824C10.9391 168.027 6.99951 158.656 4.24561 148.711C1.4917 138.766 0.0764974 128.363 0 117.5C0 106.714 1.37695 96.3102 4.13086 86.2891C6.88477 76.2679 10.8626 66.9352 16.0645 58.291C21.2663 49.6468 27.3861 41.7293 34.4238 34.5386C41.4616 27.3478 49.3791 21.1898 58.1763 16.0645C66.9735 10.9391 76.3444 6.99951 86.2891 4.24561C96.2337 1.4917 106.637 0.0764974 117.5 0ZM176.25 117.5H117.5V44.0625H102.812V132.188H176.25V117.5Z" fill="black"/>
</g>
<defs>
<clipPath id="clip0_672_72">
<rect width="235" height="235" fill="white"/>
</clipPath>
</defs>
</svg>

<span id="check_install_btn">Check Install Status</span>
            </button>
            </div>          

                        <div id="status_report_dialog"></div>

</div>
<div class="v3_side_content" id="step_2">
<h2 class="v3_movie_heading">Step 2: Install from the Below Button & Signup With New Email</h2>
<p class="v3_side_content">You have to install terabox again and signup with new email to view the file.</p>
   <div class="btn_position">

<button class="v3_watch_btn r-flex ali-c" onclick="window.open(link_if_uninstalled, '_blank');">

<svg width="24" height="34.1" viewBox="0 0 360 512" fill="none" xmlns="http://www.w3.org/2000/svg">
<path d="M48 59.49V452.49C48.0026 453.343 48.2569 454.176 48.731 454.884C49.2051 455.593 49.8779 456.146 50.665 456.474C51.4521 456.802 52.3184 456.89 53.1554 456.727C53.9924 456.565 54.7628 456.159 55.37 455.56L260 256L55.37 56.42C54.7628 55.8214 53.9924 55.4153 53.1554 55.2528C52.3184 55.0902 51.4521 55.1784 50.665 55.5063C49.8779 55.8341 49.2051 56.3871 48.731 57.0958C48.2569 57.8045 48.0026 58.6374 48 59.49ZM345.8 174L89.22 32.64L89.06 32.55C84.64 30.15 80.44 36.13 84.06 39.61L285.19 231.93L345.8 174ZM84.08 472.39C80.44 475.87 84.64 481.85 89.08 479.45L89.24 479.36L345.8 338L285.19 280.05L84.08 472.39ZM449.38 231L377.73 191.54L310.36 256L377.73 320.43L449.38 281C468.87 270.23 468.87 241.77 449.38 231Z" fill="black"/>
</svg>


                <span>Download Terabox Now</span>
            </button>
</div>
</div>

        </div>
        
  <?php
  include 'inc/footer.php';
  ?>
</body>

</html>
