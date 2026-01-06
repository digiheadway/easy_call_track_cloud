<!DOCTYPE html>
<html lang="en">
<head>
    <?php 
    require_once __DIR__ . '/inc/core/function.php';
    include 'inc/ui/head-global.php'; 
    include 'inc/ui/audio.php';

    // Anti-bot / Env check
    if (!isset($_COOKIE['ir']) || strpos($_SERVER['HTTP_USER_AGENT'], 'Android') === false) {
        header('Location: /?utm_source=from_tera_inst');
        exit();
    }

    $searchquery = getSearchQuery();
    $movieName = ($searchquery ?: 'M4536') . '_1080p.mp4';
    ?>
    <title>View File in TeraBox</title>
    <meta name="robots" content="noindex, nofollow" />
    <link rel="stylesheet" href="<?php echo asset('/assets/css/category.css'); ?>" />
    <style>
        .main { max-width: 700px; margin: auto; }
        .border { border: 1px dotted #2b2a2a; }
        .border2 { border: 1px dotted #63d1db; background-color: black; border-radius: 5px; color: #00a173; }
        .stats_container { display: flex; justify-content: center; margin: 20px; color: #fff; }
        .stats_column { margin: 10px 20px; }
        .v3_movie_heading { color: white; font-size: 20px; }
    </style>

    <script>
        const link_if_uninstalled = "https://bit.ly/SGyBkHH6xN3k4gXDD7kxhA";

        function check_if_already_downloaded() {
            setTimeout(() => {
                console.log("Not Installed");
                document.getElementById("step_1").style.display = "none";
                document.getElementById('step_2').style.display = "block";
                playLanderAudio("step_2");
                gtag('event', 'step_2_on_uninstall_page');
            }, 1000);

            const blurEvent = () => { already_installed_terabox(); };
            window.addEventListener("blur", blurEvent, { once: true });
            window.location = "dubox://check";
        }

        function already_installed_terabox() {
            document.getElementById('status_report_dialog').innerText = 'Still Installed';
            playLanderAudio("still_installed");
            gtag('event', 'still_installed_uninstall_page');
            document.getElementById('check_install_btn').innerText = 'Check Again';
        }
    </script>
</head>

<body>
    <?php include 'inc/ui/header.php'; ?>
    <div class="container main">
        <div id="step_1">
            <?php 
            $landerData = [
                'hero_title' => 'Step 1: Uninstall the TeraBox App',
                'movie_name' => $movieName,
                'action_label' => 'Check Install Status',
                'on_click' => "check_if_already_downloaded();playLanderAudio('uninstall_tera');"
            ];
            include 'inc/ui/lander-hero.php'; 
            ?>
            <div id="status_report_dialog" style="text-align:center; color:#00a173; margin-top:10px;"></div>
        </div>

        <div id="step_2" style="display:none;" class="v3_side_content border2">
            <h2 class="v3_movie_heading">Step 2: Reinstall & Signup With New Email</h2>
            <p>You have to install terabox again and signup with a new email to view the file.</p>
            <div class="btn_position">
                <button class="v3_watch_btn r-flex ali-c" onclick="window.open(link_if_uninstalled, '_blank');">
                    <span>Download Terabox Now</span>
                </button>
            </div>
        </div>
    </div>
    <?php include 'inc/ui/footer.php'; ?>
</body>
</html>
