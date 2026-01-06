<!DOCTYPE html>
<html lang="en">

<head>
    <?php
    error_reporting(E_ALL);
    ini_set('display_errors', 1);

    include 'inc/head-global.php'; ?>
    <title>
        Category Page | <?php echo $titlesuffix; ?>
    </title>
    <meta name="robots" content="noindex, nofollow" />
    <style>
        .movies_quality_type {
            background: rgba(0, 0, 0, 0.6);
            color: #ffffff;
            top: 0px;
        }

        .header_search_btn {
            color: #01583e;
        }

        .hero_container {
            margin-top: 20px;
        }

        .hero_search_form {
            display: flex;
            align-items: center;
            color: #5B5B5B;
            font-size: 12px;
            margin: 20px auto;
            padding: 0px;
            border-radius: 5px;
            background: #202020;
            max-width: 900px;

        }

        .hero_search_form button {
            background: none;
            border: none;
            margin: 0 20px;
            height: 20px;
            width: 20px;

        }

        #hero_search {
            margin: 0;
            background-color: transparent;
            border: none;
            padding: 17px 0;
            font-weight: bold;
            color: #bababa;
        }

        .v3_home_brad_camp,
        .v3_bradcamp_box svg path {
            color: #5D5B5B;
            fill: #5D5B5B;
        }

        .v3_bradcamp_box {
            margin: 20px 0;
        }

        .latest_movies_conatiner {
            max-width: 900px;
            width: 100%;
            margin: auto;
            margin-top: 41px;
        }

        .contant_container {
            max-width: 900px;
            margin: 50px auto;
        }

        #header_search_box {
            background-color: #202020;
            border: none;

        }

        .header_search_btn {
            color: #014632;
        }

        /* Change the white to any color */
        input:-webkit-autofill,
        input:-webkit-autofill:hover,
        input:-webkit-autofill:focus,
        input:-webkit-autofill:active {
            -webkit-box-shadow: 0 0 0 30px #202020 inset !important;
        }

        /*Change text in autofill textbox*/
        input:-webkit-autofill {
            -webkit-text-fill-color: #5B5B5B !important;
        }



        /* Increase the font weight of the placeholder text */
        ::placeholder {
            font-weight: bold;
            /* Adjust the font weight as desired */
        }

        input[type="search"]::-ms-clear {
            display: none;
            /* For Internet Explorer */
        }

        input[type="search"]::-webkit-search-cancel-button,
        input[type="search"]::-webkit-search-decoration,
        input[type="search"]::-webkit-search-results-button,
        input[type="search"]::-webkit-search-results-decoration {
            display: none;
            /* For WebKit-based browsers like Chrome and Safari */
        }

        .category_list {
            background-color: #202020;
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 15px;
            border-radius: 5px;
            outline: 0.1px solid #262626;
            margin-top: 20px;

        }

        .catogry_list_label {
            color: #A0A0A0;
            font-size: 14px;
            font-weight: 700;
        }

        .hero_container {
            margin: 20px auto;
            max-width: 900px;
        }

        .hero_search_btn {
            color: #004432;
        }

        .notice_board_box {
            border-radius: 5px;
            background: #FFF;
            box-shadow: 2px 3px 10.7px 3px rgba(255, 255, 255, 0.25);
            color: #141414;
            text-align: center;
            padding: 20px;
            margin: 40px 0;
        }
    </style>
</head>

