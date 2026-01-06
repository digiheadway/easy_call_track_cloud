<!DOCTYPE html>
<html lang="en">
<head>
    <?php 
    require_once __DIR__ . '/inc/logic/watch-controller.php';
    include 'inc/ui/head-global.php';
    include 'inc/logic/correction.php';
    
    // Redirect if no data
    if (empty($movieData['title'])) {
        header("Location: /");
        exit();
    }
    
    // Set variables for legacy components
    $content_title = $movieData['title'];
    ?>
    <title><?php echo $movieData['title']; ?> | Watch or Download</title>
    <meta name="robots" content="index, nofollow" />
    
    <?php include 'inc/ui/popup-scripts.php'; ?>
    <?php include 'inc/ui/tera_link_gen.php'; ?>
</head>

<body>
    <?php include 'inc/ui/popup-modal.php'; ?>
    <?php include 'inc/ui/header.php'; ?>
    
    <main>
        <section class="v3_movies_watch_section">
            <div class="container">
                <?php include 'inc/ui/watch-page.php'; ?>
                
                <?php 
                require_once __DIR__ . '/inc/ui/ads.php';
                renderAd('mid_page'); 
                ?>
            </div>
        </section>
    </main>

    <script src="/assets/js/script.js?v<?php echo filemtime('assets/js/script.js'); ?>"></script>
    <script>
        // Set search box value
        const searchBox = document.getElementById("header_search_box");
        if (searchBox) searchBox.value = "<?php echo $movieData['title']; ?>";
        
        // If we don't have an image (msearch.php case), let script.js find it
        <?php if ($movieData['image'] === '/assets/img/not-found.jpg'): ?>
            // Initial loader is already handled by watch-page.php
        <?php endif; ?>
    </script>
    
    <?php include 'inc/ui/footer.php'; ?>
</body>
</html>
