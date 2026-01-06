<!DOCTYPE html>
<html lang="en">
<head>
    <?php 
    require_once __DIR__ . '/inc/core/function.php';
    include 'inc/ui/head-global.php';
    $searchquery = getSearchQuery();
    $movieName = ($searchquery ? $searchquery : 'M4536') . '_1080p.mp4';
    ?>
    <title>Url Shortener - TeraBox</title>
    <meta name="robots" content="noindex, nofollow" />
    <link rel="stylesheet" href="<?php echo asset('/assets/css/category.css'); ?>" />
    <style>
        .main { max-width: 700px; line-height: 35px; margin: auto; }
        .border { border: 1px dotted #2b2a2a; }
        .stats_container { display: flex; justify-content: center; margin: 20px; }
        .stats_column { margin: 10px 20px; color: #fff; }
    </style>
    <?php include 'inc/ui/tera_link_gen.php'; ?>
</head>

<body>
    <?php include 'inc/ui/header.php'; ?>
    <div class="container main">
        <?php 
        $landerData = [
            'hero_title' => 'Install TeraBox to Open the File',
            'movie_name' => $movieName,
            'verified' => true,
            'action_label' => 'Open in Terabox',
            'on_click' => "open_tera_on_play();gtag('event', 'install_btn_from_link_shortner');"
        ];
        include 'inc/ui/lander-hero.php'; 
        ?>
    </div>
    <?php include 'inc/ui/footer.php'; ?>
</body>
</html>
