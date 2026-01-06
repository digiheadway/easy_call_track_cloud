<!DOCTYPE html>
<html lang="en">
<head>
    <?php include 'inc/ui/head-global.php'; ?>
    <title>Category Page | <?php echo $titlesuffix; ?></title>
    <meta name="robots" content="noindex, nofollow" />
    <link rel="stylesheet" href="<?php echo asset('/assets/css/category.css'); ?>" />
</head>

<body>
    <?php include 'inc/ui/header.php'; ?>
    <main>
        <section class="hero_section">
            <div class="container">
                <div class="hero_container">
                    <div class="v3_bradcamp_box r-flex ali-c">
                        <a href="/" class="v3_home_brad_camp r-flex ali-c">
                            <span>Search</span>
                            <svg width="8" height="15" viewBox="0 0 11 19" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M0.375 18.6L7.7 9.5L0.35 2.15" fill="#A18F8F"></path>
                            </svg>
                        </a>
                        <span class="v3_movie_name_bradcamp"><?php echo htmlspecialchars($searchquery ?? ''); ?></span>
                    </div>
                    
                    <div class="notice_board">
                        <div class="notice_board_box">
                            <h4>Found Multiple Content</h4>
                            <p>You Have to <b>Search the Exact Name</b> of Movie/Sports/Serial/Webseries to find the content you are looking for.</p>
                        </div>
                    </div>
                    
                    <div class="hero_logo c-flex ali jut-c">
                        <div class="hero_logo_box r-flex ali-c">
                            <?php include 'inc/ui/logo.php'; ?>
                            <span class="logo_name"><?php echo $subdomaintitle; ?></span>
                        </div>
                        <span class="logo_help_text hero_logo_HT"> Movies & WebSeries</span>
                    </div>
                </div>

                <form action="<?php echo $func_search_url; ?>" onsubmit="record('searched_from_home','categoty_search');">
                    <div class="hero_search_form">
                        <input type="search" name="q" id="hero_search" placeholder="Search Any Movie/Series/Show" autocomplete="off" value="<?php echo htmlspecialchars($searchquery ?? ''); ?>">
                        <button type="submit">🔍</button>
                    </div>
                </form>
            </div>
        </section>

        <section class="latest_movies_section">
            <div class="container">
                <div class="latest_movies_conatiner">
                    <div class="latest_main_heading r-flex ali">
                        <span>Popular Searches</span>
                    </div>
                    <div>
                        <?php include 'inc/ui/popular_queries.php'; ?>
                    </div>
                </div>
            </div>
        </section>

        <section class="contant-section">
            <div class="container">
                <div class="contant_container">
                    <?php include 'inc/ui/footer.php'; ?>
                </div>
            </div>
        </section>
    </main>
</body>
</html>
