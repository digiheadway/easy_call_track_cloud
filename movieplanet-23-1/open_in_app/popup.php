<?php
header("Access-Control-Allow-Origin: *");
$pop23_subdomain = isset($_SERVER['HTTP_REFERER']) ? ucfirst(explode('.', parse_url($_SERVER['HTTP_REFERER'], PHP_URL_HOST))[0]) : 'Unknown';
?>


<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>I am pop23_popup</title>

    <style>
      .pop23_container * {
        padding: 0;
        margin: 0;
        box-sizing: border-box;
        -webkit-tap-highlight-color: transparent;
        font-family: "Open Sans", sans-serif;
      }
      .noscroll {
        overflow: hidden;
      }
      .tab {
        margin-top: 15px;
      }

      /* ============= normal user clases ================= */

      .pop23_container .w-100 {
        width: 100%;
      }

      .pop23_container .pop23_r-flex {
        display: flex;
        flex-direction: row;
      }

      .pop23_container .c-flex {
        display: flex;
        flex-direction: column;
      }
      .pop23_container .flex-w {
        flex-wrap: wrap;
      }
      .pop23_container .wh-no {
        white-space: nowrap;
      }
      /* =============== justify content ============ */
      .pop23_container .jut-c {
        justify-content: center;
      }

      .pop23_container .jut-sb {
        justify-content: space-between;
      }

      .pop23_container .jut-se {
        justify-content: space-evenly;
      }

      .pop23_container .jut-sa {
        justify-content: space-around;
      }

      /* =============== alinge item ============ */
      .pop23_container .ali-c {
        align-items: center;
      }

      .pop23_container .ali-fs {
        align-items: flex-start;
      }

      .pop23_container .ali-fe {
        align-items: flex-end;
      }

      .pop23_container .ali-s {
        align-items: stretch;
      }

      .pop23_container .ali-unset {
        align-items: unset;
      }

      /* =================== pop23_containers ================ */
      .pop23_container {
        max-width: 1440px;
        width: 100%;
        margin: auto;
        padding: 0px 100px;
      }

      /* =================== fonts sizes =============== */

      .pop23_container p {
        font-size: 15px;
      }

      .pop23_container a {
        text-decoration: none;
      }
      .pop23_container li {
        list-style: none;
      }
      @media (max-width: 900px) {
        .pop23_container {
          padding: 0px 20px;
        }
      }

      /* ============== popup box ============ */
      .pop23_container {
        padding: 52px 74px;
      }

      .pop23_container {
        width: 100%;
        color: #000000;
        border-radius: 6px;
        z-index: 1;
        position: absolute;
        top: 0;
        left: 0;
        display: none;
        -webkit-animation: fadeIn 0.3s;
        animation: fadeIn 0.3s;
        transition: all 0.5s ease-in-out;
        position: fixed;
      }
      .righ-popup {
        width: 100%;
        max-width: 478px;
        padding: 30px 52px;
        background-color: #ffffff;
        z-index: 1;
        position: relative;
        left: 30%;
        top: 150px;
        border-radius: 20px;
      }
      .righ-popup .pop-form-box {
        max-width: 520px;
        width: 100%;
        padding: 18px 27px 45px 27px;
        border-radius: 20px;
        background-color: #ffffff;
      }
      .pop23_container.show {
        display: flex;
      }
      .popup-overlay {
        width: 100%;
        height: 100vh;
        position: absolute;
        background-color: #00000042;
        top: 0;
        left: 0;
      }
      .pop-clos-icon {
        position: absolute;
        right: 21px;
        top: 16px;
        cursor: pointer;
      }
      .pop23_container .form-heading {
        text-align: center;
        font-size: 18px;
        font-weight: 600;
      }

      .left-side-icon {
        gap: 8px;
        font-size: 18px;
      }
      .left-side-icon svg,
      .left-side-icon img {
        padding: 5px;
        border: 1px solid #e2e2e2;
        border-radius: 7px;
        width: 50px;
      }

      .reddit_btn {
        font-size: 16px;
        font-weight: 600;
        color: #ffffff;
        background-color: #d93a00;
        padding: 12px 30px;
        border-radius: 30px;
      }
      .chrom_btn {
        font-size: 16px;
        font-weight: 600;
        color: #0c0c0c;
        background-color: #eaedef;
        padding: 12px 30px;
        border-radius: 30px;
      }

      .pop23_data-submit {
        position: relative;
        justify-content: center;
        background-color: var(--theme-colo);
        color: #ffffff;
        padding: 4px 0px;
        border: none;
        border-radius: 8px;
        margin-top: 25px;
        width: 100%;
        font-size: 16px;
        font-weight: 700;
        cursor: pointer;
      }

      .button--loading .button__text {
        visibility: hidden;
        opacity: 0;
      }

      .button--loading::after {
        content: "";
        position: absolute;
        width: 16px;
        height: 16px;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        margin: auto;
        border: 4px solid transparent;
        border-top-color: #ffffff;
        border-radius: 50%;
        z-index: 56;
        animation: button-loading-spinner 1s ease infinite;
      }
      .pop23_data-submit svg {
        margin-top: 2.5px;
      }
      @keyframes button-loading-spinner {
        from {
          transform: rotate(0turn);
        }

        to {
          transform: rotate(1turn);
        }
      }

      /* Add animation (fade in the popup) */

      @media (max-width: 1300px) {
        .righ-popup {
          left: 25%;
        }
      }
      @media (max-width: 1100px) {
        .righ-popup {
          left: 15%;
        }
      }
      @media (max-width: 1000px) {
        .righ-popup {
          left: 13%;
        }
      }
      @media (max-width: 800px) {
        .righ-popup {
          bottom: 0;
          max-width: 100%;
          padding: 0;
        }
        .pop23_container {
          padding: 0;
        }
        .pop23_container.show {
          bottom: 0;
          display: block;
        }
        .righ-popup {
          bottom: 0;
          top: 100%;
          transition: all 0.5s ease-in-out;
        }
        .show .righ-popup {
          /* top: 100%; */
          border-radius: 12px;
          position: absolute;
          left: 0;
        }
        .righ-popup .pop-form-box {
          position: absolute;
          bottom: -12px;
          max-width: 100%;
          box-shadow: 0px 4px 35px #f3f3ff2a;
        }
        .left-side-icon {
          font-size: 16px;
        }
        @-webkit-keyframes fadeIn {
          0% {
            opacity: 0;
            bottom: -500px;
          }
          100% {
            opacity: 1;
            bottom: -1px;
          }
        }
      }
      @keyframes fadeOut {
        from {
          opacity: 1;
          bottom: -1px;
        }
        to {
          opacity: 0;
          bottom: -500px;
        }
      }

      .pop23_container.hide {
        animation: fadeOut 0.3s;
      }

    </style>
  </head>
  <body>
  

    <div class="pop23_container" id="pop23_myPopup">
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
          <div class="form-heading">Continue <?php echo $pop23_subdomain; ?> On...</div>

          <div class="tab pop23_r-flex ali-c jut-sb">
            <div class="left-side-icon pop23_r-flex ali-c">
              <img
                src="https://play-lh.googleusercontent.com/Tv3h9IHUliBayyGRxcmzOICwPGfbB8M-rnHDpzMlGM5YPS_-LytZO6GccsVPszse2Zqr"
                alt=""
                srcset="" />
              <span>TeraBox App (All Movies Available for FREE))</span>
            </div>
            <a href="https://bit.ly/1jsLoAfcjXBR8pLviRNpsBQ" target="_blank" class="reddit_btn" style="color: white;">Open</a>
          </div>
          <div class="tab pop23_r-flex ali-c jut-sb">
            <div class="left-side-icon pop23_r-flex ali-c">
              <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/e/e1/Google_Chrome_icon_%28February_2022%29.svg/1024px-Google_Chrome_icon_%28February_2022%29.svg.png" alt="" srcset="">
              <span>Website</span>
            </div>
            <a onclick="pop23_myFunction();" target="_blank" class="chrom_btn">Continue</a>
          </div>
        </div>
      </div>
    </div>

    <script>
      function pop23_myFunction() {
        var pop23_popup = document.getElementById("pop23_myPopup");
        if (pop23_popup.classList.contains("show")) {
          pop23_popup.classList.add("hide");

          // Wait for the animation to complete and then remove the 'show' and 'hide' classes
          setTimeout(() => {
            pop23_popup.classList.remove("show");
            pop23_popup.classList.remove("hide");
            document.body.classList.remove("noscroll");
          }, 300); // 300ms is the duration of the fade-out animation
        } else {
          pop23_popup.classList.add("show");
        //  document.body.classList.add("noscroll");
        }
      }
    </script>
  </body>
</html>