<body>
    <?php include 'inc/header.php'; ?>
    <main>

        <section class="hero_section">
            <div class="container">
                <div class="hero_container">
                    <div class="v3_bradcamp_box r-flex ali-c">
                        <a href="#" class="v3_home_brad_camp  r-flex ali-c">
                            <span>Search</span>
                            <svg width="8" height="15" viewBox="0 0 11 19" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M0.375 18.6C0.125 18.35 0 18.054 0 17.712C0 17.37 0.125 17.0743 0.375 16.825L7.7 9.5L0.35 2.15C0.116667 1.91667 0 1.625 0 1.275C0 0.925 0.125 0.625 0.375 0.375C0.625 0.125 0.921 0 1.263 0C1.605 0 1.90067 0.125 2.15 0.375L10.55 8.8C10.65 8.9 10.721 9.00833 10.763 9.125C10.805 9.24167 10.8257 9.36667 10.825 9.5C10.825 9.63333 10.804 9.75833 10.762 9.875C10.72 9.99167 10.6493 10.1 10.55 10.2L2.125 18.625C1.89167 18.8583 1.604 18.975 1.262 18.975C0.92 18.975 0.624333 18.85 0.375 18.6Z" fill="#A18F8F"></path>
                            </svg>
                        </a>
                        <span class="v3_movie_name_bradcamp"><?php echo $searchquery; ?></span>
                    </div>
                    <div class="notice_board">
                        <div class="notice_board_box">
                            <h4>Found Multiple Content</h4>
                            <br>
                            <p>You Have to <b>Search the Exact Name</b>
                                of Movie/Sports/Serial/Webseries or Episode name to find the exact content you are looking for</p>
                        </div>
                    </div>
                    <div class="hero_logo c-flex ali jut-c">

                        <div class="hero_logo_box r-flex ali-c">
                            <svg width="38" height="38" viewBox="0 0 38 38" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M27.9489 31.0526H31.0526V34.1579H18.6315C10.0564 34.1579 3.10522 27.2067 3.10522 18.6315C3.10522 10.0564 10.0564 3.10522 18.6315 3.10522C27.2067 3.10522 34.1579 10.0564 34.1579 18.6315C34.1598 21.0421 33.5997 23.4199 32.5219 25.5761C31.444 27.7323 29.8782 29.6075 27.9489 31.0526ZM18.6315 15.5263C19.4551 15.5263 20.2449 15.1991 20.8273 14.6168C21.4096 14.0344 21.7368 13.2446 21.7368 12.421C21.7368 11.5974 21.4096 10.8076 20.8273 10.2253C20.2449 9.64291 19.4551 9.31575 18.6315 9.31575C17.808 9.31575 17.0181 9.64291 16.4358 10.2253C15.8534 10.8076 15.5263 11.5974 15.5263 12.421C15.5263 13.2446 15.8534 14.0344 16.4358 14.6168C17.0181 15.1991 17.808 15.5263 18.6315 15.5263ZM12.421 21.7368C13.2446 21.7368 14.0344 21.4096 14.6168 20.8273C15.1991 20.2449 15.5263 19.4551 15.5263 18.6315C15.5263 17.808 15.1991 17.0181 14.6168 16.4358C14.0344 15.8534 13.2446 15.5263 12.421 15.5263C11.5974 15.5263 10.8076 15.8534 10.2253 16.4358C9.64291 17.0181 9.31575 17.808 9.31575 18.6315C9.31575 19.4551 9.64291 20.2449 10.2253 20.8273C10.8076 21.4096 11.5974 21.7368 12.421 21.7368ZM24.8421 21.7368C25.6656 21.7368 26.4555 21.4096 27.0378 20.8273C27.6202 20.2449 27.9473 19.4551 27.9473 18.6315C27.9473 17.808 27.6202 17.0181 27.0378 16.4358C26.4555 15.8534 25.6656 15.5263 24.8421 15.5263C24.0185 15.5263 23.2287 15.8534 22.6463 16.4358C22.064 17.0181 21.7368 17.808 21.7368 18.6315C21.7368 19.4551 22.064 20.2449 22.6463 20.8273C23.2287 21.4096 24.0185 21.7368 24.8421 21.7368ZM18.6315 27.9473C19.4551 27.9473 20.2449 27.6202 20.8273 27.0378C21.4096 26.4555 21.7368 25.6656 21.7368 24.8421C21.7368 24.0185 21.4096 23.2287 20.8273 22.6463C20.2449 22.064 19.4551 21.7368 18.6315 21.7368C17.808 21.7368 17.0181 22.064 16.4358 22.6463C15.8534 23.2287 15.5263 24.0185 15.5263 24.8421C15.5263 25.6656 15.8534 26.4555 16.4358 27.0378C17.0181 27.6202 17.808 27.9473 18.6315 27.9473Z" fill="#3A6B5E" />
                            </svg>
                            <span class="logo_name">
                                <?php echo $subdomaintitle; ?>
                            </span>
                        </div>
                        <span class="logo_help_text hero_logo_HT"> Movies & WebSeries</span>
                    </div>
                </div>

                <form action="<?php echo $func_search_url; ?>" onsubmit="record('searched_from_home','categoty_search');">
                    <div class="hero_search_form">
                        <input type="search" name="q" id="hero_search" placeholder="Search Any Movie/Series/Show " autocomplete="off">
                        <button type="submit">
                            <svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 19 19" fill="none">
                                <path d="M11.9509 4.67398C11.4747 4.18894 10.9071 3.80305 10.281 3.5386C9.65477 3.27415 8.98239 3.13639 8.30266 3.13327C7.62292 3.13014 6.94931 3.26172 6.32071 3.5204C5.69212 3.77909 5.12101 4.15974 4.64036 4.64039C4.15971 5.12104 3.77905 5.69216 3.52037 6.32075C3.26169 6.94934 3.13011 7.62296 3.13323 8.30269C3.13636 8.98242 3.27412 9.6548 3.53857 10.281C3.80301 10.9072 4.1889 11.4748 4.67395 11.951C5.64196 12.9014 6.9461 13.4311 8.30266 13.4248C9.65921 13.4186 10.9584 12.8769 11.9177 11.9177C12.8769 10.9585 13.4186 9.65924 13.4248 8.30269C13.431 6.94614 12.9013 5.642 11.9509 4.67398ZM3.55453 3.55457C4.76801 2.34139 6.39911 1.63761 8.11428 1.58714C9.82945 1.53668 11.4991 2.14334 12.7818 3.28308C14.0645 4.42282 14.8634 6.00952 15.015 7.71871C15.1667 9.42791 14.6596 11.1305 13.5976 12.4782L17.8291 16.7097L16.7097 17.8291L12.4782 13.5977C11.13 14.6557 9.42889 15.1596 7.72191 15.0065C6.01493 14.8534 4.43067 14.0548 3.29231 12.7736C2.15395 11.4925 1.54726 9.82526 1.59599 8.11211C1.64471 6.39897 2.34519 4.76895 3.55453 3.55457Z" fill="#6C6868" />
                            </svg>
                        </button>
                    </div>
                    <button type="submit" class="hero_search_btn r-flex ali-c jut-c">
                        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M10.0639 3.93601C9.66292 3.52755 9.18495 3.20259 8.65763 2.97989C8.13031 2.7572 7.5641 2.64119 6.99169 2.63856C6.41928 2.63593 5.85203 2.74673 5.32269 2.96457C4.79334 3.18241 4.31241 3.50296 3.90765 3.90772C3.50289 4.31248 3.18234 4.79341 2.9645 5.32276C2.74666 5.8521 2.63586 6.41935 2.63849 6.99176C2.64112 7.56417 2.75713 8.13038 2.97983 8.6577C3.20252 9.18501 3.52748 9.66299 3.93594 10.064C4.75111 10.8643 5.84933 11.3104 6.99169 11.3051C8.13405 11.2999 9.22813 10.8438 10.0359 10.036C10.8437 9.2282 11.2998 8.13412 11.3051 6.99176C11.3103 5.8494 10.8643 4.75118 10.0639 3.93601ZM2.99327 2.99334C4.01515 1.97171 5.38871 1.37906 6.83306 1.33656C8.27741 1.29406 9.68343 1.80494 10.7636 2.76472C11.8438 3.7245 12.5165 5.06067 12.6442 6.49999C12.7719 7.93931 12.3449 9.37305 11.4506 10.508L15.0139 14.0713L14.0713 15.014L10.5079 11.4507C9.3726 12.3417 7.9401 12.766 6.50264 12.6371C5.06519 12.5081 3.73107 11.8356 2.77246 10.7567C1.81384 9.67787 1.30293 8.27392 1.34397 6.83127C1.385 5.38862 1.97488 4.01598 2.99327 2.99334Z" fill="#004432" />
                        </svg>

                        <span>Search Now</span>
                    </button>
                </form>


            </div>
        </section>
        <section class="latest_movies_section">
            <div class="container">

                <div class="latest_movies_conatiner">

                    <div class="latest_main_heading r-flex ali">
                        <span>Popular Searches</span>
                        <svg width="34" height="34" viewBox="0 0 34 34" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M31.6094 7.4375V15.9375C31.6094 16.1488 31.5255 16.3515 31.376 16.501C31.2266 16.6504 31.0239 16.7344 30.8125 16.7344C30.6012 16.7344 30.3985 16.6504 30.2491 16.501C30.0996 16.3515 30.0157 16.1488 30.0157 15.9375V9.36328L18.6204 20.7453C18.5484 20.8209 18.4619 20.881 18.366 20.9221C18.2701 20.9632 18.1669 20.9844 18.0625 20.9844C17.9582 20.9844 17.855 20.9632 17.7591 20.9221C17.6632 20.881 17.5767 20.8209 17.5047 20.7453L12.75 16.0039L3.74536 24.9953C3.67211 25.0686 3.58514 25.1267 3.48943 25.1663C3.39372 25.206 3.29114 25.2264 3.18755 25.2264C3.08395 25.2264 2.98137 25.206 2.88566 25.1663C2.78995 25.1267 2.70299 25.0686 2.62974 24.9953C2.55648 24.9221 2.49837 24.8351 2.45873 24.7394C2.41909 24.6437 2.39868 24.5411 2.39868 24.4375C2.39868 24.3339 2.41909 24.2313 2.45873 24.1356C2.49837 24.0399 2.55648 23.9529 2.62974 23.8797L12.1922 14.3172C12.2642 14.2416 12.3507 14.1815 12.4466 14.1404C12.5425 14.0993 12.6457 14.0781 12.75 14.0781C12.8544 14.0781 12.9576 14.0993 13.0535 14.1404C13.1494 14.1815 13.2359 14.2416 13.3079 14.3172L18.0625 19.0586L28.8868 8.23437H22.3125C22.1012 8.23437 21.8985 8.15042 21.7491 8.00098C21.5996 7.85153 21.5157 7.64884 21.5157 7.4375C21.5157 7.22616 21.5996 7.02347 21.7491 6.87402C21.8985 6.72458 22.1012 6.64063 22.3125 6.64062H30.8125C31.0239 6.64062 31.2266 6.72458 31.376 6.87402C31.5255 7.02347 31.6094 7.22616 31.6094 7.4375Z" fill="#3A6B5E"/>
                        </svg>

                    </div>
                    <div>
<?php include 'inc/popular_queries.php'; ?>
                    </div>

                </div>

            </div>
        </section>



        <section class="contant-section">
            <div class="container">
                <div class="contant_container">

                    <?php
                    // include 'inc/other-websites.php';
                    include 'inc/footer.php';
                    ?>
