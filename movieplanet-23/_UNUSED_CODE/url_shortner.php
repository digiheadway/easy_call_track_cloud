<!DOCTYPE html>
<html lang="en">

<head>
          <?php include 'inc/head-global.php';
          $searchquery = isset($_GET['q']) ? trim(strtolower($_GET['q'])) : '';

          if (!empty($searchquery)) {
            $query = urldecode($searchquery);
          } else {
            $query = isset($_COOKIE['q']) ? urldecode($_COOKIE['q']) : 'M4536';
          }

          ?>
     <meta name="robots" content="noindex, nofollow" />
    <meta charset="UTF-8" />
    <meta name="referrer" content="no-referrer" /> 
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Url Shortener. - TeraBox</title>
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
    <?php include 'inc/tera_link_gen.php'; ?>

</head>

<body>
     <?php include 'inc/header.php'; ?>
    <div class="container main">
        <div class="v3_side_content border">
            <h2 class="v3_movie_heading">Install TeraBox to Open the File</h2>
            <img class="v3_movies_fither_img" style="width:80%;"
                src="https://cdn-landerlab.com/variants/unpublished/76383b34503afb0508f8364787c55800/cd401bc3-cc4d-4f9f-ab28-60c529a2243c"
                alt="" />
        </div>
        <div class="v3_side_content">
            <img src="/assets/img/mp4_icon.png" alt="" width="50%">
           
            <h2 class="v3_movie_heading"><?php echo $query; ?>_1080p.mp4</h2>
            <h3 class="v3_file_size">File Size: 1.15 GB</h3>
            <img src="/assets/img/verified_file.png" alt="" width="70%">

            <p class="" style="color: rgb(129, 129, 129);">Last Checked: Today</p>
            <div class="btn_position">
            <button class="v3_watch_btn r-flex ali-c" id="main_btn" onclick="open_tera_on_play();gtag('event', 'install_btn_from_link_shortner');">

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
              <div>
</div>


        </div>
        
  <?php
  include 'inc/footer.php';
  ?>
</body>

</html>
